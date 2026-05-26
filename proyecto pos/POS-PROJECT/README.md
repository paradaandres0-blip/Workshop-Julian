# 🍴 Supermercado El Tenedor — POS System

Sistema de Punto de Venta (POS) desarrollado con arquitectura hexagonal en backend, arquitectura en capas en frontend, e infraestructura serverless en AWS. Sigue principios SOLID y metodología Spec-Driven Development (SDD).

---

## 📁 Estructura del Proyecto

```
POS-PROJECT/
├── backend/        → API REST (Java 17 + Spring Boot) — uso local
├── frontend/       → Aplicación Web (Node.js 20 + Express)
├── pos-sam/        → Infraestructura AWS Serverless (SAM + Lambda + DynamoDB)
├── pos-repo/       → Documentación de referencia
└── WORKSHOP.md     → Guía del taller SDD
```

---

## 🔧 Stack Tecnológico

### Frontend
| Tecnología | Versión |
|---|---|
| Node.js | 20 |
| Express | 4.18.2 |
| Nunjucks | 3.2.4 |
| Jest | 29.7.0 |

### AWS Serverless (pos-sam)
| Tecnología | Detalle |
|---|---|
| AWS SAM | Infraestructura como código |
| AWS Lambda | Java 21 — 2 funciones |
| Amazon DynamoDB | 2 tablas (Productos, Ventas) |
| Amazon API Gateway | REST API — 14 endpoints |

### Backend local (opcional)
| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| PostgreSQL | — |

---

## 🏗️ Arquitectura

### AWS Serverless
```
Internet
    ↓
API Gateway REST (https://a0kok76acb.execute-api.us-east-1.amazonaws.com/Prod/)
    ↓
Lambda: GetProductsFunction  →  DynamoDB: ProductosTable
Lambda: SaveSaleFunction     →  DynamoDB: VentasTable + ProductosTable
```

### Frontend — Capas (Layered Architecture)
```
infrastructure (ApiClient, API Services, Express Routes)
    ↓
application (Orchestrators)
    ↓
domain (Models, Interfaces — pure JS/JSDoc)
    ↓
AWS API Gateway (BACKEND_URL en .env)
```

---

## 🚀 Cómo ejecutar

### Opción A — Frontend + AWS (recomendado)

El frontend se conecta directamente a las Lambdas en AWS. No requiere backend local.

```bash
cd frontend
# Asegúrate de que .env tenga:
# BACKEND_URL=https://a0kok76acb.execute-api.us-east-1.amazonaws.com/Prod
node src/app.js
```

La aplicación estará disponible en: `http://localhost:3000`

### Opción B — Frontend + Backend Java local

```bash
# Terminal 1 — Backend
cd backend
./mvnw spring-boot:run

# Terminal 2 — Frontend
cd frontend
# Cambia .env: BACKEND_URL=http://localhost:8080
node src/app.js
```

---

## 🌐 Rutas del Frontend

| Ruta | Descripción |
|---|---|
| `/sale` | Pantalla de Venta |
| `/inventory` | Inventario de Productos |
| `/reports` | Reportes y Estadísticas |

### Atajos de teclado en pantalla de venta

| Tecla | Acción |
|---|---|
| `↓` / `↑` | Navegar entre productos de la venta |
| `+` | Subir cantidad del producto seleccionado |
| `-` | Bajar cantidad del producto seleccionado |
| `F2` | Nueva Venta |
| `F4` | Ir a Pagar |
| `F5` | Cerrar venta activa |
| `Enter` | Confirmar pago |
| `Esc` | Cerrar modal |

---

## 📡 API REST — Endpoints AWS

Base URL: `https://a0kok76acb.execute-api.us-east-1.amazonaws.com/Prod`

### Productos
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/products` | Listar todos los productos |
| GET | `/api/v1/products/search?q=` | Buscar por nombre o código de barras |
| GET | `/api/v1/products/{id}` | Obtener producto por ID |
| POST | `/api/v1/products` | Crear producto |
| PUT | `/api/v1/products/{id}` | Actualizar producto |
| DELETE | `/api/v1/products/{id}` | Eliminar producto |

### Ventas
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/sales` | Crear venta |
| GET | `/api/v1/sales/{id}` | Obtener venta |
| POST | `/api/v1/sales/{id}/items` | Agregar ítem a la venta |
| POST | `/api/v1/sales/{id}/confirm` | Confirmar venta |

### Pagos
| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/payments` | Procesar pago |

### Reportes
| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/reports/sales` | Reporte de ventas por período |
| GET | `/api/v1/reports/top-products` | Top 10 productos más vendidos |
| GET | `/api/v1/reports/inventory` | Reporte de inventario |

---

## ☁️ Infraestructura AWS (pos-sam)

### Recursos desplegados
| Recurso | Nombre en AWS |
|---|---|
| Stack CloudFormation | `pos-sam` |
| Lambda — Productos | `pos-sam-GetProductsFunction` |
| Lambda — Ventas/Pagos/Reportes | `pos-sam-SaveSaleFunction` |
| DynamoDB — Productos | `pos-sam-ProductosTable-1KAR20ZY6LGTK` |
| DynamoDB — Ventas | `pos-sam-VentasTable-1ETRDZ6IFUUBT` |
| Región | `us-east-1` |

### Comandos de despliegue

```bash
cd pos-sam

# Compilar Lambdas
sam build

# Primer despliegue (interactivo)
sam deploy --guided

# Despliegues posteriores
sam deploy

# Cargar datos de prueba (50 productos)
python seed-products.py
```

### Requisitos para desplegar
- AWS CLI configurado (`aws configure`)
- SAM CLI instalado
- Maven en PATH
- Java 21+

---

## 📋 Variables de Entorno

### Frontend (`.env`)
```env
# AWS (producción)
BACKEND_URL=https://a0kok76acb.execute-api.us-east-1.amazonaws.com/Prod
PORT=3000

# Local (desarrollo con backend Java)
# BACKEND_URL=http://localhost:8080
```

---

## 🧪 Tests

```bash
cd frontend
npm test
```

---

## 📚 Documentación

- [`backend/design.md`](backend/design.md) — Diseño del backend
- [`frontend/design.md`](frontend/design.md) — Diseño del frontend
- [`WORKSHOP.md`](WORKSHOP.md) — Guía del taller SDD
- [`.kiro/specs/pos-aws-infrastructure/`](.kiro/specs/pos-aws-infrastructure/) — Spec de infraestructura AWS

---

## 👤 Autor

Workshop Julian V0 — Advanced Network Application Design
