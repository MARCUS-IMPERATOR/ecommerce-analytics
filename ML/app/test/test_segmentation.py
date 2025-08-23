import pytest
import pandas as pd
import numpy as np
from datetime import datetime, timedelta
from unittest.mock import Mock, patch
from sqlalchemy.orm import Session

from app.models.database import Customer
from app.services.segmentation import Segmentation
from app.models.database import Segments, CustomerSegment


@pytest.fixture
def mock_db():
    return Mock(spec=Session)

@pytest.fixture
def mock_segmentation(mock_db):
    return Segmentation(mock_db)

@pytest.fixture
def mock_rfm_data():
    return pd.DataFrame({
        "customer_id": [1, 2, 3, 4, 5],
        "recency": [1, 10, 33, 40, 150],
        "frequency": [1, 2, 10, 4, 2],
        "monetary": [100.00, 100000.00, 555.00, 95.00, 150.00]
    })

@pytest.fixture
def mock_customer():
    customer = Mock(spec=Customer)
    customer.customer_id = 1
    return customer


def test_init(mock_db):
    segmentation = Segmentation(mock_db)
    assert segmentation.db == mock_db
    assert segmentation.scaler is None
    assert segmentation.model is None
    assert segmentation.cluster_to_segment_mapping == {}

def test_rfm(mock_segmentation, mock_customer):
    now = datetime.now()
    last_order_date = now - timedelta(days=100)
    frequency = 5
    monetary = 1500.0

    last_order_date_query = Mock()
    last_order_date_query.filter.return_value.scalar.return_value = last_order_date

    frequency_query = Mock()
    frequency_query.filter.return_value.scalar.return_value = frequency

    monetary_query = Mock()
    monetary_query.filter.return_value.scalar.return_value = monetary

    mock_segmentation.db.query.side_effect = [
        last_order_date_query,
        frequency_query,
        monetary_query
    ]

    recency, freq_result, monetary_result = mock_segmentation.calculate_rfm_customer(mock_customer)

    assert recency == 100
    assert freq_result == frequency
    assert monetary_result == monetary

@patch("app.services.segmentation.text")
def test_fetch_rfm_data(mock_text, mock_segmentation):
    mock_results = [
        (1, 10, 5, 1000.0),
        (2, 30, 3, 500.0),
        (3, 60, 2, 200.0)
    ]

    mock_result_proxy = Mock()
    mock_result_proxy.fetchall.return_value = mock_results
    mock_segmentation.db.execute.return_value = mock_result_proxy

    df = mock_segmentation.fetch_rfm_data()

    assert len(df) == 3
    assert list(df.columns) == ['customer_id', 'recency', 'frequency', 'monetary']
    assert df['recency'].dtype == 'Int64'
    assert df['frequency'].dtype in (np.int64, int)
    assert df['monetary'].dtype in (np.float64, float)

def test_preprocess_data(mock_segmentation, mock_rfm_data):
    X, scaler, df_prep = mock_segmentation.preprocess_data(mock_rfm_data)

    assert X.shape[0] == len(mock_rfm_data)
    assert X.shape[1] == 3
    assert scaler is not None

    assert 'recency_score' in df_prep.columns
    assert 'frequency_log' in df_prep.columns
    assert 'monetary_log' in df_prep.columns

    assert all(df_prep['frequency_log'] >= 0)
    assert all(df_prep['monetary_log'] >= 0)

def test_clustering(mock_segmentation, mock_rfm_data):
    df_result, kmeans, scaler = mock_segmentation.perform_clustering(mock_rfm_data)

    assert 'cluster' in df_result.columns
    assert 'segment_label' in df_result.columns
    assert len(df_result) == len(mock_rfm_data)
    assert kmeans is not None
    assert scaler is not None
    assert len(mock_segmentation.cluster_to_segment_mapping) > 0

def test_calculate_cluster_stats(mock_segmentation):
    df = pd.DataFrame({
        'customer_id': [1, 2, 3, 4],
        'recency': [10, 20, 30, 40],
        'frequency': [5, 4, 3, 2],
        'monetary': [1000, 800, 600, 400],
        'cluster': [0, 0, 1, 1]
    })

    stats = mock_segmentation._calculate_cluster_stats(df)

    assert len(stats) == 2
    assert 'cluster' in stats.columns
    assert 'customer_id_count' in stats.columns

def test_map_clusters_to_segments(mock_segmentation):
    cluster_stats = pd.DataFrame({
        'cluster': [0, 1, 2],
        'recency_mean': [10, 50, 100],
        'frequency_mean': [10, 5, 2],
        'monetary_mean': [1000, 500, 100]
    })

    mapping = mock_segmentation._map_clusters_to_segments(cluster_stats)

    assert len(mapping) == 3
    assert all(isinstance(segment, Segments) for segment in mapping.values())

def test_predict_customer_segment_with_model(mock_segmentation):

    mock_segmentation.model = Mock()
    mock_segmentation.model.predict.return_value = [0]
    mock_segmentation.scaler = Mock()
    mock_segmentation.scaler.transform.return_value = np.array([[1, 2, 3]])
    mock_segmentation.cluster_to_segment_mapping = {0: Segments.CHAMPION}

    segment = mock_segmentation.predict_customer_segment(10, 5, 1000.0)

    assert segment == Segments.CHAMPION

def test_update_customer_segments_success(mock_segmentation):

    sample_data = pd.DataFrame({
        'customer_id': [1, 2],
        'recency': [10, 30],
        'frequency': [5, 3],
        'monetary': [1000.0, 500.0]
    })

    with patch.object(mock_segmentation, 'fetch_rfm_data', return_value=sample_data):
        with patch.object(mock_segmentation, 'perform_clustering') as mock_clustering:
            clustered_data = sample_data.copy()
            clustered_data['cluster'] = [0, 1]
            clustered_data['segment_label'] = [Segments.CHAMPION, Segments.LOYAL]

            mock_model = Mock()
            mock_scaler = Mock()
            mock_clustering.return_value = (clustered_data, mock_model, mock_scaler)

            mock_segmentation.db.get.return_value = Mock(spec=Customer)

            result = mock_segmentation.update_customer_segments()

            assert result['updated'] == 2
            assert result['errors'] == 0
            assert result['total'] == 2

def test_get_segment_distribution(mock_segmentation):
    mock_segments = [
        (Segments.CHAMPION, 10),
        (Segments.LOYAL, 20),
        (Segments.AT_RISK, 15),
        (Segments.NEW, 5)
    ]

    mock_segmentation.db.query.return_value.group_by.return_value.all.return_value = mock_segments

    df = mock_segmentation.get_segment_distribution()

    assert len(df) == 4
    assert 'segment' in df.columns
    assert 'count' in df.columns
    assert 'percentage' in df.columns
    assert abs(df['percentage'].sum() - 100.0) < 0.01

def test_get_cluster_analysis(mock_segmentation):

    mock_results = [
        (0, Segments.CHAMPION, 15.0, 8.5, 1200.0, 10),
        (1, Segments.LOYAL, 25.0, 6.2, 800.0, 15)
    ]

    mock = Mock()
    mock.fetchall.return_value = mock_results
    mock_segmentation.db.execute.return_value = mock

    df = mock_segmentation.get_cluster_analysis()

    assert len(df) == 2
    assert 'cluster' in df.columns
    assert 'segment_label' in df.columns
    assert 'customer_count' in df.columns

def test_get_customer_segment_info_exists(mock_segmentation):

    mock_segment = Mock(spec=CustomerSegment)
    mock_segment.customer_id = 1
    mock_segment.segment_label = Segments.CHAMPION
    mock_segment.recency = 10
    mock_segment.frequency = 5
    mock_segment.monetary = 1000.0
    mock_segment.segment_score = 0
    mock_segment.last_calculated = datetime.now()

    mock_segmentation.db.query.return_value.filter.return_value.first.return_value = mock_segment

    result = mock_segmentation.get_customer_segment_info(1)

    assert result is not None
    assert result['customer_id'] == 1
    assert result['segment_label'] == Segments.CHAMPION.value
    assert result['recency'] == 10


@pytest.fixture
def segmentation_with_real_data(mock_db):
    segmentation = Segmentation(mock_db)

    np.random.seed(42)
    n_customers = 100

    recency = np.random.exponential(30, n_customers).astype(int)
    frequency = np.random.poisson(3, n_customers) + 1
    monetary = np.random.lognormal(6, 1, n_customers)

    segmentation._test_data = pd.DataFrame({
        'customer_id': range(1, n_customers + 1),
        'recency': recency,
        'frequency': frequency,
        'monetary': monetary
    })

    return segmentation

def test_full_segmentation_pipeline(segmentation_with_real_data):

    segmentation = segmentation_with_real_data

    with patch.object(segmentation, 'fetch_rfm_data', return_value=segmentation._test_data):

        df_result, model, scaler = segmentation.perform_clustering(segmentation._test_data)
        assert len(df_result) == 100
        assert 'cluster' in df_result.columns
        assert 'segment_label' in df_result.columns
        assert all(isinstance(label, Segments) for label in df_result['segment_label'])

        segmentation.model = model
        segmentation.scaler = scaler

        predicted_segment = segmentation.predict_customer_segment(15, 8, 2000.0)
        assert isinstance(predicted_segment, Segments)