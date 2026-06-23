import os
import json
from firecrawl import Firecrawl

api_key = os.environ.get("FIRECRAWL_API_KEY")
if not api_key:
    print("Set FIRECRAWL_API_KEY first")
    exit(1)

firecrawl = Firecrawl(api_key=api_key)

url = "https://www.eventbrite.com/d/peru/technology/"

result = firecrawl.scrape(
    url,
    formats=[{
        "type": "json",
        "schema": {
            "type": "object",
            "properties": {
                "events": {
                    "type": "array",
                    "items": {
                        "type": "object",
                        "properties": {
                            "title": {"type": "string"},
                            "description": {"type": "string"},
                            "date": {"type": "string"},
                            "location": {"type": "string"},
                            "url": {"type": "string"},
                        },
                        "required": ["title", "url"],
                    },
                }
            },
            "required": ["events"],
        },
    }],
)

print(json.dumps(result.json, indent=2, ensure_ascii=False))
