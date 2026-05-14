"""Vision pipeline: image → list of wines identified on the shelf."""

from __future__ import annotations
import base64
import json
from openai import OpenAI

OPENROUTER_BASE = "https://openrouter.ai/api/v1"
VISION_MODEL = "google/gemini-2.5-flash"

VISION_PROMPT = """You are an expert wine identifier. Examine this image carefully.

Identify every wine bottle you can see. For each bottle, extract:
- name: the wine name (e.g. "Malbec Reserva", "Rioja Crianza")
- winery: the producer/winery name
- vintage: the year (if visible, else null)
- region: country or region (e.g. "Argentina", "Rioja, Spain")
- visible_price: any price tag visible in the image (number only, null if not visible)

Return ONLY a JSON array. No explanation. Example:
[
  {"name": "Malbec Reserva", "winery": "Zuccardi", "vintage": 2021, "region": "Mendoza, Argentina", "visible_price": 12.99},
  {"name": "Rioja Crianza", "winery": "Muga", "vintage": 2020, "region": "Rioja, Spain", "visible_price": null}
]

If you cannot identify any wines, return an empty array [].
Only include bottles where you can read at least the wine name or winery."""


def _image_to_data_url(image_bytes: bytes) -> str:
    if image_bytes[:4] == b'\x89PNG':
        mime = "image/png"
    elif image_bytes[:3] == b'\xff\xd8\xff':
        mime = "image/jpeg"
    else:
        mime = "image/webp"
    return f"data:{mime};base64,{base64.b64encode(image_bytes).decode()}"


def identify_wines(image_bytes: bytes, client: OpenAI, image_url: str | None = None) -> list[dict]:
    """Send image to vision model, return list of identified wines."""
    img_content = (
        {"type": "image_url", "image_url": {"url": image_url}}
        if image_url
        else {"type": "image_url", "image_url": {"url": _image_to_data_url(image_bytes)}}
    )

    response = client.chat.completions.create(
        model=VISION_MODEL,
        max_tokens=2048,
        messages=[{
            "role": "user",
            "content": [
                {"type": "text", "text": VISION_PROMPT},
                img_content,
            ],
        }],
    )

    raw = (response.choices[0].message.content or "").strip()

    if not raw:
        return []

    # Strip markdown code fences if present
    if "```" in raw:
        parts = raw.split("```")
        for part in parts:
            part = part.strip()
            if part.startswith("json"):
                part = part[4:].strip()
            if part.startswith("["):
                raw = part
                break

    # Find JSON array in response
    start = raw.find("[")
    end = raw.rfind("]")
    if start == -1 or end == -1:
        return []
    raw = raw[start:end+1]

    try:
        wines = json.loads(raw)
    except json.JSONDecodeError:
        return []

    return [w for w in wines if w.get("name") or w.get("winery")]
