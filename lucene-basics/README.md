To deploy to cloud run use gcloud.sh script first and then deploy.sh

id, name, uri:
Stored using StringField. Used for identification and display purposes; not tokenized.

title, description:
Stored and indexed using TextField with tokenization. These fields are full-text searchable with the analyzer.

availableTime:
indexed as a LongPoint for filtering/sorting and stored separately using StoredField for retrieval.

brand:
indexed as StringField, allowing exact match filtering.

category:
indexed as TextField, allowing partial and tokenized search on product categories.

attributes.*:
flattened and indexed using StringField (attributes.color, attributes.waffleshape)m for queries like color:Silver

price:
Indexed using DoublePoint to enable numeric range queries (price: 50 to 150), stored using StoredField so it can be shown in results.

currencyCode:
Stored using StringField for display or filtering.

image.uri:
Stored using StoredField, used for display in search results.


Queries tested with swagger url ```{Service URL}/q/swagger-ui``` :
### exact title scores the highest, also considers simmilar products
{
"textQuery": "Cuisinart Mini Waffle Maker",
"filters": null,
"size": 10
}

### edge case - no vaccums in products - 0 results
{
"textQuery": "Vaccum",
"filters": null,
"size": 10
}

### edge case - only 1 blender in products - 1 result

{
"textQuery": "blender",
"filters": null,
"size": 10
}

### brand filter
{
"textQuery": "Waffle",
"filters": {
"brand": "Cuisinart"
},
"size": 10
}


### price filter
{
"textQuery": "Waffle",
"filters": {
"priceFrom": "50",
"priceTo": "100"
},
"size": 10
}

### attribute filter
{
"textQuery": "Waffle",
"filters": {
"waffleshape": "Round"
},
"size": 10
}

### multiword query parser - from description
{
"textQuery": "The Krups Waffle Maker features adjustable browning settings",
"filters": {
"wattage": "1000 Watts"
},
"size": 5
}




