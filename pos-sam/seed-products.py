import boto3
import uuid
from decimal import Decimal

TABLE = "pos-sam-ProductosTable-1KAR20ZY6LGTK"
REGION = "us-east-1"

products = [
    ("7702001001", "Coca-Cola 600ml",          1.50, 120, 10),
    ("7702001002", "Pepsi 600ml",              1.50, 100, 10),
    ("7702001003", "Agua Cristal 500ml",       0.80, 200, 20),
    ("7702001004", "Jugo Hit Naranja 300ml",   1.20,  80, 10),
    ("7702001005", "Gatorade Azul 500ml",      2.00,  60,  8),
    ("7702001006", "Red Bull 250ml",           3.50,  40,  5),
    ("7702001007", "Leche Entera 1L",          1.80,  90, 15),
    ("7702001008", "Leche Descremada 1L",      1.90,  70, 10),
    ("7702001009", "Yogur Natural 200g",       1.10,  50,  8),
    ("7702001010", "Queso Mozzarella 250g",    3.20,  35,  5),
    ("7702001011", "Pan Tajado Bimbo",         2.50,  45,  8),
    ("7702001012", "Pan Integral 500g",        2.80,  30,  5),
    ("7702001013", "Galletas Oreo 154g",       1.90,  60, 10),
    ("7702001014", "Galletas Saltinas 200g",   1.50,  55, 10),
    ("7702001015", "Cereal Zucaritas 500g",    4.50,  25,  5),
    ("7702001016", "Avena Quaker 500g",        3.00,  40,  8),
    ("7702001017", "Arroz Diana 1kg",          2.20, 100, 20),
    ("7702001018", "Frijoles Negros 500g",     1.80,  80, 15),
    ("7702001019", "Lentejas 500g",            1.60,  60, 10),
    ("7702001020", "Pasta Espagueti 500g",     1.40,  90, 15),
    ("7702001021", "Salsa de Tomate 400g",     1.70,  70, 10),
    ("7702001022", "Mayonesa Fruco 400g",      2.30,  50,  8),
    ("7702001023", "Aceite Girasol 1L",        3.50,  45,  8),
    ("7702001024", "Azucar Blanca 1kg",        1.90,  80, 15),
    ("7702001025", "Sal Refisal 500g",         0.90, 100, 20),
    ("7702001026", "Cafe Colcafe 200g",        5.50,  30,  5),
    ("7702001027", "Te Lipton 25 sobres",      2.80,  40,  8),
    ("7702001028", "Chocolate Jet 150g",       2.20,  55, 10),
    ("7702001029", "Mantequilla Rama 250g",    2.90,  35,  5),
    ("7702001030", "Huevos x12",               3.80,  60, 10),
    ("7702001031", "Jabon Dove 90g",           1.80,  70, 10),
    ("7702001032", "Shampoo Head Shoulders",   5.90,  25,  5),
    ("7702001033", "Papel Higienico x4",       3.20,  80, 15),
    ("7702001034", "Detergente Ariel 500g",    4.50,  40,  8),
    ("7702001035", "Suavizante Downy 500ml",   3.80,  30,  5),
    ("7702001036", "Desodorante Axe 150ml",    4.20,  35,  5),
    ("7702001037", "Crema Dental Colgate",     2.50,  60, 10),
    ("7702001038", "Cepillo Dental Oral-B",    3.00,  45,  8),
    ("7702001039", "Panales Huggies x20",     12.00,  20,  5),
    ("7702001040", "Toallas Nosotras x10",     3.50,  30,  5),
    ("7702001041", "Manzana Roja kg",          2.00,  50, 10),
    ("7702001042", "Banano kg",                0.80,  80, 15),
    ("7702001043", "Tomate kg",                1.50,  60, 10),
    ("7702001044", "Cebolla kg",               1.20,  70, 10),
    ("7702001045", "Papa kg",                  1.00, 100, 20),
    ("7702001046", "Zanahoria kg",             1.10,  60, 10),
    ("7702001047", "Pollo Entero kg",          4.50,  30,  5),
    ("7702001048", "Carne Molida 500g",        5.50,  25,  5),
    ("7702001049", "Atun Van Camps 170g",      2.10,  80, 15),
    ("7702001050", "Sardinas Deli 125g",       1.80,  70, 10),
]

dynamodb = boto3.resource("dynamodb", region_name=REGION)
table = dynamodb.Table(TABLE)

for i, (code, name, price, stock, low) in enumerate(products, 1):
    table.put_item(Item={
        "id": str(uuid.uuid4()),
        "code": code,
        "name": name,
        "price": Decimal(str(price)),
        "stock_level": stock,
        "low_stock_threshold": low,
    })
    print(f"[{i}/50] {name}")

print(f"\nDone! 50 productos insertados en {TABLE}")
