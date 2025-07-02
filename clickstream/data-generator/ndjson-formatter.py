import json

with open('products.json', 'r', encoding='utf-8') as f:
    products = json.load(f)

with open('products.ndjson', 'w', encoding='utf-8') as f:
    for i, product in enumerate(products):
        product['id'] = i + 1

        index_line = {"index": {"_id": i + 1}}
        f.write(json.dumps(index_line) + '\n')
        f.write(json.dumps(product) + '\n')

print(f"ndjson file for {len(products)}")
