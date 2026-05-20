class IPaymentService {
    /** @param {{saleId: string, method: string, amount: number}} cmd @returns {Promise<import('../model/payment').Payment>} */
    async processPayment(cmd) { throw new Error('Not implemented'); }
}
module.exports = IPaymentService;
