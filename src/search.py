"""Search pipeline: wine name → price + critic score via Tavily."""

from __future__ import annotations
import asyncio
from tavily import TavilyClient


def _build_query(wine: dict) -> str:
    parts = []
    if wine.get("winery"):
        parts.append(wine["winery"])
    if wine.get("name"):
        parts.append(wine["name"])
    if wine.get("vintage"):
        parts.append(str(wine["vintage"]))
    parts.append("wine price rating review")
    return " ".join(parts)


def _extract_score(text: str) -> int | None:
    """Try to find a 0-100 critic score in text."""
    import re
    # Look for patterns like "92 points", "Score: 88", "91/100"
    for pat in [r'\b(\d{2,3})\s*points?\b', r'[Ss]core[:\s]+(\d{2,3})', r'\b(\d{2,3})/100\b']:
        m = re.search(pat, text)
        if m:
            score = int(m.group(1))
            if 50 <= score <= 100:
                return score
    return None


def _extract_price(text: str) -> float | None:
    """Try to find a price in text."""
    import re
    for pat in [r'[£$€]\s*(\d+(?:\.\d{1,2})?)', r'(\d+(?:\.\d{1,2})?)\s*(?:GBP|USD|EUR)']:
        m = re.search(pat, text)
        if m:
            price = float(m.group(1))
            if 2 <= price <= 2000:
                return price
    return None


def search_wine(wine: dict, client: TavilyClient) -> dict:
    """Search for price and rating for a single wine. Returns enriched wine dict."""
    query = _build_query(wine)
    result = wine.copy()
    result["score"] = None
    result["price"] = wine.get("visible_price")
    result["source_url"] = None
    result["tasting_note"] = None

    try:
        response = client.search(
            query=query,
            search_depth="basic",
            max_results=5,
            include_answer=True,
        )

        # Combine all text for parsing
        all_text = response.get("answer", "") or ""
        for r in response.get("results", []):
            all_text += " " + (r.get("content") or "")
            if not result["source_url"]:
                result["source_url"] = r.get("url")

        if not result["score"]:
            result["score"] = _extract_score(all_text)
        if not result["price"]:
            result["price"] = _extract_price(all_text)

        # Use Tavily's AI answer as tasting note if short enough
        answer = response.get("answer", "")
        if answer and len(answer) < 300:
            result["tasting_note"] = answer

    except Exception as e:
        result["search_error"] = str(e)[:100]

    return result


def search_wines_parallel(wines: list[dict], client: TavilyClient) -> list[dict]:
    """Search all wines in parallel using threads."""
    from concurrent.futures import ThreadPoolExecutor
    with ThreadPoolExecutor(max_workers=5) as pool:
        return list(pool.map(lambda w: search_wine(w, client), wines))
