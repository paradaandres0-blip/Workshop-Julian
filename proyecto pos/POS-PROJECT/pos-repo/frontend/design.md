# Design — Frontend POS
## Node.js 20 + Express 4 | Layered Architecture | SOLID | DIP

---

## 1. Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND                                │
│                                                                 │
│  ┌──────────────────┐                                           │
│  │  infrastructure  │  ← ApiClient, *ApiService, Express routes │
│  └────────┬─────────┘                                           │
│           │ depends on                                          │
│  ┌────────▼─────────┐                                           │
│  │   application    │  ← Orchestrators (use IServices)          │
│  └────────┬─────────┘                                           │
│           │ depends on                                          │
│  ┌────────▼─────────┐                                           │
│  │     domain       │  ← Models + Interfaces (pure JSDoc)       │
│  └──────────────────┘                                           │
│                                                                 │
│  ┌──────────────────┐                                           │
│  │   composition/   │  ← container.js (DI Root — single point   │
│  │   container.js   │    of dependency instantiation)           │
│  └──────────────────┘                                           │
└─────────────────────────────────────────────────────────────────┘
                          │ HTTP/JSON /api/v1/*
                          ▼
                    Backend POS API
```

**Golden rule:** Orchestrators only know interfaces (`IService`). They never import `ApiClient` or any concrete implementation.

---

## 2. Directory Structure

```
frontend/
├── src/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── product.js          ← { id, code, name, price, stockLevel, lowStock, outOfStock }
│   │   │   ├── sale.js             ← { id, status, items, total }
│   │   │   └── payment.js          ← { id, saleId, method, amount, change }
│   │   └── service/
│   │       ├── IApiClient.js       ← interface: get, post, put, delete
│   │       ├── IProductService.js  ← interface: getAll, getById, create, update, delete
│   │       ├── ISaleService.js     ← interface: createSale, addItem, confirmSale, getSale
│   │       ├── IPaymentService.js  ← interface: processPayment
│   │       └── IReportService.js   ← interface: getSalesReport, getTopProducts, getInventory
│   ├── application/
│   │   ├── SaleOrchestrator.js
│   │   ├── InventoryOrchestrator.js
│   │   └── ReportOrchestrator.js
│   ├── infrastructure/
│   │   ├── api/
│   │   │   ├── ApiClient.js        ← implements IApiClient (Fetch API)
│   │   │   ├── ApiError.js         ← class ApiError extends Error
│   │   │   ├── ProductApiService.js
│   │   │   ├── SaleApiService.js
│   │   │   ├── PaymentApiService.js
│   │   │   └── ReportApiService.js
│   │   └── routes/
│   │       ├── saleRoutes.js
│   │       ├── inventoryRoutes.js
│   │       └── reportRoutes.js
│   ├── views/
│   │   ├── layout.njk              ← base template
│   │   ├── sale/
│   │   │   └── sale.njk
│   │   ├── inventory/
│   │   │   └── inventory.njk
│   │   └── reports/
│   │       └── reports.njk
│   ├── composition/
│   │   └── container.js            ← DI Root
│   └── app.js                      ← Express entry point
├── test/
│   ├── application/
│   │   └── SaleOrchestrator.test.js
│   └── infrastructure/api/
│       └── ApiClient.test.js
├── .env
└── package.json
```

---

## 3. Domain Interfaces (JSDoc)

```javascript
// domain/service/IApiClient.js
class IApiClient {
    /** @param {string} path @returns {Promise<any>} */
    async get(path) { throw new Error('Not implemented'); }
    /** @param {string} path @param {object} body @returns {Promise<any>} */
    async post(path, body) { throw new Error('Not implemented'); }
    /** @param {string} path @param {object} body @returns {Promise<any>} */
    async put(path, body) { throw new Error('Not implemented'); }
    /** @param {string} path @returns {Promise<void>} */
    async delete(path) { throw new Error('Not implemented'); }
}
module.exports = IApiClient;

// domain/service/ISaleService.js
class ISaleService {
    /** @returns {Promise<import('../model/sale')>} */
    async createSale() { throw new Error('Not implemented'); }
    /** @param {string} saleId @param {{productId: string, quantity: number}} cmd */
    async addItem(saleId, cmd) { throw new Error('Not implemented'); }
    /** @param {string} saleId @returns {Promise<import('../model/sale')>} */
    async confirmSale(saleId) { throw new Error('Not implemented'); }
    /** @param {string} saleId @returns {Promise<import('../model/sale')>} */
    async getSale(saleId) { throw new Error('Not implemented'); }
}
module.exports = ISaleService;
```

---

## 4. ApiClient Implementation

```javascript
// infrastructure/api/ApiClient.js
const IApiClient = require('../../domain/service/IApiClient');
const ApiError = require('./ApiError');

class ApiClient extends IApiClient {
    /** @param {string} baseUrl */
    constructor(baseUrl) {
        super();
        this._baseUrl = baseUrl;
    }

    async get(path) { return this._request('GET', path); }
    async post(path, body) { return this._request('POST', path, body); }
    async put(path, body) { return this._request('PUT', path, body); }
    async delete(path) { return this._request('DELETE', path); }

    async _request(method, path, body) {
        const res = await fetch(`${this._baseUrl}${path}`, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: body ? JSON.stringify(body) : undefined,
        });
        if (!res.ok) {
            const err = await res.json();
            throw new ApiError(err.error_code, err.message);
        }
        return res.status === 204 ? null : res.json();
    }
}
module.exports = ApiClient;

// infrastructure/api/ApiError.js
class ApiError extends Error {
    constructor(errorCode, message) {
        super(message);
        this.name = 'ApiError';
        this.errorCode = errorCode;
    }
}
module.exports = ApiError;
```

---

## 5. Orchestrators (Application Layer)

```javascript
// application/SaleOrchestrator.js
class SaleOrchestrator {
    /**
     * @param {import('../domain/service/ISaleService')} saleService
     * @param {import('../domain/service/IPaymentService')} paymentService
     */
    constructor(saleService, paymentService) {
        this._saleService = saleService;
        this._paymentService = paymentService;
    }

    async startSale() {
        return this._saleService.createSale();
    }

    async addProduct(saleId, productId, quantity) {
        return this._saleService.addItem(saleId, { productId, quantity });
    }

    async checkout(saleId, method, amount) {
        await this._saleService.confirmSale(saleId);
        return this._paymentService.processPayment({ saleId, method, amount });
    }
}
module.exports = SaleOrchestrator;

// application/InventoryOrchestrator.js
class InventoryOrchestrator {
    /** @param {import('../domain/service/IProductService')} productService */
    constructor(productService) {
        this._productService = productService;
    }

    async listProducts() { return this._productService.getAll(); }
    async createProduct(data) { return this._productService.create(data); }
    async updateProduct(id, data) { return this._productService.update(id, data); }
    async deleteProduct(id) { return this._productService.delete(id); }
}
module.exports = InventoryOrchestrator;
```

---

## 6. DI Root — container.js

```javascript
// composition/container.js
require('dotenv').config();
const ApiClient = require('../infrastructure/api/ApiClient');
const ProductApiService = require('../infrastructure/api/ProductApiService');
const SaleApiService = require('../infrastructure/api/SaleApiService');
const PaymentApiService = require('../infrastructure/api/PaymentApiService');
const ReportApiService = require('../infrastructure/api/ReportApiService');
const SaleOrchestrator = require('../application/SaleOrchestrator');
const InventoryOrchestrator = require('../application/InventoryOrchestrator');
const ReportOrchestrator = require('../application/ReportOrchestrator');

// Single instantiation point for all dependencies
const apiClient = new ApiClient(process.env.BACKEND_URL);

const productService = new ProductApiService(apiClient);
const saleService = new SaleApiService(apiClient);
const paymentService = new PaymentApiService(apiClient);
const reportService = new ReportApiService(apiClient);

const saleOrchestrator = new SaleOrchestrator(saleService, paymentService);
const inventoryOrchestrator = new InventoryOrchestrator(productService);
const reportOrchestrator = new ReportOrchestrator(reportService);

module.exports = { saleOrchestrator, inventoryOrchestrator, reportOrchestrator };
```

---

## 7. Express Routes

```javascript
// infrastructure/routes/saleRoutes.js
const express = require('express');

module.exports = (saleOrchestrator) => {
    const router = express.Router();

    router.get('/', async (req, res) => {
        res.render('sale/sale', { sale: null, error: null });
    });

    router.post('/start', async (req, res) => {
        try {
            const sale = await saleOrchestrator.startSale();
            res.json(sale);
        } catch (err) {
            res.status(400).json({ error: err.message });
        }
    });

    router.post('/:id/item', async (req, res) => {
        try {
            const { productId, quantity } = req.body;
            const sale = await saleOrchestrator.addProduct(req.params.id, productId, quantity);
            res.json(sale);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    router.post('/:id/pay', async (req, res) => {
        try {
            const { method, amount } = req.body;
            const payment = await saleOrchestrator.checkout(req.params.id, method, amount);
            res.json(payment);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    return router;
};
```

---

## 8. Screens (Nunjucks Views)

### Sale Screen (`sale.njk`)
- Product search field (code or name)
- Sale_Items table: product, editable quantity, subtotal
- Sale total (real-time update with inline JS)
- Payment form: Payment_Method selector + amount field
- Calculated change display area
- Error message area (without closing the active sale)

### Inventory Screen (`inventory.njk`)
- Paginated table: code, name, price, stock
- Yellow row if `low_stock: true`
- Red row if `out_of_stock: true`
- Create/edit form with client-side validation
- Delete button with confirmation dialog

### Reports Screen (`reports.njk`)
- Date filters (from / to)
- Sales table: total Sales, amount collected, breakdown by payment method
- Top 10 best-selling products table
- Inventory table with stock indicators

---

## 9. Design Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Template engine | Nunjucks | Native Express integration, clean syntax |
| HTTP client | Native Fetch API | No external dependencies in Node.js 18+ |
| Error handling | `ApiError` class | Decouples HTTP errors from application logic |
| DI | Constructor injection | Enables mocks in tests, satisfies DIP |
| DI Root | `container.js` | Single composition point, easy to maintain and test |
| Route modules | Factory function `(orchestrator) => router` | Allows injecting the orchestrator without coupling |
