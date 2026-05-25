# Design Document — POS AWS Infrastructure

## Overview

Este documento describe el diseño técnico completo de la infraestructura serverless AWS para el sistema POS (Point of Sale). La solución utiliza AWS SAM (Serverless Application Model) para definir todos los recursos como código (IaC), con dos funciones Lambda en Java 21, dos tablas DynamoDB y un API Gateway REST.

### Objetivos de diseño

- Infraestructura completamente reproducible mediante `template.yaml`
- Principio de mínimo privilegio en roles IAM
- Separación clara entre la capa de infraestructura (IaC) y la lógica de negocio (Lambda handlers)
- Entorno local funcional con SAM CLI para desarrollo y pruebas previas al despliegue

### Diagrama de arquitectura

```
┌─────────────────────────────────────────────────────────────────┐
│                          AWS Cloud                              │
│                                                                 │
│   ┌──────────────────────────────────────────────────────────┐  │
│   │                    API Gateway REST                      │  │
│   │                                                          │  │
│   │   GET  /products?q={query}    POST /sales                │  │
│   └──────────────┬────────────────────────┬──────────────────┘  │
│                  │                        │                     │
│                  ▼                        ▼                     │
│   ┌──────────────────────┐  ┌──────────────────────────────┐   │
│   │  GetProductsFunction │  │      SaveSaleFunction        │   │
│   │  (Java 21 Lambda)    │  │      (Java 21 Lambda)        │   │
│   │                      │  │                              │   │
│   │  - Barcode detection │  │  - UUID generation           │   │
│   │  - GetItem (barcode) │  │  - PutItem                   │   │
│   │  - Scan (text)       │  │                              │   │
│   └──────────┬───────────┘  └──────────────┬───────────────┘   │
│              │  IAM Role                   │  IAM Role         │
│              │  (GetItem,Query,Scan)        │  (PutItem)        │
│              ▼                             ▼                   │
│   ┌──────────────────────┐  ┌──────────────────────────────┐   │
│   │  DynamoDB: Productos │  │    DynamoDB: Ventas          │   │
│   │  PK: productCode(S)  │  │    PK: saleId (S)            │   │
│   │  PAY_PER_REQUEST     │  │    PAY_PER_REQUEST           │   │
│   └──────────────────────┘  └──────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘

Flujo local (SAM CLI):
  Developer → sam build → sam local start-api → localhost:3000
```

---

## Architecture

### Decisiones de diseño

**1. Proyectos Maven independientes por Lambda**

Cada Lambda tiene su propio `pom.xml` y directorio de fuentes. Esto permite:
- Compilación y empaquetado independiente
- Dependencias específicas por función (sin fat-jar compartido)
- Ciclos de build más rápidos al modificar solo una función

**2. API Gateway REST (no HTTP API)**

Se usa REST API en lugar de HTTP API porque:
- Mayor compatibilidad con SAM y herramientas de testing local
- Soporte nativo para `sam local start-api`
- Modelo de integración Lambda Proxy estándar

**3. Integración Lambda Proxy**

Ambas Lambdas usan integración `AWS_PROXY`, lo que significa que reciben el evento completo de API Gateway (`APIGatewayProxyRequestEvent`) y son responsables de construir la respuesta HTTP completa (`APIGatewayProxyResponseEvent`). Esto simplifica el routing y el manejo de headers.

**4. Roles IAM por función**

Cada Lambda tiene su propio rol IAM con permisos mínimos:
- `GetProductsFunction`: solo `dynamodb:GetItem`, `dynamodb:Query`, `dynamodb:Scan` sobre la tabla `Productos`
- `SaveSaleFunction`: solo `dynamodb:PutItem` sobre la tabla `Ventas`

**5. Variables de entorno para nombres de tablas**

Los nombres de las tablas DynamoDB se pasan como variables de entorno (`PRODUCTOS_TABLE`, `VENTAS_TABLE`) en lugar de hardcodearlos. Esto permite reutilizar el mismo código en diferentes stacks (dev, staging, prod).

---

## Components and Interfaces

### Estructura de directorios del proyecto SAM

```
pos-sam/
├── template.yaml              # Definición SAM de todos los recursos AWS
├── samconfig.toml             # Configuración de despliegue (generado por sam deploy --guided)
├── get-products/              # Proyecto Maven para GetProductsFunction
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/pos/lambda/
│                   └── GetProductsHandler.java
└── save-sale/                 # Proyecto Maven para SaveSaleFunction
    ├── pom.xml
    └── src/
        └── main/
            └── java/
                └── com/pos/lambda/
                    └── SaveSaleHandler.java
```

### Contrato de API

#### GET /products

**Request:**
```
GET /products?q={query}
```

| Parámetro | Tipo   | Requerido | Descripción                                      |
|-----------|--------|-----------|--------------------------------------------------|
| `q`       | String | Sí        | Texto de búsqueda o código de barras (≥7 dígitos)|

**Responses:**

```json
// HTTP 200 — Resultados encontrados
{
  "products": [
    {
      "productCode": "1234567",
      "name": "Producto Ejemplo",
      "description": "Descripción del producto",
      "price": 9.99
    }
  ]
}

// HTTP 200 — Sin resultados
{
  "products": []
}

// HTTP 400 — Parámetro ausente o vacío
{
  "error": "El parámetro 'q' es requerido y no puede estar vacío"
}

// HTTP 500 — Error interno
{
  "error": "Error al acceder a la base de datos"
}
```

#### POST /sales

**Request:**
```
POST /sales
Content-Type: application/json
```

```json
{
  "saleDetail": {
    "date": "2024-01-15",
    "cashier": "operador01",
    "total": 29.97
  },
  "items": [
    {
      "productCode": "1234567",
      "quantity": 3
    }
  ]
}
```

| Campo                    | Tipo   | Requerido | Descripción                          |
|--------------------------|--------|-----------|--------------------------------------|
| `saleDetail`             | Object | Sí        | Información general de la venta      |
| `items`                  | Array  | Sí        | Lista de productos (mínimo 1 item)   |
| `items[].productCode`    | String | Sí        | Código del producto                  |
| `items[].quantity`       | Number | Sí        | Cantidad vendida                     |

**Responses:**

```json
// HTTP 201 — Venta registrada exitosamente
{
  "saleId": "550e8400-e29b-41d4-a716-446655440000",
  "message": "Venta registrada exitosamente"
}

// HTTP 400 — JSON inválido
{
  "error": "El cuerpo de la solicitud no es un JSON válido"
}

// HTTP 400 — Items ausentes o vacíos
{
  "error": "Se requiere al menos un producto en la venta"
}

// HTTP 500 — Error interno
{
  "error": "Error al registrar la venta"
}
```

### Lógica de detección de código de barras

La `GetProductsFunction` implementa la siguiente lógica de routing:

```
isBarcode(q):
  → true  si q contiene una subcadena de ≥7 dígitos numéricos consecutivos
  → false en caso contrario

if isBarcode(q):
  → DynamoDB GetItem con productCode = q
else:
  → DynamoDB Scan con FilterExpression sobre name y description
```

El patrón regex para detección de barcode: `\d{7,}`

---

## Data Models

### Tabla DynamoDB: Productos

| Atributo      | Tipo   | Rol              | Descripción                              |
|---------------|--------|------------------|------------------------------------------|
| `productCode` | String | Partition Key    | Código único del producto (ej: barcode)  |
| `name`        | String | Atributo         | Nombre del producto                      |
| `description` | String | Atributo         | Descripción del producto                 |
| `price`       | Number | Atributo         | Precio unitario                          |

**Configuración:**
- Billing mode: `PAY_PER_REQUEST` (on-demand)
- Sin sort key
- Sin índices secundarios (GSI/LSI) en esta fase

**Nota sobre búsqueda por texto:** La búsqueda por nombre/descripción usa `Scan` con `FilterExpression`. Para volúmenes grandes de productos, se recomienda agregar un GSI o migrar a OpenSearch en fases posteriores.

### Tabla DynamoDB: Ventas

| Atributo      | Tipo   | Rol              | Descripción                              |
|---------------|--------|------------------|------------------------------------------|
| `saleId`      | String | Partition Key    | UUID v4 generado por la Lambda           |
| `saleDetail`  | Map    | Atributo         | Objeto con información general de venta  |
| `items`       | List   | Atributo         | Lista de items `{productCode, quantity}` |
| `createdAt`   | String | Atributo         | Timestamp ISO 8601 de creación           |

**Configuración:**
- Billing mode: `PAY_PER_REQUEST` (on-demand)
- Sin sort key
- `saleId` generado como UUID v4 (`java.util.UUID.randomUUID()`)

### Ejemplo de ítem en Ventas

```json
{
  "saleId": "550e8400-e29b-41d4-a716-446655440000",
  "saleDetail": {
    "date": "2024-01-15",
    "cashier": "operador01",
    "total": 29.97
  },
  "items": [
    { "productCode": "1234567", "quantity": 3 }
  ],
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

Esta feature combina IaC (donde PBT no aplica) con lógica de negocio en las Lambdas (donde PBT sí aplica). Las propiedades a continuación se enfocan en la lógica pura de las funciones Lambda, que puede testearse con mocks de DynamoDB sin incurrir en costos de AWS.

### Property 1: Detección de código de barras

*For any* string de consulta `q`, la función de detección de barcode debe clasificarla como barcode si y solo si contiene una subcadena de 7 o más dígitos numéricos consecutivos.

**Validates: Requirements 2.2, 2.3**

### Property 2: Respuesta HTTP 200 con lista de productos

*For any* lista de productos (incluyendo la lista vacía) retornada por DynamoDB, la `GetProductsFunction` debe responder con HTTP 200 y un JSON que contenga exactamente esa lista bajo la clave `products`.

**Validates: Requirements 2.4, 2.5**

### Property 3: Unicidad de saleId generado

*For any* dos invocaciones de `SaveSaleFunction` con requests válidos, los `saleId` generados deben ser distintos entre sí y deben tener formato UUID v4 válido.

**Validates: Requirements 3.2**

### Property 4: Respuesta HTTP 201 con saleId

*For any* request válido a `SaveSaleFunction` (con `saleDetail` y `items` no vacío), la respuesta debe ser HTTP 201 y el cuerpo debe contener un campo `saleId` con formato UUID v4 válido.

**Validates: Requirements 3.4**

### Property 5: Rechazo de JSON inválido en SaveSale

*For any* string que no sea JSON válido enviado como cuerpo a `SaveSaleFunction`, la respuesta debe ser HTTP 400.

**Validates: Requirements 3.5**

### Property 6: Manejo de errores DynamoDB → HTTP 500

*For any* excepción lanzada por el cliente DynamoDB (simulada con mock), tanto `GetProductsFunction` como `SaveSaleFunction` deben responder con HTTP 500 y un mensaje de error descriptivo.

**Validates: Requirements 2.7, 3.7**

---

## Error Handling

### Estrategia de manejo de errores

Ambas Lambdas siguen el mismo patrón de manejo de errores:

```
try {
  1. Validar input (→ HTTP 400 si inválido)
  2. Ejecutar operación DynamoDB
  3. Construir respuesta exitosa (HTTP 200/201)
} catch (DynamoDbException e) {
  → HTTP 500 con mensaje genérico (no exponer detalles internos)
} catch (Exception e) {
  → HTTP 500 con mensaje genérico
}
```

### Tabla de errores

| Condición                          | HTTP | Mensaje                                              |
|------------------------------------|------|------------------------------------------------------|
| Parámetro `q` ausente o vacío      | 400  | "El parámetro 'q' es requerido y no puede estar vacío" |
| Body no es JSON válido             | 400  | "El cuerpo de la solicitud no es un JSON válido"     |
| `items` ausente o vacío            | 400  | "Se requiere al menos un producto en la venta"       |
| Error de acceso a DynamoDB         | 500  | "Error al acceder a la base de datos"                |
| Error inesperado                   | 500  | "Error interno del servidor"                         |

### Logging

Cada Lambda registra en CloudWatch Logs:
- El evento de entrada (sin datos sensibles)
- El tipo de búsqueda realizada (barcode/text) para GetProducts
- El `saleId` generado para SaveSale
- Cualquier excepción capturada con stack trace

---

## Testing Strategy

### Enfoque dual: tests unitarios + property-based tests

La estrategia de testing se divide en dos capas complementarias:

**1. Tests unitarios (JUnit 5)** — para casos específicos y edge cases:
- Validación de input con valores concretos (null, vacío, whitespace)
- Comportamiento del stub (respuesta fija)
- Integración entre componentes

**2. Property-based tests (jqwik)** — para propiedades universales de la lógica Lambda:
- Clasificación de barcode vs. texto para cualquier string
- Formato de respuestas para cualquier lista de productos
- Unicidad de UUIDs generados
- Manejo de errores para cualquier excepción DynamoDB

### Configuración de property-based testing

- **Librería**: [jqwik](https://jqwik.net/) (integración nativa con JUnit 5)
- **Iteraciones mínimas**: 100 por propiedad (`@Property(tries = 100)`)
- **Tag de referencia**: `// Feature: pos-aws-infrastructure, Property {N}: {descripción}`

### Tests de infraestructura (IaC)

Para los recursos IaC (template.yaml), PBT no aplica. En su lugar:

- **Smoke tests**: Assertions sobre la estructura del `template.yaml` (runtime, billing mode, IAM policies, variables de entorno)
- **Validación SAM**: `sam validate` como paso de CI/CD
- **Tests de integración**: Despliegue en entorno de staging y verificación de endpoints reales

### Implementación de propiedades

#### Property 1 — Detección de barcode

```java
// Feature: pos-aws-infrastructure, Property 1: barcode detection
@Property(tries = 100)
void barcodeDetectionIsCorrect(@ForAll String query) {
    boolean result = BarcodeDetector.isBarcode(query);
    boolean expected = query != null && query.matches(".*\\d{7,}.*");
    assertThat(result).isEqualTo(expected);
}
```

#### Property 2 — Respuesta HTTP 200 con lista de productos

```java
// Feature: pos-aws-infrastructure, Property 2: HTTP 200 with product list
@Property(tries = 100)
void getProductsReturnsHttp200WithList(@ForAll List<@From("products") Map<String, Object>> products) {
    // Mock DynamoDB retorna la lista generada
    // Verificar que la respuesta es HTTP 200 y contiene exactamente esa lista
}
```

#### Property 3 — Unicidad de saleId

```java
// Feature: pos-aws-infrastructure, Property 3: saleId uniqueness
@Property(tries = 100)
void generatedSaleIdsAreUnique(@ForAll @From("validSaleRequests") SaleRequest req1,
                                @ForAll @From("validSaleRequests") SaleRequest req2) {
    String id1 = handler.generateSaleId(req1);
    String id2 = handler.generateSaleId(req2);
    assertThat(id1).isNotEqualTo(id2);
    assertThat(id1).matches(UUID_PATTERN);
    assertThat(id2).matches(UUID_PATTERN);
}
```

### Comandos de configuración local

```bash
# 1. Verificar credenciales AWS
aws sts get-caller-identity

# 2. Compilar ambas Lambdas
sam build

# 3. Validar el template
sam validate

# 4. Levantar API local (puerto 3000)
sam local start-api

# 5. Probar endpoints localmente
curl "http://localhost:3000/products?q=1234567"
curl -X POST http://localhost:3000/sales \
  -H "Content-Type: application/json" \
  -d '{"saleDetail":{"cashier":"test"},"items":[{"productCode":"1234567","quantity":1}]}'

# 6. Primer despliegue (interactivo)
sam deploy --guided

# 7. Despliegues posteriores
sam deploy

# 8. Eliminar stack
sam delete
```
