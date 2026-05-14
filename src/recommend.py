"""Recommendation engine: enriched wines → best pick with reasoning."""

from __future__ import annotations
import json
from openai import OpenAI

OPENROUTER_BASE = "https://openrouter.ai/api/v1"
RECOMMEND_MODEL = "google/gemini-3.1-flash-lite"

SYSTEM_PROMPT = """You are an expert sommelier helping someone pick the best wine from a selection.
Be concise, practical, and enthusiastic. Write like a knowledgeable friend, not a textbook."""


def _value_score(wine: dict) -> float:
    """score / price — higher is better value."""
    score = wine.get("score") or 0
    price = wine.get("price") or 0
    if score and price:
        return round(score / price, 3)
    if score:
        return score / 20  # no price data, use score alone
    return 0


def pick_winner(wines: list[dict], preference: str, client: OpenAI) -> dict:
    """Use LLM to pick the best wine and explain why."""

    # Calculate value scores
    for w in wines:
        w["value_score"] = _value_score(w)

    # Sort by value score
    wines_with_data = [w for w in wines if w.get("score") or w.get("price")]
    wines_no_data = [w for w in wines if not w.get("score") and not w.get("price")]

    if not wines_with_data and not wines_no_data:
        return {"error": "No wines could be identified in the image."}

    wines_sorted = sorted(wines_with_data, key=lambda w: w["value_score"], reverse=True)

    # Build context for LLM
    wine_list = json.dumps(wines_sorted + wines_no_data, indent=2)

    user_message = f"""The user wants: "{preference}"

Here are the wines found with their data:
{wine_list}

Pick the single best wine for the user's preference. Return JSON:
{{
  "winner_index": 0,
  "reason": "2-3 sentence explanation why this wine wins for the preference",
  "tasting_note": "one sentence describing taste profile",
  "value_tip": "one sentence on why the price/quality is notable (if relevant)"
}}"""

    response = client.chat.completions.create(
        model=RECOMMEND_MODEL,
        max_tokens=512,
        messages=[
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": user_message},
        ],
        response_format={"type": "json_object"},
    )

    rec = json.loads(response.choices[0].message.content)
    winner_index = rec.get("winner_index", 0)

    all_wines = wines_sorted + wines_no_data
    winner = all_wines[winner_index] if winner_index < len(all_wines) else all_wines[0]

    return {
        "winner": {
            **winner,
            "reason": rec.get("reason", ""),
            "tasting_note": rec.get("tasting_note") or winner.get("tasting_note", ""),
            "value_tip": rec.get("value_tip", ""),
        },
        "all_wines": all_wines,
        "wines_identified": len(wines),
        "wines_with_data": len(wines_with_data),
    }
