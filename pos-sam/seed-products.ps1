$AWS = "C:\Program Files\Amazon\AWSCLIV2\aws.exe"
$TABLE = "pos-sam-ProductosTable-1KAR20ZY6LGTK"
$REGION = "us-east-1"

$products = @(
  @{ code="7702001001"; name="Coca-Cola 600ml";          price="1.50"; stock="120"; low="10" },
  @{ code="7702001002"; name="Pepsi 600ml";              price="1.50"; stock="100"; low="10" },
  @{ code="7702001003"; name="Agua Cristal 500ml";       price="0.80"; stock="200"; low="20" },
  @{ code="7702001004"; name="Jugo Hit Naranja 300ml";   price="1.20"; stock="80";  low="10" },
  @{ code="7702001005"; name="Gatorade Azul 500ml";      price="2.00"; stock="60";  low="8"  },
  @{ code="7702001006"; name="Red Bull 250ml";           price="3.50"; stock="40";  low="5"  },
  @{ code="7702001007"; name="Leche Entera 1L";          price="1.80"; stock="90";  low="15" },
  @{ code="7702001008"; name="Leche Descremada 1L";      price="1.90"; stock="70";  low="10" },
  @{ code="7702001009"; name="Yogur Natural 200g";       price="1.10"; stock="50";  low="8"  },
  @{ code="7702001010"; name="Queso Mozzarella 250g";    price="3.20"; stock="35";  low="5"  },
  @{ code="7702001011"; name="Pan Tajado Bimbo";         price="2.50"; stock="45";  low="8"  },
  @{ code="7702001012"; name="Pan Integral 500g";        price="2.80"; stock="30";  low="5"  },
  @{ code="7702001013"; name="Galletas Oreo 154g";       price="1.90"; stock="60";  low="10" },
  @{ code="7702001014"; name="Galletas Saltinas 200g";   price="1.50"; stock="55";  low="10" },
  @{ code="7702001015"; name="Cereal Zucaritas 500g";    price="4.50"; stock="25";  low="5"  },
  @{ code="7702001016"; name="Avena Quaker 500g";        price="3.00"; stock="40";  low="8"  },
  @{ code="7702001017"; name="Arroz Diana 1kg";          price="2.20"; stock="100"; low="20" },
  @{ code="7702001018"; name="Frijoles Negros 500g";     price="1.80"; stock="80";  low="15" },
  @{ code="7702001019"; name="Lentejas 500g";            price="1.60"; stock="60";  low="10" },
  @{ code="7702001020"; name="Pasta Espagueti 500g";     price="1.40"; stock="90";  low="15" },
  @{ code="7702001021"; name="Salsa de Tomate 400g";     price="1.70"; stock="70";  low="10" },
  @{ code="7702001022"; name="Mayonesa Fruco 400g";      price="2.30"; stock="50";  low="8"  },
  @{ code="7702001023"; name="Aceite Girasol 1L";        price="3.50"; stock="45";  low="8"  },
  @{ code="7702001024"; name="Azucar Blanca 1kg";        price="1.90"; stock="80";  low="15" },
  @{ code="7702001025"; name="Sal Refisal 500g";         price="0.90"; stock="100"; low="20" },
  @{ code="7702001026"; name="Cafe Colcafe 200g";        price="5.50"; stock="30";  low="5"  },
  @{ code="7702001027"; name="Te Lipton 25 sobres";      price="2.80"; stock="40";  low="8"  },
  @{ code="7702001028"; name="Chocolate Jet 150g";       price="2.20"; stock="55";  low="10" },
  @{ code="7702001029"; name="Mantequilla Rama 250g";    price="2.90"; stock="35";  low="5"  },
  @{ code="7702001030"; name="Huevos x12";               price="3.80"; stock="60";  low="10" },
  @{ code="7702001031"; name="Jabon Dove 90g";           price="1.80"; stock="70";  low="10" },
  @{ code="7702001032"; name="Shampoo Head Shoulders";   price="5.90"; stock="25";  low="5"  },
  @{ code="7702001033"; name="Papel Higienico x4";       price="3.20"; stock="80";  low="15" },
  @{ code="7702001034"; name="Detergente Ariel 500g";    price="4.50"; stock="40";  low="8"  },
  @{ code="7702001035"; name="Suavizante Downy 500ml";   price="3.80"; stock="30";  low="5"  },
  @{ code="7702001036"; name="Desodorante Axe 150ml";    price="4.20"; stock="35";  low="5"  },
  @{ code="7702001037"; name="Crema Dental Colgate";     price="2.50"; stock="60";  low="10" },
  @{ code="7702001038"; name="Cepillo Dental Oral-B";    price="3.00"; stock="45";  low="8"  },
  @{ code="7702001039"; name="Pañales Huggies x20";      price="12.00"; stock="20"; low="5"  },
  @{ code="7702001040"; name="Toallas Nosotras x10";     price="3.50"; stock="30";  low="5"  },
  @{ code="7702001041"; name="Manzana Roja kg";          price="2.00"; stock="50";  low="10" },
  @{ code="7702001042"; name="Banano kg";                price="0.80"; stock="80";  low="15" },
  @{ code="7702001043"; name="Tomate kg";                price="1.50"; stock="60";  low="10" },
  @{ code="7702001044"; name="Cebolla kg";               price="1.20"; stock="70";  low="10" },
  @{ code="7702001045"; name="Papa kg";                  price="1.00"; stock="100"; low="20" },
  @{ code="7702001046"; name="Zanahoria kg";             price="1.10"; stock="60";  low="10" },
  @{ code="7702001047"; name="Pollo Entero kg";          price="4.50"; stock="30";  low="5"  },
  @{ code="7702001048"; name="Carne Molida 500g";        price="5.50"; stock="25";  low="5"  },
  @{ code="7702001049"; name="Atun Van Camps 170g";      price="2.10"; stock="80";  low="15" },
  @{ code="7702001050"; name="Sardinas Deli 125g";       price="1.80"; stock="70";  low="10" }
)

$count = 0
foreach ($p in $products) {
    $id = [System.Guid]::NewGuid().ToString()
    $item = @{
        id              = @{ S = $id }
        code            = @{ S = $p.code }
        name            = @{ S = $p.name }
        price           = @{ N = $p.price }
        stock_level     = @{ N = $p.stock }
        low_stock_threshold = @{ N = $p.low }
    } | ConvertTo-Json -Compress

    & $AWS dynamodb put-item --table-name $TABLE --region $REGION --item $item | Out-Null
    $count++
    Write-Host "[$count/50] $($p.name)"
}

Write-Host "`nDone! $count productos insertados en $TABLE"
