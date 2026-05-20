# 🍴 Supermercado El Tenedor — POS System

Sistema de Punto de Venta (POS) desarrollado con arquitectura hexagonal en backend y arquitectura en capas en frontend, siguiendo principios SOLID y metodología Spec-Driven Development (SDD).

---

## 📁 Estructura del Proyecto

```
POS-PROJECT/
├── backend/        → API REST (Java 17 + Spring Boot)
├── frontend/       → Aplicación Web (Node.js 20 + Express)
├── pos-repo/       → Documentación de referencia
└── WORKSHOP.md     → Guía del taller SDD
```

---

## 🔧 Stack Tecnológico

### Backend
| Tecnología | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 4.0.6 |
| Spring Data JPA | — |
| PostgreSQL | — |
| Lombok | — |
| Springdoc OpenAPI | 2.5.0 |

### Frontend
| Tecnología | Versión |
|---|---|
| Node.js | 20 |
| Express | 4.18.2 |
| Nunjucks | 3.2.4 |
| Jest | 29.7.0 |
| Sinon | 17.0.1 |

---

## 🏗️ Arquitectura

### Backend — Hexagonal (Ports & Adapters)
```
infrastructure (REST controllers, JPA adapters)
    ↓
application (Use Cases)
    ↓
domain (Entities, Ports, Exceptions — pure Java)
    ↓
PostgreSQL
```

### Frontend — Capas (Layered Architecture)
```
infrastructure (ApiClient, API Services, Express Routes)
    ↓
application (Orchestrators)
    ↓
domain (Models, Interfaces — pure JS/JSDoc)
```

**Regla de oro:** Las dependencias siempre apuntan hacia el dominio. El dominio no conoce ningún framework.

---

## 🚀 Cómo ejecutar

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

> Requiere PostgreSQL corriendo. Configura las variables de entorno de conexión antes de iniciar.

La API estará disponible en: `http://localhost:8080`  
Documentación OpenAPI: `http://localhost:8080/docs`

### Frontend

```bash
cd frontend
npm install
npm start
```

La aplicación estará disponible en: `http://localhost:3000`

> Asegúrate de que el backend esté corriendo antes de iniciar el frontend.

---

## 🌐 Rutas del Frontend

| Ruta | Descripción |
|---|---|
| `/sale` | Pantalla de Venta |
| `/inventory` | Inventario de Productos |
| `/reports` | Reportes y Estadísticas |

---

## 📡 API REST — Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/v1/products` | Listar productos |
| POST | `/api/v1/products` | Crear producto |
| PUT | `/api/v1/products/{id}` | Actualizar producto |
| DELETE | `/api/v1/products/{id}` | Eliminar producto |
| POST | `/api/v1/sales` | Crear venta |
| GET | `/api/v1/sales/{id}` | Obtener venta |
| POST | `/api/v1/sales/{id}/items` | Agregar ítem |
| POST | `/api/v1/sales/{id}/confirm` | Confirmar venta |
| POST | `/api/v1/payments` | Procesar pago |
| GET | `/api/v1/reports/sales` | Reporte de ventas |
| GET | `/api/v1/reports/top-products` | Top 10 productos |
| GET | `/api/v1/reports/inventory` | Reporte de inventario |

---

## 🧪 Tests

### Frontend
```bash
cd frontend
npm test
```

Cubre:
- `SaleOrchestrator` — verifica el orden de operaciones en checkout
- `ApiClient` — verifica manejo de errores HTTP y respuestas 204

---

## 📋 Variables de Entorno

### Frontend (`.env`)
```env
BACKEND_URL=http://localhost:8080
PORT=3000
```

---

## 📚 Documentación

- [`backend/design.md`](backend/design.md) — Diseño de arquitectura del backend
- [`backend/specifications.md`](backend/specifications.md) — Especificaciones funcionales y propiedades de corrección
- [`frontend/design.md`](frontend/design.md) — Diseño de arquitectura del frontend
- [`frontend/specifications.md`](frontend/specifications.md) — Especificaciones del frontend
- [`WORKSHOP.md`](WORKSHOP.md) — Guía del taller Spec-Driven Development

---

## 👤 Autor

Workshop Julian V0 — Advanced Network Application Design
