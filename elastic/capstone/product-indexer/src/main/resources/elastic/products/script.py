import json

with open("data.json") as f:
    data = json.load(f)

with open("bulk-products.json", "w") as out:
    for doc in data:
        out.write(json.dumps({ "index": { "_id": doc["id"] } }) + "\n")
        out.write(json.dumps(doc) + "\n")
