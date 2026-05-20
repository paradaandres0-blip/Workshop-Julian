const ISaleService = require('../../domain/service/ISaleService');

class SaleApiService extends ISaleService {
    /** @param {import('../../domain/service/IApiClient')} apiClient */
    constructor(apiClient) {
        super();
        this._apiClient = apiClient;
    }

    async createSale() {
        return this._apiClient.post('/api/v1/sales', {});
    }

    async addItem(saleId, cmd) {
        return this._apiClient.post(`/api/v1/sales/${saleId}/items`, {
            product_id: cmd.productId,
            quantity: cmd.quantity,
        });
    }

    async confirmSale(saleId) {
        return this._apiClient.post(`/api/v1/sales/${saleId}/confirm`, {});
    }

    async getSale(saleId) {
        return this._apiClient.get(`/api/v1/sales/${saleId}`);
    }
}
module.exports = SaleApiService;
