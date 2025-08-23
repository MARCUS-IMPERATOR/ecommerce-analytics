from fastapi import FastAPI, Depends
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from app.config.settings import settings
from app.config.database import init_db
from app.models.database import Customer

app = FastAPI(
    title=settings.app_name
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/health")
def health():
    return {"status": "ok", "service":settings.app_name}

@app.get("/test-db")
def test_db(db: Session = Depends(init_db)):
    try:
        count = db.query(Customer).count()
        return {"status": "ok", "count": count}
    except Exception as e:
        return {"status": "error", "message": str(e)}