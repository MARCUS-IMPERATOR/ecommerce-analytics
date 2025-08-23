from datetime import datetime, timedelta
from unittest.mock import Mock, patch, MagicMock

import numpy as np
import pandas as pd
import pytest
from sqlalchemy import Engine
from sqlalchemy.orm import Session

from app.services.recommendation import ProductRecommendationSystem


@pytest.fixture
def mock_db():
    return Mock(spec=Session)


@pytest.fixture
def mock_recommendation(mock_db):
    with patch('app.services.recommendation.create_engine') as mock_create_engine:
        mock_engine = Mock(spec=Engine)
        mock_create_engine.return_value = mock_engine

        system = ProductRecommendationSystem("mock://connection", k_neighbors=5)
        return system


@pytest.fixture
def mock_data():
    data = {
        'customer_id': [1, 1, 2, 2, 3, 3],
        'product_id': [101, 102, 101, 103, 102, 103],
        'category': ['LAPTOPS', 'SMARTPHONES', 'LAPTOPS', 'TABLETS', 'SMARTPHONES', 'TABLETS'],
        'price': [1000.0, 800.0, 1000.0, 600.0, 800.0, 600.0],
        'brand': ['Apple', 'Samsung', 'Apple', 'Apple', 'Samsung', 'Apple'],
        'total_quantity': [2, 1, 1, 3, 2, 1],
        'purchase_frequency': [1, 1, 1, 2, 1, 1],
        'last_purchase_date': [
            datetime.now() - timedelta(days=30),
            datetime.now() - timedelta(days=60),
            datetime.now() - timedelta(days=45),
            datetime.now() - timedelta(days=20),
            datetime.now() - timedelta(days=90),
            datetime.now() - timedelta(days=10)
        ],
        'interaction_score': [1.0, 0.8, 0.7, 0.9, 1.1, 0.95]
    }
    return pd.DataFrame(data)


def test_create_user_item_matrix(mock_recommendation, mock_data):
    user_item_matrix, processed_df = mock_recommendation.create_user_item_matrix(mock_data)

    assert user_item_matrix.shape[0] == 3
    assert user_item_matrix.shape[1] == 3

    assert 'interaction_score' in processed_df.columns
    assert 'recency_weight' in processed_df.columns

    assert (user_item_matrix >= 0).all().all()


def test_find_similar_customers(mock_recommendation):
    data = np.array([
        [5, 3, 0, 1],
        [4, 0, 0, 1],
        [1, 1, 0, 5],
        [1, 0, 0, 4],
    ])

    user_item_matrix = pd.DataFrame(
        data,
        index=[1, 2, 3, 4],
        columns=[101, 102, 103, 104]
    )

    similar_customers = mock_recommendation.find_similar_customers(user_item_matrix, 1)

    assert len(similar_customers) > 0
    assert all(isinstance(item, tuple) and len(item) == 2 for item in similar_customers)
    assert all(customer_id != 1 for customer_id, simCus in similar_customers)

    similar_customers_empty = mock_recommendation.find_similar_customers(user_item_matrix, 999)
    assert similar_customers_empty == []


def test_recommendations(mock_recommendation, mock_data):
    similar_customers = [(2, 0.8), (3, 0.6)]

    recommendations = mock_recommendation.generate_recommendations(
        target_customer_id=1,
        similar_customers=similar_customers,
        interaction_df=mock_data
    )
    print(recommendations)

    assert isinstance(recommendations, dict)

    for product_id, rec_data in recommendations.items():
        assert 'score' in rec_data
        assert 'category' in rec_data
        assert 'price' in rec_data
        assert 'brand' in rec_data
        assert 'similar_customers' in rec_data
        assert rec_data['score'] > 0


def test_apply_business_rules(mock_recommendation):
    mock_result = MagicMock()
    mock_result.__getitem__.side_effect = lambda x: {0: 750.0, 1: "LAPTOPS,SMARTPHONES"}[x]

    mock_connection = MagicMock()
    mock_connection.execute.return_value.fetchone.return_value = mock_result

    mock_recommendation.engine.connect.return_value = mock_connection

    recommendations = {
        101: {'score': 10, 'category': 'LAPTOPS', 'price': 800.0, 'brand': 'Apple', 'similar_customers': 2},
        102: {'score': 8, 'category': 'TABLETS', 'price': 600.0, 'brand': 'Samsung', 'similar_customers': 1},
        103: {'score': 6, 'category': 'SMARTPHONES', 'price': 1200.0, 'brand': 'Apple', 'similar_customers': 3},
    }

    filtered_recommendations = mock_recommendation.apply_business_rules(recommendations, 1)

    assert isinstance(filtered_recommendations, dict)
    assert len(filtered_recommendations) <= 10

    mock_recommendation.engine.connect.assert_called_once()
    mock_connection.execute.assert_called_once()
    mock_connection.execute.return_value.fetchone.assert_called_once()

def test_save_recommendations(mock_recommendation):
    mock_connection = MagicMock()
    mock_transaction = Mock()
    mock_connection.begin.return_value = mock_transaction

    cm = MagicMock()
    cm.__enter__.return_value = mock_connection
    mock_recommendation.engine.connect.return_value = cm

    recommendations = {
        101: {'score': 10, 'category': 'LAPTOPS', 'price': 800.0, 'brand': 'Apple', 'similar_customers': 2},
        102: {'score': 8, 'category': 'TABLETS', 'price': 600.0, 'brand': 'Samsung', 'similar_customers': 1},
    }

    mock_recommendation.save_recommendations(1, recommendations)

    mock_connection.execute.assert_called()
    mock_transaction.commit.assert_called_once()


