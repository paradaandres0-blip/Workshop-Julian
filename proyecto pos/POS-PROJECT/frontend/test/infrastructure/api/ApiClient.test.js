'use strict';

const ApiClient = require('../../../src/infrastructure/api/ApiClient');
const ApiError = require('../../../src/infrastructure/api/ApiError');

describe('ApiClient', () => {
    let client;
    let originalFetch;

    beforeEach(() => {
        originalFetch = global.fetch;
        client = new ApiClient('http://localhost:8080');
    });

    afterEach(() => {
        global.fetch = originalFetch;
    });

    function mockFetch(status, body) {
        global.fetch = jest.fn().mockResolvedValue({
            ok: status >= 200 && status < 300,
            status,
            json: jest.fn().mockResolvedValue(body),
        });
    }

    describe('PBT-01: HTTP errors throw ApiError with correct errorCode and message', () => {
        it('should throw ApiError with errorCode and message on 400 response', async () => {
            mockFetch(400, { error_code: 'INVALID_REQUEST', message: 'Bad request data', timestamp: '2024-01-01T00:00:00Z' });

            await expect(client.get('/api/v1/products')).rejects.toThrow(ApiError);
            try {
                await client.get('/api/v1/products');
            } catch (err) {
                expect(err).toBeInstanceOf(ApiError);
                expect(err.errorCode).toBe('INVALID_REQUEST');
                expect(err.message).toBe('Bad request data');
            }
        });

        it('should throw ApiError with errorCode and message on 404 response', async () => {
            mockFetch(404, { error_code: 'SALE_NOT_FOUND', message: 'Sale not found', timestamp: '2024-01-01T00:00:00Z' });

            try {
                await client.get('/api/v1/sales/nonexistent');
            } catch (err) {
                expect(err).toBeInstanceOf(ApiError);
                expect(err.errorCode).toBe('SALE_NOT_FOUND');
                expect(err.message).toBe('Sale not found');
                expect(err.name).toBe('ApiError');
            }
        });

        it('should throw ApiError with errorCode and message on 422 response', async () => {
            mockFetch(422, { error_code: 'INSUFFICIENT_STOCK', message: 'Not enough stock', timestamp: '2024-01-01T00:00:00Z' });

            try {
                await client.post('/api/v1/sales/1/items', { productId: 'p1', quantity: 100 });
            } catch (err) {
                expect(err).toBeInstanceOf(ApiError);
                expect(err.errorCode).toBe('INSUFFICIENT_STOCK');
                expect(err.message).toBe('Not enough stock');
            }
        });

        it('should throw ApiError on 409 conflict', async () => {
            mockFetch(409, { error_code: 'DUPLICATE_PRODUCT', message: 'Product code already exists', timestamp: '2024-01-01T00:00:00Z' });

            try {
                await client.post('/api/v1/products', { code: 'PROD-001' });
            } catch (err) {
                expect(err).toBeInstanceOf(ApiError);
                expect(err.errorCode).toBe('DUPLICATE_PRODUCT');
                expect(err.message).toBe('Product code already exists');
            }
        });
    });

    describe('204 response returns null', () => {
        it('should return null for 204 No Content', async () => {
            global.fetch = jest.fn().mockResolvedValue({
                ok: true,
                status: 204,
                json: jest.fn(),
            });

            const result = await client.delete('/api/v1/products/123');
            expect(result).toBeNull();
        });
    });

    describe('200 response returns parsed JSON', () => {
        it('should return parsed JSON for 200 response', async () => {
            const mockData = { id: 'prod-1', code: 'PROD-001', name: 'Coffee', price: 2.50 };
            mockFetch(200, mockData);

            const result = await client.get('/api/v1/products/prod-1');
            expect(result).toEqual(mockData);
        });

        it('should return parsed JSON array for 200 list response', async () => {
            const mockList = [{ id: 'p1' }, { id: 'p2' }];
            mockFetch(200, mockList);

            const result = await client.get('/api/v1/products');
            expect(result).toEqual(mockList);
        });

        it('should return parsed JSON for 201 created response', async () => {
            const mockProduct = { id: 'new-prod', code: 'PROD-002', name: 'Tea', price: 1.50 };
            mockFetch(201, mockProduct);

            const result = await client.post('/api/v1/products', { code: 'PROD-002', name: 'Tea', price: 1.50 });
            expect(result).toEqual(mockProduct);
        });
    });

    describe('HTTP method routing', () => {
        it('should use GET method for get()', async () => {
            mockFetch(200, {});
            await client.get('/api/v1/products');
            expect(global.fetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/products',
                expect.objectContaining({ method: 'GET' })
            );
        });

        it('should use POST method with body for post()', async () => {
            mockFetch(201, {});
            const body = { code: 'P1', name: 'Test' };
            await client.post('/api/v1/products', body);
            expect(global.fetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/products',
                expect.objectContaining({
                    method: 'POST',
                    body: JSON.stringify(body),
                })
            );
        });

        it('should use DELETE method for delete()', async () => {
            global.fetch = jest.fn().mockResolvedValue({ ok: true, status: 204, json: jest.fn() });
            await client.delete('/api/v1/products/1');
            expect(global.fetch).toHaveBeenCalledWith(
                'http://localhost:8080/api/v1/products/1',
                expect.objectContaining({ method: 'DELETE' })
            );
        });
    });
});
