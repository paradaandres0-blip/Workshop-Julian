class IReportService {
    /** @param {string} from @param {string} to @returns {Promise<object>} */
    async getSalesReport(from, to) { throw new Error('Not implemented'); }
    /** @param {string} from @param {string} to @returns {Promise<object[]>} */
    async getTopProducts(from, to) { throw new Error('Not implemented'); }
    /** @returns {Promise<object[]>} */
    async getInventoryReport() { throw new Error('Not implemented'); }
}
module.exports = IReportService;
