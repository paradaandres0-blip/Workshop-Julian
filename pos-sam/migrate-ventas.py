"""
migrate-ventas.py
Migra los registros de VentasTable del formato Map nativo de DynamoDB
al formato JSON string plano, para que la consola de AWS muestre:
  detalle → {"createdAt": "...", "total": 2.3, "status": "OPEN", "items": [...]}
en lugar de:
  detalle → {"createdAt": {"S": "..."}, "total": {"N": "2.3"}, ...}
"""

import boto3
import json

VENTAS_TABLE = "pos-sam-VentasTable-1ETRDZ6IFUUBT"
REGION       = "us-east-1"

dynamodb = boto3.client("dynamodb", region_name=REGION)
resource = boto3.resource("dynamodb", region_name=REGION)
table    = resource.Table(VENTAS_TABLE)


def av_to_python(av):
    """Converts a DynamoDB typed AttributeValue dict to a plain Python value."""
    if "S" in av:
        # If it's already a JSON string, parse it
        val = av["S"]
        try:
            return json.loads(val)
        except Exception:
            return val
    if "N" in av:
        n = av["N"]
        return int(n) if "." not in n else float(n)
    if "BOOL" in av:
        return av["BOOL"]
    if "NULL" in av:
        return None
    if "M" in av:
        return {k: av_to_python(v) for k, v in av["M"].items()}
    if "L" in av:
        return [av_to_python(i) for i in av["L"]]
    return str(av)


def needs_migration(detalle_av):
    """Returns True if detalle is stored as a native DynamoDB Map (needs migration)."""
    if "M" in detalle_av:
        return True
    if "S" in detalle_av:
        # Already a string — check if it's the typed format accidentally stored as string
        try:
            parsed = json.loads(detalle_av["S"])
            # If any value is itself a dict with "S"/"N"/"M"/"L" keys, it's typed format
            for v in parsed.values():
                if isinstance(v, dict) and any(k in v for k in ("S", "N", "M", "L", "BOOL", "NULL")):
                    return True
        except Exception:
            pass
    return False


# Scan all items using low-level client to get raw AttributeValue format
print(f"Escaneando tabla {VENTAS_TABLE}...")
items = []
resp = dynamodb.scan(TableName=VENTAS_TABLE)
items.extend(resp["Items"])
while "LastEvaluatedKey" in resp:
    resp = dynamodb.scan(TableName=VENTAS_TABLE, ExclusiveStartKey=resp["LastEvaluatedKey"])
    items.extend(resp["Items"])

print(f"Total de registros encontrados: {len(items)}")

migrated = 0
skipped  = 0

for raw_item in items:
    sale_id = raw_item["id"]["S"]
    detalle_av = raw_item.get("detalle", {})

    if not needs_migration(detalle_av):
        print(f"  [SKIP] {sale_id[:8]}... — ya está en formato correcto")
        skipped += 1
        continue

    # Convert detalle to plain Python dict
    plain_detalle = av_to_python(detalle_av)

    # Write back using high-level resource (stores as native types)
    # But we want it as a JSON string, so we serialize it
    detalle_json = json.dumps(plain_detalle, ensure_ascii=False)

    table.put_item(Item={
        "id":      sale_id,
        "detalle": detalle_json,
    })

    print(f"  [OK]   {sale_id[:8]}... → {detalle_json[:80]}...")
    migrated += 1

print(f"\nMigración completada: {migrated} migrados, {skipped} ya estaban correctos.")
