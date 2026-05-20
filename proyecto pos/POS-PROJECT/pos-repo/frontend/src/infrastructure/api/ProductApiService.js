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

    async getById(id) {
        return this._apiClient.get(`/api/v1/products/${id}`);
    }

    async create(data) {
        return this._apiClient.post('/api/v1/products', data);
    }

    async update(id, data) {
        return this._apiClient.put(`/api/v1/products/${id}`, data);
    }

    async delete(id) {
        return this._apiClient.delete(`/api/v1/products/${id}`);
    }
}
module.exports = ProductApiService;
