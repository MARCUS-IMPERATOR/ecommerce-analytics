from datetime import datetime
from unittest.mock import Mock, patch

import numpy as np
import pandas as pd
import pytest
from sklearn.preprocessing import StandardScaler
from sqlalchemy.orm import Session

from app.services.forecasting import Forecasting


@pytest.fixture
def mock_db():
    return Mock(spec=Session)


@pytest.fixture
def mock_forecasting(mock_db):
    return Forecasting(mock_db)


@pytest.fixture
def mock_sales():

    dates = pd.date_range(start='2023-01-01', end='2023-12-31', freq='D')
    np.random.seed(42)

    base_sales = 1000
    seasonal_pattern = 200 * np.sin(2 * np.pi * np.arange(len(dates)) / 365)
    noise = np.random.normal(0, 100, len(dates))
    daily_sales = base_sales + seasonal_pattern + noise
    daily_sales = np.maximum(daily_sales, 0)

    data = {
        'daily_sales': daily_sales,
        'order_count': np.random.randint(10, 100, len(dates)),
        'avg_order_value': np.random.uniform(20, 200, len(dates)),
        'unique_customers': np.random.randint(5, 80, len(dates))
    }

    df = pd.DataFrame(data, index=dates)
    return df


def test_init(mock_db):
    forecasting = Forecasting(mock_db)

    assert forecasting.session == mock_db
    assert isinstance(forecasting.scaler, StandardScaler)
    assert forecasting.model is None
    assert forecasting.feature_selector is None
    assert forecasting.feature_names == []
    assert forecasting.forecast_results == {}

def test_holidays(mock_forecasting):

    with patch('app.services.forecasting.datetime') as mock_datetime:
        mock_datetime.now.return_value = datetime(2023, 6, 15)
        mock_datetime.strptime = datetime.strptime

        holidays = mock_forecasting.holidays()

        expected_dates = [
            datetime(2022, 11, 24),
            datetime(2022, 1, 1),
            datetime(2023, 11, 24),
            datetime(2023, 1, 1),
            datetime(2024, 11, 24),
            datetime(2024, 1, 1)
        ]

        assert len(holidays) == 6
        assert all(isinstance(h, datetime) for h in holidays)

def test_sales_data(mock_forecasting):

    mock_query = Mock()
    mock_query.join.return_value = mock_query
    mock_query.filter.return_value = mock_query
    mock_query.group_by.return_value = mock_query
    mock_query.order_by.return_value = mock_query

    mock_results = [
        ('2023-01-01', 1000.0, 10, 100.0, 5),
        ('2023-01-02', 1200.0, 12, 100.0, 6),
        ('2023-01-03', 800.0, 8, 100.0, 4)
    ]
    mock_query.all.return_value = mock_results

    mock_forecasting.session.query.return_value = mock_query

    result_df = mock_forecasting.sales_data()

    mock_forecasting.session.query.assert_called_once()
    assert isinstance(result_df, pd.DataFrame)
    assert len(result_df) >= 3
    assert result_df.index.dtype == 'datetime64[ns]'

def test_feature_engineering(mock_forecasting, mock_sales):

    features_df = mock_forecasting.feature_engineering(mock_sales)

    expected_features = [
        'year', 'month', 'day', 'day_of_week', 'day_of_year',
        'week_of_year', 'quarter', 'season_autumn', 'season_spring',
        'season_summer', 'season_winter', 'is_holiday', 'trend',
        'sales_lag_1', 'sales_lag_7', 'sales_lag_30',
        'sales_ma_7', 'sales_ma_30', 'sales_growth_7d', 'sales_growth_30d'
    ]

    for feature in expected_features:
        assert feature in features_df.columns, f"Missing feature: {feature}"

    assert features_df['year'].dtype in [np.int32, np.int64]
    assert 1 <= features_df['month'].min() <= 12
    assert 0 <= features_df['day_of_week'].min() <= 6
    assert features_df['is_holiday'].dtype in [np.int32, np.int64]

    season_cols = ['season_autumn', 'season_spring', 'season_summer', 'season_winter']
    season_sums = features_df[season_cols].sum(axis=1)
    assert all(season_sums == 1)

def test_prepare_training_data(mock_forecasting, mock_sales):

    featured_data = mock_forecasting.feature_engineering(mock_sales)

    X_train, X_test, y_train, y_test = mock_forecasting.prepare_training_data(
        featured_data, target='daily_sales', test_size=0.2
    )

    total_samples = len(featured_data)
    expected_train_size = int(total_samples * 0.8)
    expected_test_size = total_samples - expected_train_size

    assert len(X_train) == expected_train_size
    assert len(X_test) == expected_test_size
    assert len(y_train) == expected_train_size
    assert len(y_test) == expected_test_size

    assert 'daily_sales' not in X_train.columns
    assert 'order_count' not in X_train.columns

    assert len(mock_forecasting.feature_names) == X_train.shape[1]

    assert not np.isinf(X_train).any().any()
    assert not np.isinf(X_test).any().any()

def test_train_forecasting_model(mock_forecasting, mock_sales):

    featured_data = mock_forecasting.feature_engineering(mock_sales)
    X_train, X_test, y_train, y_test = mock_forecasting.prepare_training_data(featured_data)

    results = mock_forecasting.train_forecasting_model(X_train, X_test, y_train, y_test)

    expected_models = ['Linear Regression', 'Ridge Regression', 'Lasso Regression']
    assert set(results.keys()) == set(expected_models)

    for model_name, result in results.items():
        required_keys = [
            'model', 'selector', 'train_mae', 'test_mae', 'train_rmse',
            'test_rmse', 'train_mape', 'test_mape', 'cv_mae', 'cv_std',
            'predictions_train', 'predictions_test'
        ]
        for key in required_keys:
            assert key in result, f"Missing key {key} in {model_name} results"

        assert result['train_mae'] >= 0
        assert result['test_mae'] >= 0
        assert result['train_rmse'] >= 0
        assert result['test_rmse'] >= 0

        assert len(result['predictions_train']) == len(y_train)
        assert len(result['predictions_test']) == len(y_test)

    assert mock_forecasting.model is not None
    assert mock_forecasting.feature_selector is not None

def test_evaluation_metrics(mock_forecasting):

    y_true = pd.Series([100, 110, 90, 120, 105])
    y_pred = np.array([95, 115, 85, 125, 100])
    revenue_per_unit = 10

    metrics = mock_forecasting.evaluation(y_true, y_pred, revenue_per_unit)

    required_metrics = [
        'revenue_accuracy_error_pct', 'forecast_bias', 'direction_accuracy_pct',
        'true_total_revenue', 'predicted_total_revenue'
    ]

    for metric in required_metrics:
        assert metric in metrics


    expected_true_revenue = y_true.sum() * revenue_per_unit
    expected_pred_revenue = y_pred.sum() * revenue_per_unit

    assert metrics['true_total_revenue'] == expected_true_revenue
    assert metrics['predicted_total_revenue'] == expected_pred_revenue

    assert 0 <= metrics['direction_accuracy_pct'] <= 100

def test_prediction_intervals(mock_forecasting, mock_sales):

    featured_data = mock_forecasting.feature_engineering(mock_sales)
    X_train, X_test, y_train, y_test = mock_forecasting.prepare_training_data(featured_data)
    mock_forecasting.train_forecasting_model(X_train, X_test, y_train, y_test)

    predictions, lower_bound, upper_bound = mock_forecasting.prediction_intervals(
        X_test, y_test, confidence_level=0.95
    )

    assert len(predictions) == len(X_test)
    assert len(lower_bound) == len(X_test)
    assert len(upper_bound) == len(X_test)

    assert all(lower_bound <= predictions)
    assert all(predictions <= upper_bound)
    assert all(lower_bound < upper_bound)

