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
                     FROM customer c
                              JOIN "order" o ON c.customer_id = o.customer_id
                              JOIN order_item oi ON o.order_id = oi.order_id
                              JOIN product p ON oi.product_id = p.product_id
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

                # Skip already purchased products
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

    def apply_business_filters(self, recommendations: dict, target_customer: int) -> dict:

        spending_query = text("""
                              SELECT AVG(p.price)                         as avg_price,
                                     STRING_AGG(DISTINCT p.category, ',') as categories
                              FROM customer c
                                       JOIN "order" o ON c.customer_id = o.customer_id
                                       JOIN order_item oi ON o.order_id = oi.order_id
                                       JOIN product p ON oi.product_id = p.product_id
                              WHERE c.customer_id = :customer_id
                                AND o.status = 'DELIVERED'
                              """)

        result = self.db.execute(spending_query, {"customer_id": target_customer}).fetchone()

        avg_price = float(result[0]) if result and result[0] else 50.0
        purchased_categories = result[1].split(',') if result and result[1] else []

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
                normalized_score = min(100, max(0, data['score'] * 10))

                rec_data.append({
                    'customer_id': customer_id,
                    'product_id': product_id,
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

    def generate_all_recommendations(self) -> int:
        try:
            cutoff_date = datetime.now() - timedelta(days=180)

            query = text("""
                         SELECT DISTINCT c.customer_id
                         FROM customer c
                                  JOIN "order" o ON c.customer_id = o.customer_id
                         WHERE o.order_date > :cutoff_date
                           AND o.status = 'DELIVERED'
                         ORDER BY c.customer_id
                         """)

            customers = self.db.execute(query, {"cutoff_date": cutoff_date}).fetchall()

            processed_count = 0

            for customer_row in customers:
                customer_id = customer_row[0]
                try:
                    rec_count = self.generate_customer_recommendations(customer_id)
                    if rec_count > 0:
                        processed_count += 1

                    if processed_count % 50 == 0:
                        logger.info(f"Processed {processed_count} customers")

                except Exception as e:
                    logger.error(f"Failed processing customer {customer_id}: {e}")
                    continue

            logger.info(f"Generated recommendations for {processed_count} customers")
            return processed_count

        except Exception as e:
            logger.error(f"Failed to generate all recommendations: {e}")
            raise