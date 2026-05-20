class IProductService {
    /** @returns {Promise<import('../model/product').Product[]>} */
    async getAll() { throw new Error('Not implemented'); }
    /** @param {string} id @returns {Promise<import('../model/product').Product>} */
    async getById(id) { throw new Error('Not implemented'); }
    /** @param {object} data @returns {Promise<import('../model/product').Product>} */
    async create(data) { throw new Error('Not implemented'); }
    /** @param {string} id @param {object} data @returns {Promise<import('../model/product').Product>} */
    async update(id, data) { throw new Error('Not implemented'); }
    /** @param {string} id @returns {Promise<void>} */
    async delete(id) { throw new Error('Not implemented'); }
}
module.exports = IProductService;
