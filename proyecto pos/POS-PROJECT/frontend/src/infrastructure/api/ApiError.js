class ApiError extends Error {
    constructor(errorCode, message) {
        super(message);
        this.name = 'ApiError';
        this.errorCode = errorCode;
    }
}
module.exports = ApiError;
