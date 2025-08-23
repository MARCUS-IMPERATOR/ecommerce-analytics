from app.config.database import engine, Session
from app.models.database import Customer, Product, Order
from sqlalchemy import text


def test_database_connection():
    try:
        with engine.connect() as connection:
            result = connection.execute(text("SELECT 1"))
            print("Database connection successful")

        db = Session()
        customer_count = db.query(Customer).count()
        product_count = db.query(Product).count()
        order_count = db.query(Order).count()

        print(f"Found {customer_count} customers")
        print(f"Found {product_count} products")
        print(f"Found {order_count} orders")

        db.close()
        return True

    except Exception as e:
        print(f"Database connection failed: {str(e)}")
        return False


if __name__ == "__main__":
    test_database_connection()