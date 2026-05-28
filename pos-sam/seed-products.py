import boto3
import uuid
import json
from decimal import Decimal

TABLE  = "pos-sam-ProductosTable-1KAR20ZY6LGTK"
REGION = "us-east-1"

# code (6 digits), name, price (integer), stock, low_threshold
products = [
    ("100001", "Coca-Cola 600ml",          1500, 120, 10),
    ("100002", "Pepsi 600ml",              1500, 100, 10),
    ("100003", "Agua Cristal 500ml",        800, 200, 20),
    ("100004", "Jugo Hit Naranja 300ml",   1200,  80, 10),
    ("100005", "Gatorade Azul 500ml",      2000,  60,  8),
    ("100006", "Red Bull 250ml",           3500,  40,  5),
    ("100007", "Leche Entera 1L",          1800,  90, 15),
    ("100008", "Leche Descremada 1L",      1900,  70, 10),
    ("100009", "Yogur Natural 200g",       1100,  50,  8),
    ("100010", "Queso Mozzarella 250g",    3200,  35,  5),
    ("100011", "Pan Tajado Bimbo",         2500,  45,  8),
    ("100012", "Pan Integral 500g",        2800,  30,  5),
    ("100013", "Galletas Oreo 154g",       1900,  60, 10),
    ("100014", "Galletas Saltinas 200g",   1500,  55, 10),
    ("100015", "Cereal Zucaritas 500g",    4500,  25,  5),
    ("100016", "Avena Quaker 500g",        3000,  40,  8),
    ("100017", "Arroz Diana 1kg",          2200, 100, 20),
    ("100018", "Frijoles Negros 500g",     1800,  80, 15),
    ("100019", "Lentejas 500g",            1600,  60, 10),
    ("100020", "Pasta Espagueti 500g",     1400,  90, 15),
    ("100021", "Salsa de Tomate 400g",     1700,  70, 10),
    ("100022", "Mayonesa Fruco 400g",      2300,  50,  8),
    ("100023", "Aceite Girasol 1L",        3500,  45,  8),
    ("100024", "Azucar Blanca 1kg",        1900,  80, 15),
    ("100025", "Sal Refisal 500g",          900, 100, 20),
    ("100026", "Cafe Colcafe 200g",        5500,  30,  5),
    ("100027", "Te Lipton 25 sobres",      2800,  40,  8),
    ("100028", "Chocolate Jet 150g",       2200,  55, 10),
    ("100029", "Mantequilla Rama 250g",    2900,  35,  5),
    ("100030", "Huevos x12",               3800,  60, 10),
    ("100031", "Jabon Dove 90g",           1800,  70, 10),
    ("100032", "Shampoo Head Shoulders",   5900,  25,  5),
    ("100033", "Papel Higienico x4",       3200,  80, 15),
    ("100034", "Detergente Ariel 500g",    4500,  40,  8),
    ("100035", "Suavizante Downy 500ml",   3800,  30,  5),
    ("100036", "Desodorante Axe 150ml",    4200,  35,  5),
    ("100037", "Crema Dental Colgate",     2500,  60, 10),
    ("100038", "Cepillo Dental Oral-B",    3000,  45,  8),
    ("100039", "Panales Huggies x20",     12000,  20,  5),
    ("100040", "Toallas Nosotras x10",     3500,  30,  5),
    ("100041", "Manzana Roja kg",          2000,  50, 10),
    ("100042", "Banano kg",                 800,  80, 15),
    ("100043", "Tomate kg",                1500,  60, 10),
    ("100044", "Cebolla kg",               1200,  70, 10),
    ("100045", "Papa kg",                  1000, 100, 20),
    ("100046", "Zanahoria kg",             1100,  60, 10),
    ("100047", "Pollo Entero kg",          4500,  30,  5),
    ("100048", "Carne Molida 500g",        5500,  25,  5),
    ("100049", "Atun Van Camps 170g",      2100,  80, 15),
    ("100050", "Sardinas Deli 125g",       1800,  70, 10),
]

dynamodb = boto3.resource("dynamodb", region_name=REGION)
table    = dynamodb.Table(TABLE)

# Delete all existing items
print("Limpiando tabla...")
resp  = table.scan()
items = resp["Items"]
while "LastEvaluatedKey" in resp:
    resp   = table.scan(ExclusiveStartKey=resp["LastEvaluatedKey"])
    items += resp["Items"]
for item in items:
    table.delete_item(Key={"id": item["id"]})
print(f"Eliminados {len(items)} items anteriores.")

# Insert with structure: { id, code, producto: "<JSON string>" }
# producto is stored as a plain JSON string so the AWS console shows it as readable JSON.
for i, (code, name, price, stock, low) in enumerate(products, 1):
    producto_json = json.dumps({
        "name":                name,
        "price":               price,
        "stock_level":         stock,
        "low_stock_threshold": low,
    })
    table.put_item(Item={
        "id":       str(uuid.uuid4()),
        "code":     code,
        "producto": producto_json,
    })
    print(f"[{i}/50] {code} - {name} - ${price}")

print(f"\nDone! 50 productos insertados en {TABLE}")
