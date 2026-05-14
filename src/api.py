"""FastAPI app — single /api/analyze endpoint."""

from __future__ import annotations
import base64
from fastapi import FastAPI, File, Form, UploadFile, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from openai import OpenAI
from tavily import TavilyClient

from src.config import settings
from src.vision import identify_wines, OPENROUTER_BASE
from src.search import search_wines_parallel
from src.recommend import pick_winner, OPENROUTER_BASE as REC_BASE

app = FastAPI(title="WineSnap", description="AI wine picker from photos.")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

vision_client = OpenAI(api_key=settings.openrouter_api_key, base_url=OPENROUTER_BASE)
rec_client = OpenAI(api_key=settings.openrouter_api_key, base_url=REC_BASE)
tavily_client = TavilyClient(api_key=settings.tavily_api_key)


@app.post("/api/analyze")
async def analyze(
    image: UploadFile = File(...),
    preference: str = Form(default="best price/quality ratio"),
):
    image_bytes = await image.read()
    if len(image_bytes) > 20 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Image too large (max 20MB)")

    # Step 1: identify wines in image
    try:
        wines = identify_wines(image_bytes, vision_client)
    except Exception as e:
        raise HTTPException(status_code=422, detail=f"Could not read image: {e}")

    if not wines:
        raise HTTPException(status_code=422, detail="No wine bottles could be identified in the image.")

    # Step 2: search for price + score for each wine
    wines = search_wines_parallel(wines, tavily_client)

    # Step 3: pick the winner
    result = pick_winner(wines, preference, rec_client)
    return result


@app.get("/health")
def health():
    return {"status": "ok"}
