# Requirements Document

## Introduction

Esta feature define la infraestructura serverless en AWS para el sistema POS (Point of Sale), utilizando AWS SAM (Serverless Application Model). La infraestructura incluye dos funciones Lambda en Java, dos tablas DynamoDB y dos endpoints en API Gateway. El objetivo es tener una infraestructura completamente definida como código (IaC) que pueda desplegarse de forma reproducible, junto con un entorno de desarrollo local configurado con SAM CLI para pruebas antes del despliegue.

## Glossary

- **SAM**: AWS Serverless Application Model — framework de IaC para definir recursos serverless en AWS mediante un archivo `template.yaml`.
- **SAM_CLI**: Herramienta de línea de comandos que permite construir, probar localmente y desplegar aplicaciones SAM.
- **Lambda**: Función AWS Lambda — unidad de cómputo serverless que ejecuta código en respuesta a eventos.
- **GetProducts_Lambda**: Función Lambda en Java responsable de buscar productos en la tabla Products.
- **SaveSale_Lambda**: Función Lambda en Java responsable de registrar ventas en la tabla Sales.
- **API_Gateway**: Servicio de AWS que expone los endpoints HTTP REST que invocan las funciones Lambda.
- **DynamoDB**: Base de datos NoSQL administrada de AWS utilizada para almacenar productos y ventas.
- **Products_Table**: Tabla DynamoDB que almacena los productos del sistema POS. Clave primaria: `productCode`. Nombre en AWS: `Productos`.
- **Sales_Table**: Tabla DynamoDB que almacena las ventas registradas. Clave primaria: `saleId`. Nombre en AWS: `Ventas`.
- **Barcode**: Código de barras — cadena numérica de mínimo 7 dígitos que identifica unívocamente un producto.
- **Template**: Archivo `template.yaml` que define todos los recursos de la infraestructura SAM.
- **Stack**: Conjunto de recursos AWS creados a partir del Template mediante AWS CloudFormation.
- **Stub**: Implementación mínima de una Lambda que retorna una respuesta fija, usada para validar que la infraestructura despliega correctamente.
- **AWS_CLI**: Herramienta de línea de comandos para interactuar con servicios AWS.
- **Local_Environment**: Entorno de desarrollo local configurado con SAM CLI para simular la infraestructura AWS.

---

## Requirements

### Requirement 1: Definición del Template SAM

**User Story:** Como desarrollador, quiero tener todos los recursos AWS definidos en un único archivo `template.yaml`, para que la infraestructura sea reproducible y gestionable como código.

#### Acceptance Criteria

1. THE Template SHALL definir los recursos: `GetProducts_Lambda`, `SaveSale_Lambda`, `Products_Table`, `Sales_Table` y `API_Gateway` en un único archivo `template.yaml`.
2. THE Template SHALL especificar el runtime `java21` para ambas funciones Lambda.
3. THE Template SHALL asignar a cada Lambda un rol IAM con permisos mínimos necesarios para acceder a su tabla DynamoDB correspondiente.
4. THE Template SHALL definir variables de entorno en cada Lambda con el nombre de la tabla DynamoDB que utiliza.
5. WHEN el Template es procesado por SAM CLI, THE SAM_CLI SHALL validar el Template sin errores de sintaxis ni de referencias.

---

### Requirement 2: Función Lambda GetProducts

**User Story:** Como operador del POS, quiero buscar productos por código de barras o por texto, para que pueda encontrar rápidamente los productos durante una venta.

#### Acceptance Criteria

1. THE API_Gateway SHALL exponer el endpoint `GET /products` con el parámetro de consulta `q`.
2. WHEN el parámetro `q` contiene una cadena de mínimo 7 dígitos numéricos consecutivos, THE GetProducts_Lambda SHALL buscar un único producto en la Products_Table cuyo `productCode` coincida exactamente con dicho valor.
3. WHEN el parámetro `q` contiene texto que no cumple el patrón de código de barras, THE GetProducts_Lambda SHALL buscar todos los productos en la Products_Table cuyo nombre o descripción contengan el texto proporcionado como subcadena.
4. WHEN la búsqueda retorna resultados, THE GetProducts_Lambda SHALL responder con HTTP 200 y un JSON con la lista de productos encontrados.
5. WHEN la búsqueda no retorna resultados, THE GetProducts_Lambda SHALL responder con HTTP 200 y un JSON con una lista vacía.
6. IF el parámetro `q` está ausente o vacío, THEN THE GetProducts_Lambda SHALL responder con HTTP 400 y un mensaje de error descriptivo.
7. IF ocurre un error al acceder a la Products_Table, THEN THE GetProducts_Lambda SHALL responder con HTTP 500 y un mensaje de error descriptivo.

---

### Requirement 3: Función Lambda SaveSale

**User Story:** Como operador del POS, quiero registrar una venta con su detalle de productos y cantidades, para que quede persistida en el sistema.

#### Acceptance Criteria

1. THE API_Gateway SHALL exponer el endpoint `POST /sales` que acepta un cuerpo JSON.
2. WHEN se recibe una solicitud `POST /sales` con un JSON válido, THE SaveSale_Lambda SHALL persistir la venta en la Sales_Table con un `saleId` único generado por la Lambda.
3. THE SaveSale_Lambda SHALL aceptar un JSON con los campos: `saleDetail` (objeto con información de la venta) y `items` (lista de objetos con `productCode` y `quantity`).
4. WHEN la venta es persistida exitosamente, THE SaveSale_Lambda SHALL responder con HTTP 201 y un JSON que incluya el `saleId` generado y un mensaje de confirmación.
5. IF el cuerpo de la solicitud no es un JSON válido, THEN THE SaveSale_Lambda SHALL responder con HTTP 400 y un mensaje de error descriptivo.
6. IF el campo `items` está ausente o es una lista vacía, THEN THE SaveSale_Lambda SHALL responder con HTTP 400 indicando que se requiere al menos un producto en la venta.
7. IF ocurre un error al acceder a la Sales_Table, THEN THE SaveSale_Lambda SHALL responder con HTTP 500 y un mensaje de error descriptivo.

---

### Requirement 4: Tabla DynamoDB Products

**User Story:** Como desarrollador, quiero una tabla DynamoDB para almacenar productos, para que los datos persistan de forma escalable y administrada.

#### Acceptance Criteria

1. THE Template SHALL definir la Products_Table con clave primaria de tipo String llamada `productCode`.
2. THE Template SHALL configurar la Products_Table con modo de capacidad `PAY_PER_REQUEST` (on-demand).
3. THE GetProducts_Lambda SHALL tener permisos IAM para ejecutar las operaciones `dynamodb:GetItem`, `dynamodb:Query` y `dynamodb:Scan` sobre la Products_Table.

---

### Requirement 5: Tabla DynamoDB Sales

**User Story:** Como desarrollador, quiero una tabla DynamoDB para almacenar ventas, para que los registros de ventas persistan de forma escalable y administrada.

#### Acceptance Criteria

1. THE Template SHALL definir la Sales_Table con clave primaria de tipo String llamada `saleId`.
2. THE Template SHALL configurar la Sales_Table con modo de capacidad `PAY_PER_REQUEST` (on-demand).
3. THE SaveSale_Lambda SHALL tener permisos IAM para ejecutar la operación `dynamodb:PutItem` sobre la Sales_Table.

---

### Requirement 6: Implementaciones Stub de las Lambdas

**User Story:** Como desarrollador, quiero implementaciones stub de las funciones Lambda, para que pueda verificar que la infraestructura despliega y responde correctamente antes de implementar la lógica de negocio.

#### Acceptance Criteria

1. THE GetProducts_Lambda SHALL implementar la interfaz `RequestHandler` de AWS Lambda SDK para Java.
2. WHEN la GetProducts_Lambda es invocada, THE GetProducts_Lambda SHALL retornar una respuesta HTTP 200 con un JSON que contenga una lista vacía de productos y un mensaje indicando que es una implementación stub.
3. THE SaveSale_Lambda SHALL implementar la interfaz `RequestHandler` de AWS Lambda SDK para Java.
4. WHEN la SaveSale_Lambda es invocada, THE SaveSale_Lambda SHALL retornar una respuesta HTTP 201 con un JSON que contenga un `saleId` de prueba y un mensaje indicando que es una implementación stub.
5. THE Template SHALL referenciar los artefactos compilados de ambas Lambdas mediante la propiedad `CodeUri` apuntando a sus respectivos directorios de proyecto Maven.

---

### Requirement 7: Configuración del Entorno Local

**User Story:** Como desarrollador, quiero configurar el entorno local con AWS CLI y SAM CLI, para que pueda construir, probar y desplegar la infraestructura desde mi máquina.

#### Acceptance Criteria

1. THE SAM_CLI SHALL estar instalado en el entorno local con una versión compatible con el runtime `java21`.
2. THE AWS_CLI SHALL estar configurado en el entorno local con credenciales válidas (Access Key ID, Secret Access Key y región por defecto).
3. WHEN se ejecuta `aws sts get-caller-identity`, THE AWS_CLI SHALL retornar el ARN de la identidad configurada sin errores.
4. WHEN se ejecuta `sam validate` en el directorio del proyecto, THE SAM_CLI SHALL retornar confirmación de que el Template es válido.
5. WHEN se ejecuta `sam build` en el directorio del proyecto, THE SAM_CLI SHALL compilar ambas Lambdas Java y preparar los artefactos de despliegue sin errores.
6. WHEN se ejecuta `sam local start-api`, THE SAM_CLI SHALL levantar un servidor HTTP local que exponga los endpoints `GET /products` y `POST /sales` invocando las Lambdas stub localmente.

---

### Requirement 8: Flujo de Despliegue en AWS

**User Story:** Como desarrollador, quiero un flujo de despliegue claro y reproducible, para que pueda publicar la infraestructura en AWS de forma consistente.

#### Acceptance Criteria

1. WHEN se ejecuta `sam deploy --guided` por primera vez, THE SAM_CLI SHALL solicitar de forma interactiva el nombre del Stack, la región AWS y los parámetros de despliegue, y guardar la configuración en un archivo `samconfig.toml`.
2. WHEN se ejecuta `sam deploy` en ejecuciones posteriores, THE SAM_CLI SHALL utilizar la configuración guardada en `samconfig.toml` para desplegar sin requerir parámetros adicionales.
3. WHEN el despliegue finaliza exitosamente, THE SAM_CLI SHALL mostrar en la salida los endpoints del API_Gateway generados para `GET /products` y `POST /sales`.
4. WHEN se ejecuta `sam delete`, THE SAM_CLI SHALL eliminar todos los recursos del Stack de AWS sin dejar recursos huérfanos.
5. THE Template SHALL incluir una sección `Outputs` que exponga las URLs de los endpoints del API_Gateway y los nombres de las tablas DynamoDB creadas.
