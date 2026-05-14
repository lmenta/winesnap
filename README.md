# WineSnap

AI wine picker from photos. Upload a photo of a wine shelf or supermarket aisle — the app identifies every bottle, looks up live prices and critic scores, and recommends the best value for your preference.

## How it works

1. **Vision** — Gemini reads wine labels from your photo
2. **Search** — Tavily looks up live price + critic score for each wine
3. **Recommend** — LLM picks the best match and explains why

## Stack

- Python + FastAPI backend
- Next.js frontend
- OpenRouter (LLM + vision)
- Tavily (web search)
- Railway (backend) + Vercel (frontend)

## Setup

```bash
# Backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env  # add your keys
uvicorn src.api:app --reload

# Frontend
cd frontend && npm install
echo "NEXT_PUBLIC_API_URL=http://localhost:8000" > .env.local
npm run dev
```

## Keys needed

- `OPENROUTER_API_KEY` — openrouter.ai
- `TAVILY_API_KEY` — tavily.com (free tier: 1000 searches/month)
