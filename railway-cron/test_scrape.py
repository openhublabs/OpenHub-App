import os
import json
from firecrawl import Firecrawl

from config import EVENTBRITE_URLS, MEETUP_URLS, EVENT_JSON_SCHEMA
from scrapers import normalize_event

api_key = os.environ.get("FIRECRAWL_API_KEY")
if not api_key:
    print("Set FIRECRAWL_API_KEY first")
    exit(1)

firecrawl = Firecrawl(api_key=api_key)

ALL_URLS = EVENTBRITE_URLS + MEETUP_URLS
source_map = {url: ("eventbrite" if "eventbrite" in url else "meetup") for url, _ in ALL_URLS}

total_scraped = 0
total_accepted = 0
total_filtered = 0
filter_reasons = {"no_category": 0, "no_location": 0, "no_date": 0}

for url, ciudad in ALL_URLS:
    source = source_map[url]
    print(f"\n{'='*60}")
    print(f"[{source}] {ciudad}")
    print(f"URL: {url}")
    print(f"{'='*60}")

    try:
        result = firecrawl.scrape(
            url,
            formats=[{"type": "json", "schema": EVENT_JSON_SCHEMA}],
        )
        data = result.json
        if not data or "events" not in data:
            print("No events found")
            continue

        events = data["events"]
        total_scraped += len(events)
        accepted = []
        filtered = 0

        for event in events:
            normalized = normalize_event(event, source, ciudad=ciudad)
            if normalized:
                accepted.append(normalized)
            else:
                filtered += 1
                title = event.get("title", "N/A")
                has_cat = bool(event.get("tags"))
                has_loc = bool(event.get("location", "").strip())
                has_date = bool(event.get("date", "").strip())

                reasons = []
                if not has_cat:
                    reasons.append("no_category")
                if not has_loc:
                    reasons.append("no_location")
                if not has_date:
                    reasons.append("no_date")

                for r in reasons:
                    filter_reasons[r] += 1

                print(f"  FILTERED: {title} -> {', '.join(reasons)}")

        total_accepted += len(accepted)
        total_filtered += filtered

        print(f"\n  Scraped: {len(events)} | Accepted: {len(accepted)} | Filtered: {filtered}")

        for i, ev in enumerate(accepted[:2], 1):
            print(f"  {i}. {ev['titulo']}")
            print(f"     Ciudad: {ev['ciudad']} | Ubicacion: {ev['ubicacion']}")
            print(f"     Fecha: {ev['fecha']} | Categoria: {ev['categoria']}")

        if len(accepted) > 2:
            print(f"  ... and {len(accepted) - 2} more")

    except Exception as e:
        print(f"Error: {e}")

print(f"\n{'='*60}")
print("SUMMARY")
print(f"{'='*60}")
print(f"Total scraped:    {total_scraped}")
print(f"Total accepted:   {total_accepted}")
print(f"Total filtered:   {total_filtered}")
print(f"Filter reasons:")
for reason, count in filter_reasons.items():
    print(f"  {reason}: {count}")
print(f"{'='*60}")
