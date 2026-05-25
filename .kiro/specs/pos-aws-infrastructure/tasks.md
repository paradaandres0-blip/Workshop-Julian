# Implementation Plan: POS AWS Infrastructure

## Overview

Crear desde cero el proyecto SAM `pos-sam/` con dos funciones Lambda en Java 21 (Maven), dos tablas DynamoDB, un API Gateway REST y los roles IAM mínimos. El plan sigue un orden incremental: primero la estructura y el template IaC, luego los stubs Java compilables, y finalmente la validación y el despliegue.

## Tasks

- [x] 1. Crear estructura de directorios del proyecto SAM
  - Crear el directorio raíz `pos-sam/` y los subdirectorios `get-products/src/main/java/com/pos/lambda/` y `save-sale/src/main/java/com/pos/lambda/`
  - _Requirements: 1.1, 6.5_

- [x] 2. Crear el `template.yaml` SAM completo
  - [x] 2.1 Definir globals, tablas DynamoDB y roles IAM
    - Sección `Globals` con `Runtime: java21`, `Architectures: [x86_64]` y `MemorySize: 512`
    - Recurso `ProductosTable` (PK: `productCode`, tipo String, `BillingMode: PAY_PER_REQUEST`)
    - Recurso `VentasTable` (PK: `saleId`, tipo String, `BillingMode: PAY_PER_REQUEST`)
    - Rol IAM `GetProductsRole` con política inline que permite `dynamodb:GetItem`, `dynamodb:Query`, `dynamodb:Scan` sobre `ProductosTable`
    - Rol IAM `SaveSaleRole` con política inline que permite `dynamodb:PutItem` sobre `VentasTable`
    - _Requirements: 1.1, 1.3, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3_

  - [x] 2.2 Definir funciones Lambda y API Gateway
    - Recurso `GetProductsFunction`: `CodeUri: get-products/`, `Handler: com.pos.lambda.GetProductsHandler::handleRequest`, variable de entorno `PRODUCTOS_TABLE`, evento `GetProductsApi` (`GET /products`), referencia a `GetProductsRole`
    - Recurso `SaveSaleFunction`: `CodeUri: save-sale/`, `Handler: com.pos.lambda.SaveSaleHandler::handleRequest`, variable de entorno `VENTAS_TABLE`, evento `SaveSaleApi` (`POST /sales`), referencia a `SaveSaleRole`
    - API Gateway REST implícito via `AWS::Serverless::Api` o eventos inline con `Type: Api`
    - _Requirements: 1.1, 1.2, 1.4, 2.1, 3.1, 6.5_

  - [x] 2.3 Agregar sección `Outputs`
    - Output `ApiUrl` con la URL base del API Gateway (`!Sub "https://${ServerlessRestApi}.execute-api.${AWS::Region}.amazonaws.com/Prod/"`)
    - Output `GetProductsEndpoint` con la URL completa de `GET /products`
    - Output `SaveSaleEndpoint` con la URL completa de `POST /sales`
    - Output `ProductosTableName` con `!Ref ProductosTable`
    - Output `VentasTableName` con `!Ref VentasTable`
    - _Requirements: 8.3, 8.5_

- [x] 3. Crear `pom.xml` para `get-products/`
  - `groupId: com.pos`, `artifactId: get-products`, `version: 1.0.0`, `packaging: jar`, `java.version: 21`
  - Dependencias: `aws-lambda-java-core:1.2.3`, `aws-lambda-java-events:3.11.4`, `aws-java-sdk-dynamodb:1.12.x` (última estable), `jackson-databind:2.16.x`
  - Plugin `maven-shade-plugin` configurado para generar un fat-jar con `finalName: get-products` y `shadedArtifactAttached: false`
  - _Requirements: 6.1, 6.5_

- [x] 4. Crear `GetProductsHandler.java` stub
  - Paquete `com.pos.lambda`, clase `GetProductsHandler` que implementa `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
  - Método `handleRequest` retorna `APIGatewayProxyResponseEvent` con `statusCode: 200`, header `Content-Type: application/json` y body `{"products":[],"message":"stub"}`
  - _Requirements: 6.1, 6.2_

- [ ]* 4.1 Escribir tests unitarios para `GetProductsHandler` stub
  - Verificar que el handler retorna HTTP 200
  - Verificar que el body contiene `"products":[]` y `"message":"stub"`
  - _Requirements: 6.2_

- [x] 5. Crear `pom.xml` para `save-sale/`
  - `groupId: com.pos`, `artifactId: save-sale`, `version: 1.0.0`, `packaging: jar`, `java.version: 21`
  - Mismas dependencias que `get-products/`: `aws-lambda-java-core:1.2.3`, `aws-lambda-java-events:3.11.4`, `aws-java-sdk-dynamodb:1.12.x`, `jackson-databind:2.16.x`
  - Plugin `maven-shade-plugin` configurado para generar fat-jar con `finalName: save-sale`
  - _Requirements: 6.3, 6.5_

- [x] 6. Crear `SaveSaleHandler.java` stub
  - Paquete `com.pos.lambda`, clase `SaveSaleHandler` que implementa `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`
  - Método `handleRequest` retorna `APIGatewayProxyResponseEvent` con `statusCode: 201`, header `Content-Type: application/json` y body `{"saleId":"test-id","message":"stub"}`
  - _Requirements: 6.3, 6.4_

- [ ]* 6.1 Escribir tests unitarios para `SaveSaleHandler` stub
  - Verificar que el handler retorna HTTP 201
  - Verificar que el body contiene `"saleId":"test-id"` y `"message":"stub"`
  - _Requirements: 6.4_

- [x] 7. Checkpoint — Validar template y compilar
  - Ejecutar `sam validate` en `pos-sam/` y confirmar que no hay errores de sintaxis ni referencias rotas
  - Ejecutar `sam build` en `pos-sam/` y confirmar que ambas Lambdas compilan sin errores y los artefactos quedan en `.aws-sam/build/`
  - Asegurarse de que todos los tests pasen; consultar al usuario si surgen dudas
  - _Requirements: 1.5, 7.4, 7.5_

- [x] 8. Implementar lógica real de `GetProductsHandler`
  - [x] 8.1 Implementar validación del parámetro `q`
    - Leer `queryStringParameters.get("q")` del evento
    - Si `q` es null o blank, retornar HTTP 400 con `{"error":"El parámetro 'q' es requerido y no puede estar vacío"}`
    - _Requirements: 2.6_

  - [x] 8.2 Implementar detección de código de barras
    - Extraer método estático `BarcodeDetector.isBarcode(String q)` en clase separada `com.pos.lambda.BarcodeDetector`
    - Retorna `true` si `q` contiene una subcadena que coincide con `\d{7,}`
    - _Requirements: 2.2, 2.3_

  - [ ]* 8.3 Escribir property test para detección de barcode (Property 1)
    - **Property 1: Detección de código de barras**
    - Para cualquier string `q`, `BarcodeDetector.isBarcode(q)` debe retornar `true` si y solo si `q` contiene `\d{7,}`
    - Usar jqwik `@Property(tries = 100)` con `@ForAll String query`
    - Agregar dependencia `net.jqwik:jqwik:1.8.x` en `pom.xml` de `get-products/`
    - **Validates: Requirements 2.2, 2.3**

  - [x] 8.4 Implementar búsqueda en DynamoDB
    - Inicializar `AmazonDynamoDB` client y `DynamoDBMapper` en el constructor del handler (lectura de `PRODUCTOS_TABLE` desde variable de entorno)
    - Si `isBarcode(q)` → `GetItem` con `productCode = q`; si no → `Scan` con `FilterExpression` sobre `name` y `description`
    - Retornar HTTP 200 con `{"products": [...]}` (lista vacía si no hay resultados)
    - Capturar `AmazonDynamoDBException` y cualquier `Exception` → HTTP 500 con mensaje genérico
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.7_

  - [ ]* 8.5 Escribir property test para respuesta HTTP 200 (Property 2)
    - **Property 2: Respuesta HTTP 200 con lista de productos**
    - Para cualquier lista de productos (incluyendo vacía) retornada por un mock de DynamoDB, el handler debe responder HTTP 200 con esa lista bajo la clave `products`
    - Usar jqwik con `@ForAll List<Map<String, Object>> products`
    - **Validates: Requirements 2.4, 2.5**

  - [ ]* 8.6 Escribir property test para manejo de errores DynamoDB (Property 6 — GetProducts)
    - **Property 6: Manejo de errores DynamoDB → HTTP 500**
    - Para cualquier excepción lanzada por el mock de DynamoDB, el handler debe responder HTTP 500
    - **Validates: Requirements 2.7**

- [x] 9. Implementar lógica real de `SaveSaleHandler`
  - [x] 9.1 Implementar validación del body JSON
    - Parsear `event.getBody()` con Jackson `ObjectMapper`
    - Si el body es null, blank o no es JSON válido → HTTP 400 con `{"error":"El cuerpo de la solicitud no es un JSON válido"}`
    - Si `items` está ausente o es lista vacía → HTTP 400 con `{"error":"Se requiere al menos un producto en la venta"}`
    - _Requirements: 3.3, 3.5, 3.6_

  - [x] 9.2 Implementar persistencia en DynamoDB
    - Generar `saleId` con `UUID.randomUUID().toString()`
    - Agregar campo `createdAt` con timestamp ISO 8601 (`Instant.now().toString()`)
    - Construir `Item` de DynamoDB con `saleId`, `saleDetail`, `items` y `createdAt`
    - Ejecutar `PutItem` en la tabla `VENTAS_TABLE`
    - Retornar HTTP 201 con `{"saleId":"<uuid>","message":"Venta registrada exitosamente"}`
    - Capturar `AmazonDynamoDBException` y `Exception` → HTTP 500 con mensaje genérico
    - _Requirements: 3.2, 3.4, 3.7_

  - [ ]* 9.3 Escribir property test para unicidad de saleId (Property 3)
    - **Property 3: Unicidad de saleId generado**
    - Para dos invocaciones con requests válidos distintos, los `saleId` generados deben ser distintos y tener formato UUID v4
    - Usar jqwik con `@ForAll @From("validSaleRequests") String body`
    - Agregar dependencia `net.jqwik:jqwik:1.8.x` en `pom.xml` de `save-sale/`
    - **Validates: Requirements 3.2**

  - [ ]* 9.4 Escribir property test para respuesta HTTP 201 (Property 4)
    - **Property 4: Respuesta HTTP 201 con saleId**
    - Para cualquier request válido (con `saleDetail` y `items` no vacío), la respuesta debe ser HTTP 201 con un `saleId` en formato UUID v4
    - **Validates: Requirements 3.4**

  - [ ]* 9.5 Escribir property test para rechazo de JSON inválido (Property 5)
    - **Property 5: Rechazo de JSON inválido en SaveSale**
    - Para cualquier string que no sea JSON válido, el handler debe responder HTTP 400
    - Usar jqwik con `@ForAll @StringLength(min = 1) String invalidJson` filtrado para excluir JSON válido
    - **Validates: Requirements 3.5**

  - [ ]* 9.6 Escribir property test para manejo de errores DynamoDB (Property 6 — SaveSale)
    - **Property 6: Manejo de errores DynamoDB → HTTP 500**
    - Para cualquier excepción lanzada por el mock de DynamoDB, el handler debe responder HTTP 500
    - **Validates: Requirements 3.7**

- [x] 10. Checkpoint final — Pruebas locales y despliegue
  - Ejecutar `sam build` para compilar con la lógica real
  - Ejecutar `sam local start-api` y verificar manualmente `GET /products?q=1234567` y `POST /sales` con curl (ver comandos en design.md)
  - Ejecutar `sam deploy --guided` para el primer despliegue interactivo; confirmar que los Outputs muestran las URLs de los endpoints
  - Asegurarse de que todos los tests pasen; consultar al usuario si surgen dudas
  - _Requirements: 7.4, 7.5, 7.6, 8.1, 8.2, 8.3_

## Notes

- Las tareas marcadas con `*` son opcionales y pueden omitirse para un MVP más rápido
- El orden de las tareas es incremental: IaC primero, stubs compilables, luego lógica real
- Los property tests usan [jqwik](https://jqwik.net/) con JUnit 5; agregar la dependencia al `pom.xml` correspondiente antes de escribirlos
- `sam local start-api` requiere Docker instalado y en ejecución en el entorno local
- Para el despliegue, las credenciales AWS deben estar configuradas (`aws sts get-caller-identity` debe responder sin error)
- Las tareas 8 y 9 (lógica real) pueden ejecutarse en paralelo una vez que los stubs y el template estén validados
