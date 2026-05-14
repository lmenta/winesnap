"""Quick CLI test of the full pipeline."""
from __future__ import annotations
import sys
from dotenv import load_dotenv
load_dotenv()

from openai import OpenAI
from tavily import TavilyClient
from rich.console import Console
from rich.panel import Panel
from rich import print as rprint

from src.config import settings
from src.vision import identify_wines, OPENROUTER_BASE
from src.search import search_wines_parallel
from src.recommend import pick_winner

console = Console()

# Use a real wine shelf image URL for testing
# A real wine shop shelf photo from Wikipedia Commons
# Public wine shelf photo (Pexels)
TEST_IMAGE_URL = "https://images.pexels.com/photos/1407846/pexels-photo-1407846.jpeg?auto=compress&cs=tinysrgb&w=1260&h=750&dpr=2"

import httpx

console.rule("[bold blue]WineSnap Pipeline Test")

# Download test image
console.print(f"Using image: {TEST_IMAGE_URL[:60]}...")
img_bytes = b""  # not needed when passing URL directly

vision_client = OpenAI(api_key=settings.openrouter_api_key, base_url=OPENROUTER_BASE)
tavily = TavilyClient(api_key=settings.tavily_api_key)

console.print("\n[bold]Step 1: Identifying wines...[/bold]")
wines = identify_wines(img_bytes, vision_client, image_url=TEST_IMAGE_URL)
console.print(f"Found {len(wines)} wine(s):")
for w in wines:
    console.print(f"  - {w.get('winery','')} {w.get('name','')} {w.get('vintage','') or ''}")

if wines:
    console.print("\n[bold]Step 2: Searching for price + scores...[/bold]")
    wines = search_wines_parallel(wines, tavily)
    for w in wines:
        console.print(f"  - {w.get('name','')} | price={w.get('price')} | score={w.get('score')}")

    console.print("\n[bold]Step 3: Picking winner...[/bold]")
    from openai import OpenAI as OAI
    rec_client = OAI(api_key=settings.openrouter_api_key, base_url=OPENROUTER_BASE)
    result = pick_winner(wines, "best price/quality ratio", rec_client)

    winner = result.get("winner", {})
    console.print(Panel(
        f"[bold green]{winner.get('winery','')} {winner.get('name','')} {winner.get('vintage','') or ''}[/bold green]\n\n"
        f"Price: £{winner.get('price','?')}  |  Score: {winner.get('score','?')}  |  Value: {winner.get('value_score','?')}\n\n"
        f"{winner.get('reason','')}\n\n"
        f"[italic]{winner.get('tasting_note','')}[/italic]",
        title="🍷 Winner",
        border_style="green"
    ))
else:
    console.print("[yellow]No wines identified — try a different image[/yellow]")
