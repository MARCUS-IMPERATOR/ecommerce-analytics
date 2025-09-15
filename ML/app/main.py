import logging
import threading
import os
import time
import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, Response
from fastapi.responses import JSONResponse
import uvicorn

# --- Prometheus imports ---
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST
from prometheus_fastapi_instrumentator import Instrumentator

from app.telemetry.tel_setup import setup_telemetry
from events.ml_event_consumer import MLEventConsumer
from config.database import SessionLocal

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)
tracer = setup_telemetry()

kafka_healthy = False
consumer_instance = None
consumer_thread = None
consumer_running = False

# --- Prometheus custom metrics ---
ml_predictions_total = Counter(
    'ml_predictions_total',
    'Total number of ML predictions made',
    ['model_name', 'prediction_type']
)

ml_prediction_duration = Histogram(
    'ml_prediction_duration_seconds',
    'Time spent on ML predictions',
    ['model_name']
)

ml_model_accuracy = Gauge(
    'ml_model_accuracy',
    'Current model accuracy score',
    ['model_name']
)

ml_kafka_messages_processed = Counter(
    'ml_kafka_messages_processed_total',
    'Total Kafka messages processed',
    ['topic', 'status']
)

ml_service_health = Gauge(
    'ml_service_health',
    'Health status of ML service components',
    ['component']
)


def start_kafka_consumer():
    """Start Kafka consumer in background thread"""
    global kafka_healthy, consumer_instance, consumer_running
    try:
        kafka_bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
        logger.info(f"Using Kafka bootstrap servers: {kafka_bootstrap_servers}")

        kafka_config = {
            'bootstrap_servers': kafka_bootstrap_servers.split(','),
            'group_id': 'ml-processing-group'
        }

        consumer_instance = MLEventConsumer(db_session_factory=SessionLocal, kafka_config=kafka_config)
        logger.info("Starting ML Event Consumer (background)...")

        consumer_running = True
        consumer_instance.start_consuming()
        kafka_healthy = True
        logger.info("Kafka consumer started successfully")

    except Exception as e:
        logger.exception("Consumer failed with exception")
        kafka_healthy = False
        consumer_running = False
        if consumer_instance:
            consumer_instance.stop_consuming()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage FastAPI application lifecycle"""
    global consumer_thread, kafka_healthy

    logger.info("Starting ML Service...")

    # Start Kafka consumer
    consumer_thread = threading.Thread(target=start_kafka_consumer, daemon=True)
    consumer_thread.start()

    await asyncio.sleep(2)
    kafka_healthy = True

    # Init Prometheus metrics
    ml_service_health.labels(component='kafka').set(1)
    ml_service_health.labels(component='model').set(1)
    ml_service_health.labels(component='database').set(1)

    ml_model_accuracy.labels(model_name='recommendation').set(0.89)
    ml_model_accuracy.labels(model_name='classification').set(0.92)

    yield

    logger.info("Shutting down ML Service...")
    global consumer_running, consumer_instance
    consumer_running = False
    kafka_healthy = False
    if consumer_instance:
        try:
            consumer_instance.stop_consuming()
        except Exception:
            pass


# --- Create FastAPI app ---
app = FastAPI(
    title="ML Service",
    description="Machine Learning Service for E-commerce Analytics",
    version="1.0.0",
    lifespan=lifespan
)

# Attach Prometheus Instrumentator
instrumentator = Instrumentator().instrument(app)
instrumentator.expose(app)


@app.get("/health")
async def health_check():
    return {"status": "ok", "timestamp": int(time.time()), "service": "ml-service", "version": "1.0.0"}


@app.get("/health/ready")
async def readiness_check():
    checks = {
        "kafka": kafka_healthy,
        "consumer": consumer_instance is not None,
        "consumer_running": consumer_running,
        "thread": consumer_thread is not None and consumer_thread.is_alive()
    }
    all_healthy = all(checks.values())
    return JSONResponse(status_code=200 if all_healthy else 503,
                        content={"status": "ready" if all_healthy else "not ready",
                                 "timestamp": int(time.time()), "checks": checks})


@app.get("/predict")
async def predict(data: dict):
    """Example prediction endpoint with metrics"""
    model_name = data.get('model', 'default')

    ml_predictions_total.labels(model_name=model_name, prediction_type='classification').inc()
    with ml_prediction_duration.labels(model_name=model_name).time():
        prediction = {"result": "example", "confidence": 0.95}

    ml_model_accuracy.labels(model_name=model_name).set(0.94)
    return prediction


@app.get("/metrics/prometheus")
async def prometheus_metrics():
    """Expose metrics in Prometheus format"""
    return Response(generate_latest(), media_type=CONTENT_TYPE_LATEST)


def process_kafka_message(topic: str, message: dict):
    try:
        result = process_message(message)
        ml_kafka_messages_processed.labels(topic=topic, status='success').inc()
        return result
    except Exception:
        ml_kafka_messages_processed.labels(topic=topic, status='error').inc()
        raise


def process_message(message):
    return {"processed": True}


def main():
    port = int(os.getenv('PORT', 8000))
    host = os.getenv('HOST', '0.0.0.0')
    logger.info(f"Starting FastAPI server on {host}:{port}")
    uvicorn.run(app, host=host, port=port, log_level="info", access_log=True)


if __name__ == "__main__":
    main()