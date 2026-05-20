# WineSnap

> Take a photo of a wine shelf. Get an AI recommendation with live prices and critic scores in seconds.

**Live:** [winesnap-app.vercel.app](https://winesnap-app.vercel.app) · **Installable PWA** (works on mobile, add to home screen)

---

## What it does

Point your phone camera at any wine shelf — a supermarket aisle, a restaurant list, a cellar. WineSnap reads every label, looks up live prices and critic scores for each bottle, and recommends the best match for your preference.

No account needed. No database. Fully stateless — each request is independent.

```
┌──────────────────────────────────────────────────────────────┐
│                     You take a photo                          │
└──────────────────────────┬───────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │   Step 1: Vision        │
              │   Gemini reads labels   │
              │   → list of wine names  │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   Step 2: Search        │
              │   Tavily fetches live   │
              │   price + critic score  │
              │   for each bottle       │
              └────────────┬────────────┘
                           │
              ┌────────────▼────────────┐
              │   Step 3: Recommend     │
              │   LLM picks best match  │
              │   and explains why      │
              └────────────┬────────────┘
                           │
              Best bottle + comparison table
```

---

## Architecture

```
Frontend (Next.js PWA)
    │  multipart/form-data (image + preference)
    ▼
FastAPI  POST /api/analyze
    │
    ├── src/vision.py
    │       Gemini Vision (via OpenRouter)
    │       → list of wine names from image
    │
    ├── src/search.py
    │       Tavily parallel web search
    │       → price + critic score per wine
    │
    └── src/recommend.py
            LLM (via OpenRouter)
            → best pick + explanation + comparison table
```

The backend is **stateless** — no database, no caching. Every request runs the full three-step pipeline from scratch.

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Backend | Python · FastAPI |
| Vision | Gemini 2.5 Flash (via OpenRouter) |
| Web search | Tavily API |
| LLM | OpenRouter |
| Frontend | Next.js · TypeScript · Tailwind |
| PWA | Web App Manifest + camera capture |
| Backend hosting | Railway |
| Frontend hosting | Vercel |

---

## Local setup

### Prerequisites

- Python 3.11+
- Node 20+
- [OpenRouter](https://openrouter.ai) API key
- [Tavily](https://tavily.com) API key (free tier: 1,000 searches/month)

### 1. Clone

```bash
git clone https://github.com/lmenta/winesnap
cd winesnap
```

### 2. Backend

```bash
python -m venv .venv && source .venv/bin/activate
pip install -e .

cp .env.example .env
# Fill in your keys (see below)

uvicorn src.api:app --reload --port 8000
```

`.env` contents:

```env
OPENROUTER_API_KEY=sk-or-...
TAVILY_API_KEY=tvly-...
```

### 3. Frontend

```bash
cd frontend
npm install
echo 'NEXT_PUBLIC_API_URL=http://localhost:8000' > .env.local
npm run dev
```

Open [http://localhost:3000](http://localhost:3000). The frontend has a mock mode for testing without the backend — toggle with `NEXT_PUBLIC_USE_MOCK=true`.

### 4. Test the pipeline directly

```bash
python test_pipeline.py path/to/wine-photo.jpg
```

---

## API

### `POST /api/analyze`

Accepts a wine shelf photo and an optional preference string.

**Request** (multipart/form-data):

| Field | Type | Description |
|-------|------|-------------|
| `image` | file | Photo of wine bottles (max 20MB) |
| `preference` | string | Optional — defaults to `"best price/quality ratio"` |

**Response:**

```json
{
  "winner": {
    "name": "Château Margaux 2018",
    "reason": "Highest critic score at a mid-range price point",
    "price": "£45",
    "score": 96
  },
  "wines": [
    { "name": "...", "price": "...", "score": 94 },
    ...
  ]
}
```

**Example with curl:**

```bash
curl -X POST http://localhost:8000/api/analyze \
  -F "image=@/path/to/shelf.jpg" \
  -F "preference=under £30, prefer French wines"
```

---

## Deployment

### Backend → Railway

1. Connect this repo to [Railway](https://railway.app)
2. Set environment variables: `OPENROUTER_API_KEY`, `TAVILY_API_KEY`
3. Railway uses `railway.toml` and the root `Dockerfile` automatically

### Frontend → Vercel

1. Connect the `frontend/` directory to [Vercel](https://vercel.com)
2. Set `NEXT_PUBLIC_API_URL` to your Railway backend URL

---

## PWA install

On mobile (iOS/Android), open the live site in Safari/Chrome and use **Add to Home Screen**. The app opens full-screen with direct camera access — no app store needed.
