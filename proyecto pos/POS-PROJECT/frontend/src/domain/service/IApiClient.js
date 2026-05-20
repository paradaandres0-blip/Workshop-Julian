class IApiClient {
    /** @param {string} path @returns {Promise<any>} */
    async get(path) { throw new Error('Not implemented'); }
    /** @param {string} path @param {object} body @returns {Promise<any>} */
    async post(path, body) { throw new Error('Not implemented'); }
    /** @param {string} path @param {object} body @returns {Promise<any>} */
    async put(path, body) { throw new Error('Not implemented'); }
    /** @param {string} path @returns {Promise<void>} */
    async delete(path) { throw new Error('Not implemented'); }
}
module.exports = IApiClient;
