import os
import re
import json
import base64
from datetime import datetime

import requests
from firebase_admin import credentials, firestore, initialize_app


EVENT_SOURCE = "peruanos.dev"
API_URL = "https://peruanos.dev/api/events"

FIREBASE_SERVICE_ACCOUNT_B64 = os.environ.get("FIREBASE_SERVICE_ACCOUNT")
if not FIREBASE_SERVICE_ACCOUNT_B64:
    raise ValueError("Missing FIREBASE_SERVICE_ACCOUNT environment variable")

sa_key = json.loads(base64.b64decode(FIREBASE_SERVICE_ACCOUNT_B64).decode("utf-8"))
cred = credentials.Certificate(sa_key)
initialize_app(cred)
db = firestore.client()

CATEGORY_KEYWORDS = {
    "hackathon": ["hackathon", "hackatón", "hack"],
    "conferencia": ["conferencia", "summit", "conference", "keynote"],
    "taller": ["taller", "workshop", "hands-on", "tutorial"],
    "meetup": ["meetup", "encuentro", "networking"],
    "webinar": ["webinar", "webinar", "online", "virtual"],
    "curso": ["curso", "course", "bootcamp", "training"],
}


def infer_category(title: str) -> str:
    title_lower = title.lower()
    for category, keywords in CATEGORY_KEYWORDS.items():
        if any(kw in title_lower for kw in keywords):
            return category
    return "general"


def infer_tags(title: str) -> list[str]:
    tech_keywords = {
        "python": "Python",
        "react": "React",
        "angular": "Angular",
        "vue": "Vue.js",
        "kotlin": "Kotlin",
        "java": "Java",
        "javascript": "JavaScript",
        "typescript": "TypeScript",
        "go": "Go",
        "rust": "Rust",
        "kubernetes": "Kubernetes",
        "docker": "Docker",
        "aws": "AWS",
        "azure": "Azure",
        "gcp": "GCP",
        "ia": "IA",
        "ai": "AI",
        "machine learning": "Machine Learning",
        "blockchain": "Blockchain",
        "devops": "DevOps",
        "ci/cd": "CI/CD",
        "mobile": "Mobile",
        "android": "Android",
        "ios": "iOS",
        "design": "Design",
        "ux": "UX",
        "ui": "UI",
        "seguridad": "Seguridad",
        "cybersecurity": "Cybersecurity",
    }
    title_lower = title.lower()
    found = []
    for keyword, tag in tech_keywords.items():
        if keyword in title_lower:
            found.append(tag)
    return found


def is_online(location: str) -> bool:
    location_lower = location.lower()
    return any(
        kw in location_lower for kw in ["online", "virtual", "webinar", "zoom", "meet"]
    )


def parse_date(date_str: str) -> str:
    try:
        dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
        meses = [
            "ene",
            "feb",
            "mar",
            "abr",
            "may",
            "jun",
            "jul",
            "ago",
            "sep",
            "oct",
            "nov",
            "dic",
        ]
        return f"{dt.day} {meses[dt.month - 1]} {dt.year}"
    except (ValueError, AttributeError):
        return date_str


def main():
    print(f"Fetching events from {API_URL}...")
    try:
        response = requests.get(API_URL, timeout=30)
        response.raise_for_status()
        events = response.json()
    except Exception as e:
        print(f"Error fetching events: {e}")
        return

    print(f"Got {len(events)} events")

    batch = db.batch()
    count = 0

    for event in events:
        title = event.get("title", "").strip()
        if not title:
            continue

        event_id = f"peruanosdev-{abs(hash(title + event.get('date', '')))}"
        category = infer_category(title)
        source_location = event.get("location", "")
        location = source_location if source_location else "Online"

        doc_ref = db.collection("events").document(event_id)
        batch.set(doc_ref, {
            "id": event_id,
            "source": EVENT_SOURCE,
            "titulo": title,
            "descripcion": f"Evento organizado por {event.get('organizer', 'Desconocido')}",
            "categoria": category,
            "ubicacion": location,
            "fecha": parse_date(event.get("date", "")),
            "horaInicio": "",
            "horaFin": "",
            "organizador": event.get("organizer", ""),
            "imagenUrl": "",
            "isOnline": is_online(source_location),
            "clips": 0,
            "tiempoTexto": "",
            "tags": infer_tags(title),
            "updatedAt": firestore.SERVER_TIMESTAMP,
        })
        count += 1

    batch.commit()
    print(f"Upserted {count} events to Firestore")


if __name__ == "__main__":
    main()
