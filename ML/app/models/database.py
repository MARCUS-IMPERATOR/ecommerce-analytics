from sqlalchemy import Column, Integer, String, DateTime, Text, Numeric, ForeignKey, Enum
from sqlalchemy.orm import relationship
from sqlalchemy.ext.declarative import declarative_base
from datetime import datetime
import enum
Base = declarative_base()

class OrderStatus(enum.Enum):
    PENDING = "PENDING"
    DELIVERED = "DELIVERED"
    CANCELLED = "CANCELLED"

class ProductCategory(enum.Enum):
    LAPTOPS = "LAPTOPS"
    SMARTPHONES = "SMARTPHONES"
    GAMING_CONSOLES = "GAMING_CONSOLES"
    ACCESSORIES = "ACCESSORIES"
    TABLETS = "TABLETS"
    OTHERS = "OTHERS"

class Segments(enum.Enum):
    CHAMPION = "CHAMPION"
    LOYAL = "LOYAL"
    AT_RISK = "AT_RISK"
    NEW = "NEW"


class AbstractAudit(Base):
    __abstract__ = True

    created_at = Column(DateTime, nullable=False, default=datetime.now())
    updated_at = Column(DateTime, nullable=True, onupdate=datetime.now())


class Customer(AbstractAudit):
    __tablename__ = "customers"

    customer_id = Column(Integer, primary_key=True, autoincrement=True)
    customer_code = Column(String(255), nullable=False, unique=True)
    first_name = Column(String(255), nullable=False)
    last_name = Column(String(255), nullable=False)
    age = Column(Integer, nullable=False)
    country = Column(String(255), nullable=False)
    email = Column(String(255), nullable=False)
    phone = Column(String(255), nullable=False)
    registration_date = Column(DateTime, nullable=False)
    total_spent = Column(Numeric(10, 2), nullable=False)
    order_count = Column(Integer, nullable=False)
    last_order_date = Column(DateTime, nullable=True)

    orders = relationship("Order", back_populates="customer")
    customer_segment = relationship("CustomerSegment", back_populates="customer", uselist=False)


class Product(AbstractAudit):
    __tablename__ = "products"

    product_id = Column(Integer, primary_key=True, autoincrement=True)
    sku = Column(String(255), nullable=False, unique=True)
    name = Column(String(255), nullable=False)
    description = Column(Text, nullable=False)
    category = Column(Enum(ProductCategory), nullable=False)
    brand = Column(String(255), nullable=False)
    price = Column(Numeric(10, 2), nullable=False)
    stock_quantity = Column(Integer, nullable=False, default=0)


class Order(AbstractAudit):
    __tablename__ = "orders"

    order_id = Column(Integer, primary_key=True, autoincrement=True)
    customer_id = Column(Integer, ForeignKey("customers.customer_id"), nullable=False)
    status = Column(Enum(OrderStatus), nullable=False)
    order_date = Column(DateTime, nullable=False)
    total_amount = Column(Numeric(10, 2), nullable=True)

    customer = relationship("Customer", back_populates="orders")
    order_items = relationship("OrderItem", back_populates="order")


class OrderItem(AbstractAudit):
    __tablename__ = "order_items"

    order_id = Column(Integer, ForeignKey("orders.order_id"), primary_key=True)
    product_id = Column(Integer, ForeignKey("products.product_id"), primary_key=True)
    quantity = Column(Integer, nullable=False)
    unit_price = Column(Numeric(10, 2), nullable=False)

    order = relationship("Order", back_populates="order_items")
    product = relationship("Product")


class CustomerSegment(AbstractAudit):
    __tablename__ = "customer_segments"

    customer_id = Column(Integer, ForeignKey("customers.customer_id"), primary_key=True)
    segment_label = Column(Enum(Segments), nullable=False)
    recency = Column(Integer, nullable=False)
    frequency = Column(Numeric(10, 2), nullable=False)
    monetary = Column(Numeric(10, 2), nullable=False)
    segment_score = Column(Integer, nullable=True)
    last_calculated = Column(DateTime, nullable=True)

    customer = relationship("Customer", back_populates="customer_segment")


class ProductRecommendation(AbstractAudit):
    __tablename__ = "product_recommendations"

    customer_id = Column(Integer, ForeignKey("customers.customer_id"), primary_key=True)
    product_id = Column(Integer, ForeignKey("products.product_id"), primary_key=True)
    score = Column(Numeric(10, 2), nullable=False)

    customer = relationship("Customer")
    product = relationship("Product")