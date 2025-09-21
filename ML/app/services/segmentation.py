import pandas as pd
import numpy as np
from sklearn.cluster import KMeans
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import silhouette_score

from datetime import datetime
from sqlalchemy.orm import Session
from sqlalchemy import func, text
from ..models.database import Customer, Order, CustomerSegment, Segments, OrderStatus
import logging

logger = logging.getLogger(__name__)
class Segmentation:
    def __init__(self, db: Session):
        self.db = db
        self.scaler = StandardScaler()
        self.model = None
        self.segment_mapping = {}

    def calculate_rfm_metrics(self, customer: Customer):
        now = datetime.now()

        last_order = (
            self.db.query(func.max(Order.order_date))
            .filter(Order.customer_id == customer.customer_id,
                    Order.status == OrderStatus.DELIVERED)
            .scalar()
        )

        recency = (now - last_order).days if last_order else None

        frequency = (
            self.db.query(func.count(Order.order_id))
            .filter(Order.customer_id == customer.customer_id,
                    Order.status == OrderStatus.DELIVERED)
            .scalar()
        )

        monetary = (
            self.db.query(func.coalesce(func.sum(Order.total_amount), 0))
            .filter(Order.customer_id == customer.customer_id,
                    Order.status == OrderStatus.DELIVERED)
            .scalar()
        )

        return recency, frequency, float(monetary)

    def fetch_customer_data(self) -> pd.DataFrame:
        try:
            query = text("""
                         SELECT c.customer_id,
                                CASE
                                    WHEN MAX(o.order_date) IS NOT NULL
                                        THEN EXTRACT(DAY FROM (NOW() - MAX(o.order_date)))::INTEGER
                         ELSE NULL
                         END AS recency,
                    COUNT(o.order_id) AS frequency,
                    COALESCE(SUM(o.total_amount), 0) AS monetary
                FROM customers c
                LEFT JOIN "orders" o ON c.customer_id = o.customer_id
                    AND o.status = 'DELIVERED'
                GROUP BY c.customer_id
                HAVING COUNT(o.order_id) > 0
                         """)

            result = self.db.execute(query).fetchall()
            df = pd.DataFrame(result, columns=['customer_id', 'recency', 'frequency', 'monetary'])

            df['recency'] = df['recency'].astype('Int64')
            df['frequency'] = df['frequency'].astype(int)
            df['monetary'] = df['monetary'].astype(float)

            logger.info(f"Loaded RFM data for {len(df)} customers")
            return df

        except Exception as e:
            logger.error(f"Failed to fetch customer data: {e}")
            raise

    def prepare_features(self, df: pd.DataFrame) -> tuple:
        df_clean = df.copy()

        max_recency = df_clean['recency'].max()
        df_clean['recency'] = df_clean['recency'].fillna(max_recency + 30)

        df_clean['recency_score'] = df_clean['recency'].max() - df_clean['recency']
        df_clean['frequency_log'] = np.log1p(df_clean['frequency'])
        df_clean['monetary_log'] = np.log1p(df_clean['monetary'])

        features = ['recency_score', 'frequency_log', 'monetary_log']
        X = self.scaler.fit_transform(df_clean[features])

        return X, df_clean

    def find_best_clusters(self, X: np.ndarray) -> int:
        best_k, best_score = 2, -1
        for k in range(2, min(7, len(X))):
            try:
                kmeans = KMeans(n_clusters=k, random_state=42, n_init=10)
                labels = kmeans.fit_predict(X)

                if len(np.unique(labels)) > 1:
                    score = silhouette_score(X, labels)
                    if score > best_score:
                        best_score = score
                        best_k = k
            except Exception as e:
                logger.warning(f"Clustering failed for k={k}: {e}")
        logger.info(f"Best cluster count: {best_k} (score: {best_score:.3f})")
        return best_k

    def create_segments(self, df: pd.DataFrame) -> pd.DataFrame:
        if df.empty:
            raise ValueError("No customer data provided")

        X, df_processed = self.prepare_features(df)
        optimal_k = self.find_best_clusters(X)

        self.model = KMeans(n_clusters=optimal_k, random_state=42, n_init=10)
        clusters = self.model.fit_predict(X)

        df_result = df.copy()
        df_result['cluster'] = clusters

        self.segment_mapping = self.assign_segment_labels(df_result)
        df_result['segment_label'] = df_result['cluster'].map(self.segment_mapping)

        logger.info(f"Created {optimal_k} customer segments")
        return df_result

    def assign_segment_labels(self, df: pd.DataFrame) -> dict:
        cluster_stats = df.groupby('cluster').agg({
            'recency': 'mean',
            'frequency': 'mean',
            'monetary': 'mean'
        })

        mapping = {}
        thresholds = {
            'recency': cluster_stats['recency'].median(),
            'frequency': cluster_stats['frequency'].median(),
            'monetary': cluster_stats['monetary'].median()
        }

        for cluster_id, stats in cluster_stats.iterrows():
            recency_good = stats['recency'] <= thresholds['recency']
            frequency_good = stats['frequency'] >= thresholds['frequency']
            monetary_good = stats['monetary'] >= thresholds['monetary']

            if recency_good and frequency_good and monetary_good:
                mapping[cluster_id] = Segments.CHAMPION
            elif frequency_good and monetary_good:
                mapping[cluster_id] = Segments.LOYAL
            elif not recency_good and (frequency_good or monetary_good):
                mapping[cluster_id] = Segments.AT_RISK
            else:
                mapping[cluster_id] = Segments.NEW

        return mapping

    def update_all_segments(self) -> dict:
        try:
            logger.info("Starting customer segmentation update...")

            df = self.fetch_customer_data()
            if df.empty:
                return {"updated": 0, "errors": 0, "total": 0}

            df_segmented = self.create_segments(df)

            updated_count = 0
            error_count = 0

            for _, row in df_segmented.iterrows():
                try:
                    segment = (
                        self.db.query(CustomerSegment)
                        .filter(CustomerSegment.customer_id == row['customer_id'])
                        .first()
                    )

                    if not segment:
                        customer = self.db.query(Customer).get(row['customer_id'])
                        if not customer:
                            error_count += 1
                            continue

                        segment = CustomerSegment(
                            customer_id=row['customer_id'],
                            customer=customer
                        )
                        self.db.add(segment)

                    segment.recency = int(row['recency']) if pd.notna(row['recency']) else None
                    segment.frequency = row['frequency']
                    segment.monetary = row['monetary']
                    segment.segment_label = row['segment_label']
                    segment.segment_score = int(row['cluster'])
                    segment.last_calculated = datetime.now()

                    updated_count += 1

                except Exception as e:
                    logger.error(f"Failed to update customer {row['customer_id']}: {e}")
                    error_count += 1

            self.db.commit()

            stats = {
                "updated": updated_count,
                "errors": error_count,
                "total": len(df_segmented),
                "segments": len(self.segment_mapping)
            }

            logger.info(f"Segmentation complete: {stats}")
            return stats

        except Exception as e:
            self.db.rollback()
            logger.error(f"Segmentation failed: {e}")
            raise