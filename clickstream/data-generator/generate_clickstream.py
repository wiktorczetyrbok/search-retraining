import json
import random
import re
import requests
from datetime import datetime, timedelta
from tqdm import tqdm

with open("products.json", "r", encoding="utf-8") as f:
    products = json.load(f)


def extract_keywords_from_products(products):
    words = set()
    for product in products:
        text = product.get("title", "") + " " + product.get("description", "")
        tokens = re.findall(r'\b[a-zA-Z]{4,}\b', text)
        words.update(token.lower() for token in tokens)
    return list(words)


def extract_categories(products):
    categories = set()
    for product in products:
        for cat in product.get("categories", []):
            categories.add(cat)
    return list(categories)


# Generate random timestamp
def random_timestamp():
    now = datetime.utcnow()
    past = now - timedelta(days=30)
    return (past + (now - past) * random.random()).isoformat() + "Z"


# Generate query from keywords
def generate_query(keywords):
    return " ".join(random.sample(keywords, k=random.randint(3, 5)))


# Simulate single user search and click event
def simulate_event(query, categories):
    payload = {
        "textQuery": query,
        "size": 10
    }
    try:
        response = requests.post(
            "http://localhost:8080/search",
            json=payload,
            headers={"Content-Type": "application/json"}
        )
        data = response.json()
        hits = data.get("products") or data.get("productHits") or []
        if hits:
            shown_product_ids = [p.get("id") for p in hits if p.get("id")]
            clicked_index = random.randint(0, len(shown_product_ids) - 1)
            clicked_product_id = shown_product_ids[clicked_index]
            event_type = "click"
            if random.random() < 0.10: event_type = "purchase"

            return {
                "query": query,
                "category": random.choice(categories),
                "shownProductIds": shown_product_ids,
                "productId": clicked_product_id,
                "position": clicked_index + 1,
                "timestamp": random_timestamp(),
                "eventType": event_type
            }
    except Exception as e:
        print(f"Query failed for '{query}': {e}")
    return None


keywords = extract_keywords_from_products(products)
categories = extract_categories(products)
clickstream = []

for _ in tqdm(range(4500), desc="Generating enriched clickstream"):
    query = generate_query(keywords)
    event = simulate_event(query, categories)
    if event:
        clickstream.append(event)

with open("clickstream.json", "w", encoding="utf-8") as f:
    json.dump(clickstream, f, indent=2)

print("Clickstream generated successfully")
