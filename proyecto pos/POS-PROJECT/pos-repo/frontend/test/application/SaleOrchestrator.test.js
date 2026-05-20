'use strict';

const sinon = require('sinon');
const SaleOrchestrator = require('../../src/application/SaleOrchestrator');

describe('SaleOrchestrator', () => {
    let saleService;
    let paymentService;
    let orchestrator;

    beforeEach(() => {
        saleService = {
            createSale: sinon.stub(),
            addItem: sinon.stub(),
            confirmSale: sinon.stub(),
            getSale: sinon.stub(),
        };
        paymentService = {
            processPayment: sinon.stub(),
        };
        orchestrator = new SaleOrchestrator(saleService, paymentService);
    });

    afterEach(() => {
        sinon.restore();
    });

    describe('checkout()', () => {
        it('should call confirmSale() before processPayment()', async () => {
            const saleId = 'sale-123';
            const method = 'CASH';
            const amount = 50.00;
            const callOrder = [];

            saleService.confirmSale.callsFake(async () => {
                callOrder.push('confirmSale');
                return { id: saleId, status: 'CONFIRMED' };
            });
            paymentService.processPayment.callsFake(async () => {
                callOrder.push('processPayment');
                return { id: 'pay-1', change: 10.00 };
            });

            await orchestrator.checkout(saleId, method, amount);

            expect(callOrder[0]).toBe('confirmSale');
            expect(callOrder[1]).toBe('processPayment');
            expect(saleService.confirmSale.calledOnce).toBe(true);
            expect(paymentService.processPayment.calledOnce).toBe(true);
        });

        it('should NOT call processPayment() if confirmSale() throws', async () => {
            const saleId = 'sale-456';
            saleService.confirmSale.rejects(new Error('Sale cannot be confirmed'));

            await expect(orchestrator.checkout(saleId, 'CASH', 100)).rejects.toThrow('Sale cannot be confirmed');
            expect(paymentService.processPayment.called).toBe(false);
        });

        it('should pass correct arguments to processPayment()', async () => {
            const saleId = 'sale-789';
            const method = 'CREDIT_CARD';
            const amount = 75.50;

            saleService.confirmSale.resolves({ id: saleId, status: 'CONFIRMED' });
            paymentService.processPayment.resolves({ id: 'pay-2', change: 0 });

            await orchestrator.checkout(saleId, method, amount);

            const callArgs = paymentService.processPayment.firstCall.args[0];
            expect(callArgs.saleId).toBe(saleId);
            expect(callArgs.method).toBe(method);
            expect(callArgs.amount).toBe(amount);
        });
    });

    describe('startSale()', () => {
        it('should delegate to saleService.createSale()', async () => {
            const mockSale = { id: 'sale-new', status: 'OPEN', items: [], total: 0 };
            saleService.createSale.resolves(mockSale);

            const result = await orchestrator.startSale();

            expect(saleService.createSale.calledOnce).toBe(true);
            expect(result).toEqual(mockSale);
        });
    });

    describe('addProduct()', () => {
        it('should delegate to saleService.addItem() with correct args', async () => {
            const saleId = 'sale-123';
            const productId = 'prod-abc';
            const quantity = 3;
            const mockSale = { id: saleId, items: [{ productId, quantity }], total: 9.00 };
            saleService.addItem.resolves(mockSale);

            const result = await orchestrator.addProduct(saleId, productId, quantity);

            expect(saleService.addItem.calledOnce).toBe(true);
            const args = saleService.addItem.firstCall.args;
            expect(args[0]).toBe(saleId);
            expect(args[1]).toEqual({ productId, quantity });
            expect(result).toEqual(mockSale);
        });
    });
});
