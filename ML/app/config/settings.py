from pydantic_settings import BaseSettings
from pydantic import Field

class Settings(BaseSettings):
    database_url: str = Field(..., env="DATABASE_URL")
    app_name: str = Field(..., env="APP_NAME")

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
    }

settings = Settings()