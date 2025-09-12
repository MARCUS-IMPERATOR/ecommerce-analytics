import pandas as pd
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
from sqlalchemy import text
from datetime import datetime, timedelta
import logging

from sqlalchemy.orm import Session

logger = logging.getLogger(__name__)

class ProductRecommendationSystem:
    def __init__(self, db: Session, num_neighbors: int = 15):
        self.db = db
        self.num_neighbors = num_neighbors

    def load_interaction_data(self) -> pd.DataFrame:
        query = text("""
                     SELECT c.customer_id,
                            p.product_id,
                            p.category,
                            p.price,
                            p.brand,
                            SUM(oi.quantity)           as quantity,
                            COUNT(DISTINCT o.order_id) as order_frequency,
                            MAX(o.order_date)          as last_purchase
                     FROM customers c
                              JOIN "orders" o ON c.customer_id = o.customer_id
                              JOIN order_items oi ON o.order_id = oi.order_id
                              JOIN products p ON oi.product_id = p.product_id
                     WHERE o.status = 'DELIVERED'
                     GROUP BY c.customer_id, p.product_id, p.category, p.price, p.brand
                     """)

        result = self.db.execute(query).fetchall()
        df = pd.DataFrame(result, columns=[
            'customer_id', 'product_id', 'category', 'price', 'brand',
            'quantity', 'order_frequency', 'last_purchase'
        ])

        df['days_since_purchase'] = (datetime.now() - pd.to_datetime(df['last_purchase'])).dt.days
        df['recency_weight'] = np.exp(-df['days_since_purchase'] / 365)
        df['interaction_score'] = df['quantity'] * df['order_frequency'] * df['recency_weight']

        return df

    def create_similarity_matrix(self, interaction_df: pd.DataFrame):

        user_item_matrix = interaction_df.pivot_table(
            index='customer_id',
            columns='product_id',
            values='interaction_score',
            fill_value=0
        )

        return user_item_matrix

    def find_similar_customers(self, user_item_matrix, target_customer: int) -> list:
        if target_customer not in user_item_matrix.index:
            return []

        target_vector = user_item_matrix.loc[target_customer].values.reshape(1, -1)
        similarities = cosine_similarity(target_vector, user_item_matrix.values)[0]

        customer_similarities = [
            (customer_id, similarity)
            for customer_id, similarity in zip(user_item_matrix.index, similarities)
            if customer_id != target_customer and similarity > 0.1
        ]

        return sorted(customer_similarities, key=lambda x: x[1], reverse=True)[:self.num_neighbors]

    def generate_recommendations(self, target_customer: int, similar_customers: list,
                                 interaction_df: pd.DataFrame) -> dict:

        purchased_products = set(
            interaction_df[interaction_df['customer_id'] == target_customer]['product_id']
        )

        recommendations = {}

        for similar_customer, similarity in similar_customers:
            similar_purchases = interaction_df[
                interaction_df['customer_id'] == similar_customer
                ]

            for _, product in similar_purchases.iterrows():
                product_id = product['product_id']

                if product_id in purchased_products:
                    continue

                if product_id not in recommendations:
                    recommendations[product_id] = {
                        'score': 0,
                        'category': product['category'],
                        'price': float(product['price']),
                        'brand': product['brand'],
                        'recommender_count': 0
                    }

                weighted_score = product['interaction_score'] * similarity
                recommendations[product_id]['score'] += weighted_score
                recommendations[product_id]['recommender_count'] += 1

        return recommendations

    def apply_business_filters_optimized(self, recommendations: dict, customer_profile: dict) -> dict:
        avg_price = customer_profile.get('avg_price', 50.0)
        purchased_categories = customer_profile.get('purchased_categories', [])

        filtered = {}
        category_counts = {}

        sorted_recs = sorted(recommendations.items(), key=lambda x: x[1]['score'], reverse=True)

        for product_id, rec_data in sorted_recs:
            category = rec_data['category']
            price = rec_data['price']

            if not (avg_price * 0.5 <= price <= avg_price * 2.0):
                continue

            category_counts.setdefault(category, 0)
            if category_counts[category] >= 3:
                continue

            if category not in purchased_categories:
                rec_data['score'] *= 1.2

            if rec_data['recommender_count'] >= 3:
                rec_data['score'] *= 1.1

            rec_data['score'] = float(rec_data['score'])

            filtered[product_id] = rec_data
            category_counts[category] += 1

            if len(filtered) >= 10:
                break

        return filtered

    def save_recommendations(self, customer_id: int, recommendations: dict):
        if not recommendations:
            return

        try:
            self.db.execute(
                text("DELETE FROM product_recommendations WHERE customer_id = :customer_id"),
                {"customer_id": customer_id}
            )

            rec_data = []
            for product_id, data in recommendations.items():

                raw_score = float(data['score'])
                normalized_score = min(1.0, max(0.0, raw_score / 100.0))

                rec_data.append({
                    'customer_id': customer_id,
                    'product_id': int(product_id),
                    'score': round(normalized_score, 2),
                    'created_at': datetime.now(),
                    'updated_at': datetime.now()
                })

            insert_query = text("""
                                INSERT INTO product_recommendations
                                    (customer_id, product_id, score, created_at, updated_at)
                                VALUES (:customer_id, :product_id, :score, :created_at, :updated_at)
                                """)

            for rec in rec_data:
                self.db.execute(insert_query, rec)

            self.db.commit()
            logger.info(f"Saved {len(rec_data)} recommendations for customer {customer_id}")

        except Exception as e:
            self.db.rollback()
            logger.error(f"Failed to save recommendations for customer {customer_id}: {e}")
            raise

    def generate_customer_recommendations(self, customer_id: int) -> int:
        try:
            interaction_data = self.load_interaction_data()
            user_item_matrix = self.create_similarity_matrix(interaction_data)

            similar_customers = self.find_similar_customers(user_item_matrix, customer_id)

            if not similar_customers:
                logger.warning(f"No similar customers found for {customer_id}")
                return 0

            recommendations = self.generate_recommendations(
                customer_id, similar_customers, interaction_data
            )
            filtered_recommendations = self.apply_business_filters(recommendations, customer_id)

            self.save_recommendations(customer_id, filtered_recommendations)

            return len(filtered_recommendations)

        except Exception as e:
            logger.error(f"Failed to generate recommendations for {customer_id}: {e}")
            raise

    def _load_all_customer_profiles(self, customer_ids: list) -> dict:
        if not customer_ids:
            return {}

        ids_str = ','.join(map(str, customer_ids))

        query = text(f"""
                     SELECT c.customer_id,
                            AVG(p.price)                         as avg_price,
                            STRING_AGG(DISTINCT p.category, ',') as categories
                     FROM customers c
                              JOIN "orders" o ON c.customer_id = o.customer_id
                              JOIN order_items oi ON o.order_id = oi.order_id
                              JOIN products p ON oi.product_id = p.product_id
                     WHERE c.customer_id IN ({ids_str})
                       AND o.status = 'DELIVERED'
                     GROUP BY c.customer_id
                     """)

        result = self.db.execute(query).fetchall()

        profiles = {}
        for row in result:
            customer_id = row[0]
            profiles[customer_id] = {
                'avg_price': float(row[1]) if row[1] else 50.0,
                'purchased_categories': row[2].split(',') if row[2] else []
            }

        return profiles

    def _prepare_recommendation_records(self, customer_id: int, recommendations: dict) -> list:
        records = []
        for product_id, data in recommendations.items():
            raw_score = float(data['score'])
            normalized_score = min(1.0, max(0.0, raw_score / 100.0))

            records.append({
                'customer_id': customer_id,
                'product_id': int(product_id),
                'score': round(normalized_score, 2),
                'created_at': datetime.now(),
                'updated_at': datetime.now()
            })

        return records

    def _batch_insert_recommendations(self, batch_records: list):
        if not batch_records:
            return

        try:
            customer_ids = list(set(record['customer_id'] for record in batch_records))
            delete_query = text("""
                                DELETE
                                FROM product_recommendations
                                WHERE customer_id = ANY (:customer_ids)
                                """)

            self.db.execute(delete_query, {"customer_ids": customer_ids})

            insert_query = text("""
                                INSERT INTO product_recommendations
                                    (customer_id, product_id, score, created_at, updated_at)
                                VALUES (:customer_id, :product_id, :score, :created_at, :updated_at)
                                """)

            self.db.execute(insert_query, batch_records)
            self.db.commit()

            logger.info(f"Batch inserted {len(batch_records)} recommendations")

        except Exception as e:
            self.db.rollback()
            logger.error(f"Failed batch insert: {e}")
            raise

    def generate_all_recommendations(self) -> int:
        try:
            cutoff_date = datetime.now() - timedelta(days=180)

            query = text("""
                         SELECT DISTINCT c.customer_id
                         FROM customers c
                                  JOIN "orders" o ON c.customer_id = o.customer_id
                         WHERE o.order_date > :cutoff_date
                           AND o.status = 'DELIVERED'
                         ORDER BY c.customer_id
                         """)

            customers = self.db.execute(query, {"cutoff_date": cutoff_date}).fetchall()
            customer_ids = [row[0] for row in customers]

            logger.info("Loading interaction data once for all customers...")
            interaction_data = self.load_interaction_data()
            user_item_matrix = self.create_similarity_matrix(interaction_data)

            logger.info("Pre-loading customer spending patterns...")
            customer_profiles = self._load_all_customer_profiles(customer_ids)

            processed_count = 0
            batch_size = 50
            all_recommendations = []

            for i in range(0, len(customer_ids), batch_size):
                batch = customer_ids[i:i + batch_size]
                logger.info(
                    f"Processing batch {i // batch_size + 1}/{(len(customer_ids) + batch_size - 1) // batch_size}")

                batch_recommendations = []

                for customer_id in batch:
                    try:
                        similar_customers = self.find_similar_customers(user_item_matrix, customer_id)

                        if not similar_customers:
                            continue

                        recommendations = self.generate_recommendations(
                            customer_id, similar_customers, interaction_data
                        )

                        customer_profile = customer_profiles.get(customer_id, {})
                        filtered_recommendations = self.apply_business_filters_optimized(
                            recommendations, customer_profile
                        )

                        if filtered_recommendations:
                            batch_recommendations.extend(
                                self._prepare_recommendation_records(customer_id, filtered_recommendations)
                            )
                            processed_count += 1

                    except Exception as e:
                        logger.error(f"Failed processing customer {customer_id}: {e}")
                        continue

                if batch_recommendations:
                    self._batch_insert_recommendations(batch_recommendations)
                    logger.info(f"Saved {len(batch_recommendations)} recommendations for batch")

            logger.info(f"Generated recommendations for {processed_count} customers")
            return processed_count

        except Exception as e:
            logger.error(f"Failed to generate all recommendations: {e}")
            raise