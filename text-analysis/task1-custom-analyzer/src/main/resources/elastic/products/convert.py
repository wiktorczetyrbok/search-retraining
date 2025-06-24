import csv
import json

input_csv = 'blogs.csv'
output_ndjson = 'blogs.ndjson'

with open(input_csv, mode='r', encoding='utf-8') as infile, open(output_ndjson, mode='w', encoding='utf-8') as outfile:
    reader = csv.DictReader(infile)
    for row in reader:
        doc_id = row['id']
        row['text'] = row['text'].strip()
        action_metadata = {
            "index": {
                "_id": doc_id
            }
        }
        outfile.write(json.dumps(action_metadata) + "\n")
        outfile.write(json.dumps(row) + "\n")

print(f"NDJSON bulk file saved to {output_ndjson}")
