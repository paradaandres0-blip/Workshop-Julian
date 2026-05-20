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
