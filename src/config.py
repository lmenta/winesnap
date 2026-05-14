from __future__ import annotations
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    openrouter_api_key: str
    tavily_api_key: str

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
