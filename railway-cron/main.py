import os
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


def parse_date(date_str: str) -> str:
    if not date_str:
        return ""
    try:
        dt = datetime.fromisoformat(date_str.replace("Z", "+00:00"))
    except (ValueError, AttributeError):
        try:
            dt = datetime.strptime(date_str, "%Y-%m-%d")
        except (ValueError, AttributeError):
            return date_str
    meses = [
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    ]
    return f"{dt.day} {meses[dt.month - 1]} {dt.year}"


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
        event_type = event.get("type", "").lower()

        doc_ref = db.collection("events").document(event_id)
        batch.set(doc_ref, {
            "id": event_id,
            "source": EVENT_SOURCE,
            "titulo": title,
            "descripcion": event.get("description", ""),
            "categoria": event.get("category", ""),
            "ubicacion": event.get("location", ""),
            "fecha": parse_date(event.get("date", "")),
            "horaInicio": event.get("time", ""),
            "horaFin": "",
            "organizador": event.get("organizer", ""),
            "imagenUrl": event.get("image_url", ""),
            "url": event.get("registration_url", ""),
            "isOnline": event_type in ("online", "virtual"),
            "clips": 0,
            "tiempoTexto": "",
            "tags": event.get("tags", []),
            "updatedAt": firestore.SERVER_TIMESTAMP,
        })
        count += 1

    batch.commit()
    print(f"Upserted {count} events to Firestore")


if __name__ == "__main__":
    main()
