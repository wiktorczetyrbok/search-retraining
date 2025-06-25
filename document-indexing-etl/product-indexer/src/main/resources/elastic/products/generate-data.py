from faker import Faker
import random
import json
from datetime import datetime

fake = Faker()
Faker.seed(0)
random.seed(0)

def generate_product(product_id):
    product_id_str = str(product_id)
    name = fake.catch_phrase()
    description = fake.text(max_nb_chars=100)
    category = " > ".join(fake.words(nb=3))
    brand = fake.company()
    price = round(random.uniform(10, 300), 2)
    currency = "USD"
    available = True
    stock = random.randint(0, 100)
    colors = [fake.color_name() for _ in range(random.randint(1, 2))]
    sizes = random.sample(["XS", "S", "M", "L", "XL", "XXL"], k=random.randint(1, 3))
    last_updated = datetime.utcnow().isoformat() + "Z"

    # Combine colors and sizes into skus
    skus = [{"color": color, "size": size} for color in colors for size in sizes]

    product_doc = {
        "id": product_id_str,
        "name": name,
        "description": description,
        "category": category,
        "brand": brand,
        "price": price,
        "currency": currency,
        "available": available,
        "stock": stock,
        "last_updated": last_updated,
        "skus": skus
    }

    return {"index": {"_id": product_id_str}}, product_doc

# Generate 100 products
catalog_path = "product_catalog.ndjson"
with open(catalog_path, "w") as f:
    for i in range(100):
        meta, doc = generate_product(i)
        f.write(json.dumps(meta) + "\n")
        f.write(json.dumps(doc) + "\n")
