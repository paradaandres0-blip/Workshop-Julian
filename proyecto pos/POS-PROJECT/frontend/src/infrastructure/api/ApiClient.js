const IApiClient = require('../../domain/service/IApiClient');
const ApiError = require('./ApiError');

class ApiClient extends IApiClient {
    /** @param {string} baseUrl */
    constructor(baseUrl) {
        super();
        this._baseUrl = baseUrl;
    }

    async get(path) { return this._request('GET', path); }
    async post(path, body) { return this._request('POST', path, body); }
    async put(path, body) { return this._request('PUT', path, body); }
    async delete(path) { return this._request('DELETE', path); }

    async _request(method, path, body) {
        const res = await fetch(`${this._baseUrl}${path}`, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: body ? JSON.stringify(body) : undefined,
        });
        if (!res.ok) {
            const err = await res.json();
            throw new ApiError(err.error_code, err.message);
        }
        return res.status === 204 ? null : res.json();
    }
}
module.exports = ApiClient;
