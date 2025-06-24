import csv

# Input and output file paths
input_file = 'blogtext.csv'
output_file = 'task1-custom-analyzer/src/main/resources/elastic/products/first_50_rows.csv'

# Read and write only first 50 rows
with open(input_file, mode='r', encoding='utf-8') as infile:
    reader = csv.reader(infile)
    header = next(reader)

    rows = []
    for i, row in enumerate(reader):
        if i >= 50:
            break
        rows.append(row)

# Write to new CSV
with open(output_file, mode='w', encoding='utf-8', newline='') as outfile:
    writer = csv.writer(outfile)
    writer.writerow(header)
    writer.writerows(rows)

print(f"Saved first 50 rows to {output_file}")
