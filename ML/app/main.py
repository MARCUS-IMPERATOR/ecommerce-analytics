import logging
from events.ml_event_consumer import MLEventConsumer
from config.database import SessionLocal

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
logger = logging.getLogger(__name__)

def main():
    kafka_config = {'bootstrap_servers': ['localhost:9092'], 'group_id': 'ml-processing-group'}
    consumer = MLEventConsumer(db_session_factory=SessionLocal, kafka_config=kafka_config)

    success = False
    try:
        logger.info("Starting ML Event Consumer (main)...")
        consumer.start_consuming()
        success = True
    except KeyboardInterrupt:
        logger.info("KeyboardInterrupt received — shutting down consumer...")
        consumer.stop_consuming()
    except Exception:
        logger.exception("Consumer failed with an unexpected exception")
    finally:
        try:
            consumer.stop_consuming()
        except Exception:
            pass

        if success:
            print("MAIN RAN CORRECTLY")
            logger.info("Main finished successfully")
        else:
            logger.info("Main exited (interrupted or failed)")

if __name__ == "__main__":
    main()