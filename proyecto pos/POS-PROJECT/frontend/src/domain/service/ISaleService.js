class ISaleService {
    /** @returns {Promise<import('../model/sale').Sale>} */
    async createSale() { throw new Error('Not implemented'); }
    /** @param {string} saleId @param {{productId: string, quantity: number}} cmd */
    async addItem(saleId, cmd) { throw new Error('Not implemented'); }
    /** @param {string} saleId @returns {Promise<import('../model/sale').Sale>} */
    async confirmSale(saleId) { throw new Error('Not implemented'); }
    /** @param {string} saleId @returns {Promise<import('../model/sale').Sale>} */
    async getSale(saleId) { throw new Error('Not implemented'); }
}
module.exports = ISaleService;
