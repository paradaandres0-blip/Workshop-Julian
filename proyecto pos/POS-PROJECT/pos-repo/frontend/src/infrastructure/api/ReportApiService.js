const IReportService = require('../../domain/service/IReportService');

class ReportApiService extends IReportService {
    /** @param {import('../../domain/service/IApiClient')} apiClient */
    constructor(apiClient) {
        super();
        this._apiClient = apiClient;
    }

    async getSalesReport(from, to) {
        return this._apiClient.get(`/api/v1/reports/sales?from=${from}&to=${to}`);
    }

    async getTopProducts(from, to) {
        return this._apiClient.get(`/api/v1/reports/top-products?from=${from}&to=${to}`);
    }

    async getInventoryReport() {
        return this._apiClient.get('/api/v1/reports/inventory');
    }
}
module.exports = ReportApiService;
