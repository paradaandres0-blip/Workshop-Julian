const IPaymentService = require('../../domain/service/IPaymentService');

class PaymentApiService extends IPaymentService {
    /** @param {import('../../domain/service/IApiClient')} apiClient */
    constructor(apiClient) {
        super();
        this._apiClient = apiClient;
    }

    async processPayment(cmd) {
        return this._apiClient.post('/api/v1/payments', {
            sale_id: cmd.saleId,
            method: cmd.method,
            amount: cmd.amount,
        });
    }
}
module.exports = PaymentApiService;
