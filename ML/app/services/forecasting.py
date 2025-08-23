import pandas as pd
import numpy as np
from sqlalchemy import func
from sqlalchemy.orm import Session
from sklearn.linear_model import LinearRegression, Ridge, Lasso
from sklearn.metrics import mean_squared_error, mean_absolute_error
from sklearn.preprocessing import StandardScaler
from sklearn.feature_selection import SelectKBest, f_regression
from datetime import datetime
from ..models.database import Order, OrderItem, OrderStatus
import logging

logger = logging.getLogger(__name__)

class Forecasting:
    def __init__(self, db: Session):
        self.db = db
        self.scaler = StandardScaler()
        self.model = None
        self.feature_selector = None
        self.results = {}

    def get_holiday_dates(self) -> list:
        current_year = datetime.now().year
        holidays = []

        for year in range(current_year - 1, current_year + 2):
            holidays.extend([
                datetime(year, 1, 1),  # New Year's Day
                datetime(year, 11, 24),  # Thanksgiving (example)
                datetime(year, 12, 25),  # Christmas Day
            ])

        return holidays

    def get_sales_data(self) -> pd.DataFrame:
        query = self.db.query(
            func.date(Order.order_date).label('date'),
            func.sum(OrderItem.quantity * OrderItem.unit_price).label('sales'),
            func.count(Order.order_id).label('orders'),
            func.count(func.distinct(Order.customer_id)).label('customers')
        ).join(OrderItem).filter(Order.status == OrderStatus.DELIVERED)

        query = query.group_by(func.date(Order.order_date)).order_by('date')

        results = query.all()
        df = pd.DataFrame(results, columns=['date', 'sales', 'orders', 'customers'])

        df['date'] = pd.to_datetime(df['date'])
        df = df.set_index('date')

        full_range = pd.date_rnge(start=df.index.min(), end=df.index.max(), freq='D')
        df = df.reindex(full_range, fill_value=0)

        return df

    def create_features(self, df: pd.DataFrame) -> pd.DataFrame:
        features = df.copy()

        features['year'] = features.index.year
        features['month'] = features.index.month
        features['day_of_week'] = features.index.dayofweek
        features['day_of_year'] = features.index.dayofyear
        features['quarter'] = features.index.quarter
        features['is_weekend'] = features['day_of_week'].isin([5, 6]).astype(int)

        season_map = {12: 0, 1: 0, 2: 0, 3: 1, 4: 1, 5: 1,
                      6: 2, 7: 2, 8: 2, 9: 3, 10: 3, 11: 3}
        features['season'] = features['month'].map(season_map)

        holiday_dates = self.get_holiday_dates()
        features['is_holiday'] = features.index.isin([h.date() for h in holiday_dates]).astype(int)

        # Lag features
        features['sales_lag_1'] = features['sales'].shift(1)
        features['sales_lag_7'] = features['sales'].shift(7)
        features['sales_ma_7'] = features['sales'].rolling(7).mean()
        features['sales_ma_30'] = features['sales'].rolling(30).mean()

        features['sales_growth_7d'] = features['sales'].pct_change(7).fillna(0)
        features['trend'] = np.arange(len(features))

        return features

    def clean_data(self, df: pd.DataFrame) -> pd.DataFrame:
        df_clean = df.copy()

        numeric_cols = df_clean.select_dtypes(include=[np.number]).columns
        for col in numeric_cols:
            df_clean[col] = df_clean[col].fillna(df_clean[col].median())

        Q1 = df_clean['sales'].quantile(0.25)
        Q3 = df_clean['sales'].quantile(0.75)
        IQR = Q3 - Q1

        lower_bound = Q1 - 1.5 * IQR
        upper_bound = Q3 + 1.5 * IQR

        df_clean['sales'] = df_clean['sales'].clip(lower_bound, upper_bound)

        return df_clean

    def prepare_training_data(self, df: pd.DataFrame, test_size: float = 0.2):

        exclude_cols = ['sales', 'orders', 'customers']
        feature_cols = [col for col in df.columns if col not in exclude_cols]

        X = df[feature_cols].replace([np.inf, -np.inf], np.nan).fillna(0)
        y = df['sales']

        split_idx = int(len(df) * (1 - test_size))
        X_train, X_test = X.iloc[:split_idx], X.iloc[split_idx:]
        y_train, y_test = y.iloc[:split_idx], y.iloc[split_idx:]

        return X_train, X_test, y_train, y_test

    def train_models(self, X_train, y_train, X_test, y_test) -> dict:
        models = {
            'Linear Regression': LinearRegression(),
            'Ridge Regression': Ridge(alpha=1.0),
            'Lasso Regression': Lasso(alpha=1.0)
        }

        results = {}

        X_train_scaled = self.scaler.fit_transform(X_train)
        X_test_scaled = self.scaler.transform(X_test)

        for name, model in models.items():
            selector = SelectKBest(score_func=f_regression, k=min(20, X_train.shape[1]))
            X_train_selected = selector.fit_transform(X_train_scaled, y_train)
            X_test_selected = selector.transform(X_test_scaled)

            model.fit(X_train_selected, y_train)

            train_pred = model.predict(X_train_selected)
            test_pred = model.predict(X_test_selected)

            results[name] = {
                'model': model,
                'selector': selector,
                'train_mae': mean_absolute_error(y_train, train_pred),
                'test_mae': mean_absolute_error(y_test, test_pred),
                'train_rmse': np.sqrt(mean_squared_error(y_train, train_pred)),
                'test_rmse': np.sqrt(mean_squared_error(y_test, test_pred)),
                'predictions': test_pred
            }

        best_model_name = min(results.keys(), key=lambda k: results[k]['test_mae'])
        self.model = results[best_model_name]['model']
        self.feature_selector = results[best_model_name]['selector']

        logger.info(f"Best model: {best_model_name}")
        return results

    def create_forecast(self) -> dict:
        try:
            sales_data = self.get_sales_data()
            features_data = self.create_features(sales_data)
            clean_data = self.clean_data(features_data)

            X_train, X_test, y_train, y_test = self.prepare_training_data(clean_data)
            model_results = self.train_models(X_train, y_train, X_test, y_test)

            best_model = min(model_results.keys(), key=lambda k: model_results[k]['test_mae'])
            predictions = model_results[best_model]['predictions']

            residuals = y_test - predictions
            std_error = np.std(residuals)
            confidence_interval = 1.96 * std_error

            self.results = {
                'model_performance': model_results,
                'predictions': predictions,
                'confidence_lower': predictions - confidence_interval,
                'confidence_upper': predictions + confidence_interval,
                'best_model': best_model,
                'test_mae': model_results[best_model]['test_mae'],
                'test_rmse': model_results[best_model]['test_rmse']
            }

            logger.info(f"Forecast created with MAE: {self.results['test_mae']:.2f}")
            return self.results

        except Exception as e:
            logger.error(f"Forecasting failed: {e}")
            raise