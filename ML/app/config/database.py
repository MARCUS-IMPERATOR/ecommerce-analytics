from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from .settings import settings

engine = create_engine(
    settings.database_url,
    pool_pre_ping=True,
    pool_recycle=300,
    future=True,
)

SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False, future=True)

Base = declarative_base()

def get_db():

    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

def init_db():
    Base.metadata.create_all(bind=engine)
