class SaleOrchestrator {
    /**
     * @param {import('../domain/service/ISaleService')} saleService
     * @param {import('../domain/service/IPaymentService')} paymentService
     * @param {import('../domain/service/IProductService')} productService
     */
    constructor(saleService, paymentService, productService) {
        this._saleService = saleService;
        this._paymentService = paymentService;
        this._productService = productService;
    }

    async startSale() {
        return this._saleService.createSale();
    }

    async searchProducts(query) {
        return this._productService.search(query);
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
