const IProductService = require('../../domain/service/IProductService');

class ProductApiService extends IProductService {
    /** @param {import('../../domain/service/IApiClient')} apiClient */
    constructor(apiClient) {
        super();
        this._apiClient = apiClient;
    }

    async getAll() {
        return this._apiClient.get('/api/v1/products');
    }

    async search(query) {
        return this._apiClient.get(`/api/v1/products/search?q=${encodeURIComponent(query)}`);
    }

    async getById(id) {
        return this._apiClient.get(`/api/v1/products/${id}`);
    }

    async create(data) {
        return this._apiClient.post('/api/v1/products', {
            code: data.code,
            name: data.name,
            price: data.price,
            stock_level: data.stockLevel,
            low_stock_threshold: data.lowStockThreshold ?? 5,
        });
    }

    async update(id, data) {
        return this._apiClient.put(`/api/v1/products/${id}`, {
            name: data.name,
            price: data.price,
            stock_level: data.stockLevel,
            low_stock_threshold: data.lowStockThreshold ?? 5,
        });
    }

    async delete(id) {
        return this._apiClient.delete(`/api/v1/products/${id}`);
    }
}
module.exports = ProductApiService;
