# POS System – Point of Sale (POS)

## 📘 Resumen del desarrollo

Este proyecto es una **aplicación full‑stack** para la gestión de ventas en un supermercado. Fue desarrollado siguiendo la metodología **Spec‑Driven Development (SDD)**, con tres archivos principales de especificación:
- `requirements.md` – Qué debe hacer el sistema.
- `design.md` – Cómo está estructurado (arquitectura hexagonal en el backend y arquitectura en capas con DIP en el frontend).
- `tasks.md` – Lista de tareas incrementales que el agente Kiro ejecutó.

Se implementó el backend con **Spring Boot 3.x** (Java 17) y el frontend con **Node.js 20 + Express** (Nunjucks). Todo el código está cubierto con pruebas unitarias (JUnit 5 + Mockito para el backend, Jest + Sinon para el frontend).

## 🛠️ Requisitos previos
- **Java 17** (JDK)
- **Maven** (`mvn` en el PATH)
- **Node.js 20** (incluye npm)
- **Git** (para clonar el repo)
- Navegador para la UI (Chrome/Firefox recomendado)

## 📂 Estructura del proyecto
```
POS-PROJECT/
├─ backend/          ← API REST (Spring Boot)
│   ├─ src/main/java/com/pos/...   (código fuente)
│   └─ pom.xml                    (build Maven)
├─ frontend/         ← Aplicación web (Node/Express)
│   ├─ src/                     (código fuente)
│   ├─ package.json              (dependencias npm)
│   └─ .env                      (variables de entorno)
└─ pos-repo/          ← Documentación y specs
    ├─ README.md                (este archivo)
    ├─ specifications.md
    ├─ design.md
    └─ tasks.md
```

## 🚀 Cómo ejecutar el proyecto en local
### Backend (Spring Boot)
```bash
# 1. Clonar el repositorio (si no lo has hecho)
git clone <repo‑url>
cd POS-PROJECT/backend

# 2. Compilar y ejecutar
mvn clean install
mvn spring-boot:run   # el API se levanta en http://localhost:8080
```

### Frontend (Node/Express)
```bash
cd ../frontend

# 1. Instalar dependencias
npm install

# 2. Crear archivo .env (ejemplo)
cat > .env <<EOF
BACKEND_URL=http://localhost:8080
PORT=3000
EOF

# 3. Ejecutar la aplicación
npm start   # abre http://localhost:3000 en el navegador
```

### Ejecutar pruebas
#### Backend
```bash
cd ../backend
mvn test   # JUnit + Mockito
```
#### Frontend
```bash
cd ../frontend
npm test   # Jest + Sinon
```

## 📦 Funcionalidades del sistema
| Área | Funcionalidad | Descripción |
|------|----------------|-------------|
| **Producto** | Listado de productos | Obtención paginada de todos los productos disponibles. |
| | Búsqueda por nombre/categoría | Endpoint `/api/v1/products?search=…`. |
| | CRUD de productos | Crear, actualizar y eliminar productos (POST/PUT/DELETE). |
| **Venta** | Crear venta | POST `/api/v1/sales` crea una venta en estado `ACTIVE`. |
| | Añadir ítems a venta | POST `/api/v1/sales/{id}/items` añade productos a la venta. |
| | Confirmar venta | POST `/api/v1/sales/{id}/confirm` cambia a `COMPLETED`. |
| | Cancelar/ revertir venta | DELETE `/api/v1/sales/{id}` (solo si está `ACTIVE` o `FROZEN`). |
| **Pago** | Procesar pagos | POST `/api/v1/payments` maneja cash, credit, etc. |
| | Validación de fondos | Devuelve error `INSUFFICIENT_FUNDS` si no hay suficiente dinero. |
| **Reportes** | Ventas por rango de fechas | GET `/api/v1/reports/sales?from=…&to=…`. |
| | Top 10 productos más vendidos | GET `/api/v1/reports/top-products`. |
| | Inventario actual | GET `/api/v1/reports/inventory`. |
| **UI Frontend** | Catálogo de productos | Vista con paginación y buscador. |
| | Carrito interactivo | Tabla de ítems con cantidades editables y total en tiempo real. |
| | Checkout multi‑método | Pago en efectivo, tarjeta, o crédito. |
| | Recepción digital | Genera y muestra un recibo HTML. |
| | Offline fallback | Funcionalidad de búsqueda y carrito disponible sin conexión. |
| | Reportes visuales | Tablas y filtros para ventas, top productos e inventario. |

## 🛡️ Manejo de errores
- Todos los endpoints devuelven JSON con `error_code`, `message` y `timestamp`.
- Códigos comunes: `DUPLICATE_PRODUCT`, `INSUFFICIENT_STOCK`, `INVALID_PAYMENT`, `SALE_NOT_FOUND`, `INVALID_REQUEST`.

## 📚 Documentación adicional
- **OpenAPI UI**: http://localhost:8080/api/v1/docs
- Especificaciones completas en `design.md` y `requirements.md` dentro de `pos-repo/`.

---

*¡Listo! Con estos pasos puedes clonar, construir y probar el POS localmente.*
