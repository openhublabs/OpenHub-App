import hashlib
import re
from datetime import datetime

import requests
from firecrawl import Firecrawl

from config import (
    EVENTBRITE_URLS,
    EVENT_JSON_SCHEMA,
    FIRECRAWL_API_KEY,
    MEETUP_URLS,
    PERUANOS_API_URL,
    TAG_TO_CATEGORY,
)


def log(msg: str):
    print(f"[{datetime.now().strftime('%Y-%m-%d %H:%M:%S')}] {msg}")


def generate_event_id(title: str, date: str) -> str:
    raw = f"{title}|{date}".lower().strip()
    return hashlib.md5(raw.encode()).hexdigest()[:16]


def infer_category(tags: list[str]) -> str:
    for tag in tags:
        key = tag.lower().strip()
        if key in TAG_TO_CATEGORY:
            return TAG_TO_CATEGORY[key]
    return ""


TITLE_CATEGORY_KEYWORDS: dict[str, str] = {
    "hackathon": "hackathon",
    "hackaton": "hackathon",
    "conference": "conferencia",
    "conferencia": "conferencia",
    "summit": "conferencia",
    "symposium": "conferencia",
    "workshop": "taller",
    "taller": "taller",
    "hands-on": "taller",
    "tutorial": "taller",
    "meetup": "meetup",
    "webinar": "webinar",
    "networking": "networking",
    "artificial intelligence": "inteligencia artificial",
    "machine learning": "inteligencia artificial",
    "deep learning": "inteligencia artificial",
    "llm": "inteligencia artificial",
    "generative ai": "inteligencia artificial",
    "genai": "inteligencia artificial",
    "gpt": "inteligencia artificial",
    "kubernetes": "devops",
    "k8s": "devops",
    "devops": "devops",
    "docker": "devops",
    "terraform": "devops",
    "cloud": "devops",
    "ci/cd": "devops",
    "ciberseguridad": "seguridad",
    "cybersecurity": "seguridad",
    "security": "seguridad",
    "hacking": "seguridad",
    "ctf": "seguridad",
    "pentest": "seguridad",
    "bug bounty": "seguridad",
    "python": "backend",
    "node.js": "backend",
    "nodejs": "backend",
    "rust": "backend",
    "golang": "backend",
    "java": "backend",
    "backend": "backend",
    "graphql": "backend",
    "javascript": "frontend",
    "typescript": "frontend",
    "react": "frontend",
    "angular": "frontend",
    "vue": "frontend",
    "frontend": "frontend",
    "android": "mobile",
    "ios": "mobile",
    "flutter": "mobile",
    "react native": "mobile",
    "kotlin": "mobile",
    "swift": "mobile",
    "startup": "startup",
    "emprendimiento": "startup",
    "entrepreneur": "startup",
    "pitch": "startup",
    "founders": "startup",
    "venture capital": "inversion",
    "investors": "inversion",
    "funding": "inversion",
    "design": "design",
    "ux": "design",
    "ui": "design",
    "figma": "design",
    "marketing": "marketing",
    "growth": "marketing",
    "seo": "marketing",
}


def infer_category_from_title(title: str) -> str:
    title_lower = title.lower()
    words = set(title_lower.split())
    for keyword, category in TITLE_CATEGORY_KEYWORDS.items():
        if " " in keyword:
            if keyword in title_lower:
                return category
        else:
            if keyword in words:
                return category
    if re.search(r'\bai\b', title_lower):
        return "inteligencia artificial"
    return ""


def infer_tags_from_url(url: str) -> list[str]:
    url_lower = url.lower()
    if "eventbrite.com" in url_lower:
        match = re.search(r'/d/[^/]+/([^/?]+)', url_lower)
        if match:
            return [match.group(1)]
    if "meetup.com" in url_lower:
        match = re.search(r'keywords=([^&]+)', url_lower)
        if match:
            return [match.group(1)]
    return []


def parse_date(date_str: str) -> str:
    if not date_str:
        return ""
    try:
        dt = datetime.strptime(date_str, "%Y-%m-%d")
    except (ValueError, AttributeError):
        try:
            dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
        except (ValueError, AttributeError):
            return date_str
    meses = [
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    ]
    return f"{dt.day} {meses[dt.month - 1]} {dt.year}"


def normalize_event(raw: dict, source: str, url_tags: list[str] | None = None) -> dict | None:
    title = (raw.get("title") or "").strip()
    if not title:
        return None

    date_str = raw.get("date", "")
    event_id = generate_event_id(title, date_str)
    tags = raw.get("tags", [])
    url_tags = url_tags or []
    is_online = raw.get("is_online", False)
    event_type = (raw.get("type") or "").lower()
    if event_type in ("online", "virtual"):
        is_online = True

    category = infer_category(tags) or infer_category_from_title(title) or infer_category(url_tags)
    final_tags = tags if tags else url_tags

    return {
        "id": event_id,
        "source": source,
        "titulo": title,
        "descripcion": raw.get("description", ""),
        "categoria": category,
        "ubicacion": raw.get("location", ""),
        "fecha": parse_date(date_str),
        "horaInicio": raw.get("time", ""),
        "horaFin": "",
        "organizador": raw.get("organizer", ""),
        "imagenUrl": raw.get("image_url", ""),
        "url": raw.get("url", ""),
        "isOnline": is_online,
        "clips": 0,
        "tiempoTexto": "",
        "tags": final_tags,
    }


def scrape_peruanos() -> list[dict]:
    log("[peruanos.dev] Fetching from API...")
    try:
        response = requests.get(PERUANOS_API_URL, timeout=30)
        response.raise_for_status()
        events = response.json()
    except Exception as e:
        log(f"[peruanos.dev] Error: {e}")
        return []

    normalized = []
    today = datetime.now().date()
    for event in events:
        date_str = event.get("date", "")
        try:
            event_date = datetime.strptime(date_str, "%Y-%m-%d").date()
            if event_date < today:
                continue
        except ValueError:
            pass
        raw = {
            "title": event.get("title", ""),
            "description": event.get("description", ""),
            "date": date_str,
            "time": event.get("time", ""),
            "location": event.get("location", ""),
            "organizer": event.get("organizer", ""),
            "image_url": event.get("image_url", ""),
            "url": event.get("registration_url", ""),
            "type": event.get("type", ""),
            "tags": event.get("tags", []),
        }
        normalized_event = normalize_event(raw, "peruanos.dev")
        if normalized_event:
            normalized.append(normalized_event)

    log(f"[peruanos.dev] Got {len(normalized)} events")
    return normalized


def _firecrawl_scrape_urls(urls: list[str], source: str) -> list[dict]:
    if not FIRECRAWL_API_KEY:
        log(f"[{source}] No FIRECRAWL_API_KEY set, skipping")
        return []

    if not urls:
        log(f"[{source}] No URLs configured, skipping")
        return []

    firecrawl = Firecrawl(api_key=FIRECRAWL_API_KEY)

    all_events = []
    for url in urls:
        log(f"  [{source}] Scraping {url}")
        try:
            result = firecrawl.scrape(
                url,
                formats=[{"type": "json", "schema": EVENT_JSON_SCHEMA}],
            )
        except Exception as e:
            log(f"  [{source}] Error: {e}")
            continue

        json_data = result.json if result and hasattr(result, "json") else None
        if not json_data or "events" not in json_data:
            log(f"  [{source}] No events found")
            continue

        source_url = url
        if result and hasattr(result, "metadata") and result.metadata:
            source_url = getattr(result.metadata, "source_url", "") or url

        url_tags = infer_tags_from_url(source_url)
        event_count = len(json_data["events"])

        for event in json_data["events"]:
            normalized = normalize_event(event, source, url_tags=url_tags)
            if normalized:
                all_events.append(normalized)

        log(f"  [{source}] {source_url} -> {event_count} events")

    log(f"[{source}] Done — {len(all_events)} events from {len(urls)} URLs")
    return all_events


def scrape_eventbrite() -> list[dict]:
    return _firecrawl_scrape_urls(EVENTBRITE_URLS, "eventbrite")


def scrape_meetup() -> list[dict]:
    return _firecrawl_scrape_urls(MEETUP_URLS, "meetup")


SCRAPERS = {
    "peruanos.dev": scrape_peruanos,
    "eventbrite": scrape_eventbrite,
    "meetup": scrape_meetup,
}
