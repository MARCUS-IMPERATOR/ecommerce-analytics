import json
import time
from dataclasses import dataclass
from datetime import datetime, timedelta
from enum import Enum
from typing import Optional, Any, Dict
from contextlib import contextmanager

import numpy as np
import pandas as pd
from kafka import KafkaConsumer, KafkaProducer
from kafka.errors import KafkaError
from sqlalchemy.orm import Session

from app.services.forecasting import Forecasting
from app.services.recommendation import ProductRecommendationSystem
from app.services.segmentation import Segmentation
import logging

logger = logging.getLogger(__name__)


class Events(Enum):
    CUSTOMER_CREATED = "CUSTOMER_CREATED"
    CUSTOMER_UPDATED = "CUSTOMER_UPDATED"
    ORDER_CREATED = "ORDER_CREATED"
    ORDER_UPDATED = "ORDER_UPDATED"
    INITIAL_DATA_GENERATED = "INITIAL_DATA_GENERATED"
    FORECAST_REQUESTED = "FORECAST_REQUESTED"


@dataclass
class MLEvent:
    customer_id: str
    event_id: str
    event_type: str
    data: Optional[Any] = None
    timestamp: Optional[Any] = None

    @classmethod
    def from_json(cls, json_obj: Dict[str, Any]) -> "MLEvent":
        customer_id = str(json_obj.get("customerId", json_obj.get("customer_id", "")))
        event_type = json_obj.get("eventType", json_obj.get("event_type", ""))
        event_id = json_obj.get("eventId", json_obj.get("event_id", f"evt-{customer_id}-{event_type}"))

        data = json_obj.get("data")

        return cls(
            customer_id=customer_id,
            event_id=event_id,
            event_type=str(event_type),
            data=data,
            timestamp=json_obj.get("timestamp"),
        )


class MLEventConsumer:
    def __init__(self, db_session_factory, kafka_config: Dict[str, Any]):
        self.db_session_factory = db_session_factory
        self.kafka_config = kafka_config
        self.consumer = None
        self.running = False

        self.forecasting_service = None
        self.recommendation_service = None
        self.segmentation_service = None

    @contextmanager
    def get_db(self):
        session = self.db_session_factory()
        try:
            yield session
            session.commit()
        except Exception:
            session.rollback()
            raise
        finally:
            session.close()

    def initialize_services(self, db_session: Session):
        self.forecasting_service = Forecasting(db_session)
        self.recommendation_service = ProductRecommendationSystem(db_session)
        self.segmentation_service = Segmentation(db_session)

    def setup_consumer(self):
        consumer_config = {
            'bootstrap_servers': self.kafka_config.get('bootstrap_servers', ['localhost:9092']),
            'group_id': self.kafka_config.get('group_id', 'ml-processing-group'),
            'auto_offset_reset': self.kafka_config.get('auto_offset_reset', 'latest'),
            'enable_auto_commit': False,
            'session_timeout_ms': 300000,
            'heartbeat_interval_ms': 10000,
            'max_poll_interval_ms': 600000,
            'max_poll_records': 1,
            'value_deserializer': lambda x: json.loads(x.decode('utf-8')) if x else None,
            'key_deserializer': lambda x: x.decode('utf-8') if x else None,
        }

        self.consumer = KafkaConsumer('ml-events', **consumer_config)
        logger.info("Kafka consumer initialized")

    def process_customer_created(self, event: MLEvent):
        logger.info(f"Processing CUSTOMER_CREATED for customer {event.customer_id}")

        with self.get_db() as session:
            self.initialize_services(session)

            try:
                recommendations = self.recommendation_service.generate_customer_recommendations(event.customer_id)
                logger.info(f"Generated {recommendations} recommendations for new customer {event.customer_id}")

                segment = self.segmentation_service.update_all_segments()
                logger.info(f"Updated segmentation after new customer: {segment}")

            except Exception as e:
                logger.error(f"Error processing CUSTOMER_CREATED for {event.customer_id}: {e}")
                raise

    def process_customer_updated(self, event: MLEvent):
        logger.info(f"Processing CUSTOMER_UPDATED for customer {event.customer_id}")
        with self.get_db() as session:
            self.initialize_services(session)

            try:
                recommendation = self.recommendation_service.generate_customer_recommendations(event.customer_id)
                logger.info(f"Updated {recommendation} recommendations for customer {event.customer_id}")

                segment = self.segmentation_service.update_all_segments()
                logger.info(f"Updated segmentation after customer update: {segment}")

            except Exception as e:
                logger.error(f"Error processing CUSTOMER_UPDATED for {event.customer_id}: {e}")
                raise

    def process_order_created(self, event: MLEvent):
        order_id = event.data
        logger.info(f"Processing ORDER_CREATED for customer {event.customer_id}, order {order_id}")

        with self.get_db() as session:
            self.initialize_services(session)

            try:
                recommendation = self.recommendation_service.generate_customer_recommendations(event.customer_id)
                logger.info(f"Updated {recommendation} recommendations after new order for customer {event.customer_id}")

                segment = self.segmentation_service.update_all_segments()
                logger.info(f"Updated segmentation after new order: {segment}")

            except Exception as e:
                logger.error(f"Error processing ORDER_CREATED for customer {event.customer_id}: {e}")
                raise

    def process_order_updated(self, event: MLEvent):
        order_id = event.data
        logger.info(f"Processing ORDER_UPDATED for customer {event.customer_id}, order {order_id}")

        with self.get_db() as session:
            self.initialize_services(session)

            try:
                recommendation = self.recommendation_service.generate_customer_recommendations(event.customer_id)
                logger.info(f"Updated {recommendation} recommendations after order update for customer {event.customer_id}")

                segment = self.segmentation_service.update_all_segments()
                logger.info(f"Updated segmentation after order update: {segment}")

            except Exception as e:
                logger.error(f"Error processing ORDER_UPDATED for customer {event.customer_id}: {e}")
                raise

    def process_initial_data_generated(self, event: MLEvent):
        logger.info("Processing INITIAL_DATA_GENERATED ")

        with self.get_db() as session:
            self.initialize_services(session)

            try:
                logger.info("Starting full customer segmentation...")
                segmentation_stats = self.segmentation_service.update_all_segments()
                logger.info(f"Segmentation completed: {segmentation_stats}")

                logger.info("Starting recommendation generation for all customers...")
                processed_customers = self.recommendation_service.generate_all_recommendations()
                logger.info(f"Generated recommendations for {processed_customers} customers")

                logger.info("Starting sales forecasting...")
                forecast_results = self.forecasting_service.create_forecast()
                logger.info(f"Forecasting completed with MAE: {forecast_results.get('test_mae', 'N/A')}")

                logger.info("Full ML pipeline completed successfully")

            except Exception as e:
                logger.error(f"Error in full ML pipeline processing: {e}")
                raise

    def process_event(self, event: MLEvent):
        event_processors = {
            Events.CUSTOMER_CREATED.value: self.process_customer_created,
            Events.CUSTOMER_UPDATED.value: self.process_customer_updated,
            Events.ORDER_CREATED.value: self.process_order_created,
            Events.ORDER_UPDATED.value: self.process_order_updated,
            Events.INITIAL_DATA_GENERATED.value: self.process_initial_data_generated,
            Events.FORECAST_REQUESTED.value: self.process_forecast_requested,
        }

        processor = event_processors.get(event.event_type)
        if processor:
            try:
                processor(event)
                logger.info(f"Successfully processed {event.event_type} for customer {event.customer_id}")
            except Exception as e:
                logger.error(f"Failed to process {event.event_type} for customer {event.customer_id}: {e}")
                raise
        else:
            logger.warning(f"Unknown event type: {event.event_type}")

    def start_consuming(self):
        if not self.consumer:
            self.setup_consumer()

        self.running = True
        logger.info("Starting ML event consumer...")

        try:
            while self.running:
                message_batch = self.consumer.poll(timeout_ms=1000)

                for topic_partition, messages in message_batch.items():
                    for message in messages:
                        try:
                            if message.value is None:
                                continue

                            logger.info(f"Raw message received: {message.value}")
                            logger.info(f"Message key: {message.key}")
                            logger.info(
                                f"Message keys: {list(message.value.keys()) if isinstance(message.value, dict) else 'Not a dict'}")

                            event = MLEvent.from_json(message.value)
                            logger.info(
                                f"Parsed event - Type: '{event.event_type}', Customer: {event.customer_id}, Event ID: {event.event_id}")
                            logger.info(f"Event data after parsing: {event.data}")

                            if not event.event_type:
                                logger.error(f"Empty event_type! Raw message: {message.value}")
                                continue

                            self.process_event(event)

                        except json.JSONDecodeError as e:
                            logger.error(f"Failed to decode message: {e}")
                        except Exception as e:
                            logger.error(f"Error processing message: {e}")
                            continue

        except KeyboardInterrupt:
            logger.info("Consumer interrupted by user")
        except KafkaError as e:
            logger.error(f"Kafka error: {e}")
        except Exception as e:
            logger.error(f"Unexpected error in consumer: {e}")
        finally:
            self.stop_consuming()

    def process_forecast_requested(self, event: MLEvent):
        logger.info("Processing FORECAST_REQUESTED event")

        response_key = None
        forecast_days = 30
        include_confidence = True
        model_comparison = True

        if isinstance(event.data, dict):
            response_key = event.data.get('requestId')
            forecast_days = event.data.get('forecastDays', 30)
            include_confidence = event.data.get('includeConfidenceIntervals', True)
            model_comparison = event.data.get('modelComparison', True)

            logger.info(
                f"Request parameters - Days: {forecast_days}, Confidence: {include_confidence}, Comparison: {model_comparison}")

        if not response_key:
            logger.error(f"Could not find requestId in event data. Data: {event.data}")
            response_key = event.event_id

        logger.info(f"Using requestId as response key: {response_key}")

        with self.get_db() as session:
            self.initialize_services(session)

            try:
                start_time = time.time()

                logger.info(f"Starting forecast generation for {forecast_days} days...")
                forecast_results = self.forecasting_service.create_forecast(forecast_days)
                logger.info(f"Forecast generation completed. Results keys: {list(forecast_results.keys())}")

                forecast_response = self.prepare_forecast_response(
                    forecast_results, forecast_days, include_confidence, model_comparison, start_time
                )
                logger.info("Response preparation completed")

                self.send_forecast_response(response_key, forecast_response)

            except Exception as e:
                logger.error(f"Error processing FORECAST_REQUESTED: {e}", exc_info=True)
                start_time = start_time if 'start_time' in locals() else time.time()
                error_response = {
                    "success": False,
                    "error": str(e),
                    "processingTimeMs": int((time.time() - start_time) * 1000)
                }
                self.send_forecast_response(response_key, error_response)

    def prepare_forecast_response(self, forecast_results, forecast_days, include_confidence, model_comparison,
                                  start_time):

        processing_time = int((time.time() - start_time) * 1000)

        logger.info(f"Preparing response - forecast_results keys: {list(forecast_results.keys())}")

        forecast_data = []
        today = datetime.now().date()

        try:
            historical_data = forecast_results.get('historical_data', pd.DataFrame())

            if not historical_data.empty:
                logger.info(f"Processing {len(historical_data)} historical data points")

                historical_limit = min(90, len(historical_data))
                recent_historical = historical_data.tail(historical_limit)

                for date, row in recent_historical.iterrows():
                    try:
                        date_str = date.strftime("%Y-%m-%d") if hasattr(date, 'strftime') else str(date)[:10]
                        sales_value = float(row['sales']) if pd.notna(row['sales']) else 0.0

                        forecast_data.append({
                            "date": date_str,
                            "actualSales": sales_value,
                            "predictedSales": None,
                            "confidenceLower": None,
                            "confidenceUpper": None
                        })
                    except Exception as e:
                        logger.warning(f"Error processing historical point: {e}")
                        continue

            predictions = forecast_results.get('predictions', [])
            confidence_lower = forecast_results.get('confidence_lower', [])
            confidence_upper = forecast_results.get('confidence_upper', [])

            logger.info(f"Adding {len(predictions)} forecast predictions")
            logger.info(
                f"Prediction values range: {np.min(predictions) if len(predictions) > 0 else 'None'} to {np.max(predictions) if len(predictions) > 0 else 'None'}")

            for i in range(len(predictions)):
                try:
                    forecast_date = today + timedelta(days=i + 1)

                    pred_val = float(predictions[i]) if i < len(predictions) else 0.0

                    conf_lower = None
                    conf_upper = None
                    if include_confidence:
                        conf_lower = float(confidence_lower[i]) if i < len(confidence_lower) else None
                        conf_upper = float(confidence_upper[i]) if i < len(confidence_upper) else None

                    forecast_data.append({
                        "date": forecast_date.strftime("%Y-%m-%d"),
                        "actualSales": None,
                        "predictedSales": pred_val,
                        "confidenceLower": conf_lower,
                        "confidenceUpper": conf_upper
                    })
                except Exception as e:
                    logger.warning(f"Error processing forecast point {i}: {e}")
                    continue

        except Exception as e:
            logger.error(f"Error processing forecast data: {e}")

        model_metrics = []
        if model_comparison:
            try:
                model_performance = forecast_results.get('model_performance', {})
                logger.info(f"Processing {len(model_performance)} model metrics")

                for model_name, metrics in model_performance.items():
                    try:
                        model_metrics.append({
                            "modelName": str(model_name),
                            "trainMae": float(metrics.get('train_mae', 0)),
                            "testMae": float(metrics.get('test_mae', 0)),
                            "trainRmse": float(metrics.get('train_rmse', 0)),
                            "testRmse": float(metrics.get('test_rmse', 0))
                        })
                    except Exception as e:
                        logger.warning(f"Error processing model {model_name}: {e}")

            except Exception as e:
                logger.error(f"Error processing model metrics: {e}")

        try:
            historical_points = [fp for fp in forecast_data if fp.get('actualSales') is not None]
            forecast_points = [fp for fp in forecast_data if fp.get('predictedSales') is not None]

            avg_conf_width = 0.0
            if include_confidence:
                conf_widths = []
                for fp in forecast_points:
                    if fp.get('confidenceUpper') is not None and fp.get('confidenceLower') is not None:
                        conf_widths.append(abs(fp['confidenceUpper'] - fp['confidenceLower']))
                avg_conf_width = sum(conf_widths) / len(conf_widths) if conf_widths else 0.0

            test_mae = forecast_results.get('test_mae', 0)
            historical_sales = [fp['actualSales'] for fp in historical_points if
                                fp['actualSales'] is not None and fp['actualSales'] > 0]
            avg_sales = sum(historical_sales) / len(historical_sales) if historical_sales else 1
            accuracy_score = max(0, min(1, 1 - (test_mae / avg_sales))) if avg_sales > 0 else 0

        except Exception as e:
            logger.error(f"Error calculating summary: {e}")
            avg_conf_width = 0.0
            accuracy_score = 0.0

        response = {
            "success": True,
            "bestModel": str(forecast_results.get('best_model', 'Unknown')),
            "modelMetrics": model_metrics if model_comparison else None,
            "forecastData": forecast_data,
            "summary": {
                "totalDataPoints": len(historical_points),
                "forecastHorizonDays": len(forecast_points),
                "averageConfidenceIntervalWidth": avg_conf_width,
                "modelAccuracyScore": accuracy_score
            },
            "processingTimeMs": processing_time
        }

        logger.info(f"Final response summary:")
        logger.info(f"  - Success: {response['success']}")
        logger.info(f"  - Best model: {response['bestModel']}")
        logger.info(f"  - Historical points: {response['summary']['totalDataPoints']}")
        logger.info(f"  - Forecast points: {response['summary']['forecastHorizonDays']}")
        logger.info(f"  - Model metrics: {len(response['modelMetrics']) if response['modelMetrics'] else 0}")

        return response

    def send_forecast_response(self, event_id, response_data):
        try:
            producer_config = {
                'bootstrap_servers': self.kafka_config.get('bootstrap_servers', ['localhost:9092']),
                'value_serializer': lambda x: json.dumps(x).encode('utf-8'),
                'key_serializer': lambda x: x.encode('utf-8') if x else None,
            }

            producer = KafkaProducer(**producer_config)

            producer.send('forecast-responses', key=event_id, value=response_data)
            producer.flush()
            producer.close()

            logger.info(f"Sent forecast response for event {event_id}")

        except Exception as e:
            logger.error(f"Failed to send forecast response: {e}")

    def stop_consuming(self):
        self.running = False
        if self.consumer:
            self.consumer.close()
            logger.info("Kafka consumer closed")