const express = require('express');

module.exports = (saleOrchestrator) => {
    const router = express.Router();

    router.get('/', async (req, res) => {
        res.render('sale/sale', { sale: null, error: null });
    });

    router.post('/start', async (req, res) => {
        try {
            const sale = await saleOrchestrator.startSale();
            res.json(sale);
        } catch (err) {
            res.status(400).json({ error: err.message });
        }
    });

    router.post('/:id/item', async (req, res) => {
        try {
            const { productId, quantity } = req.body;
            const sale = await saleOrchestrator.addProduct(req.params.id, productId, quantity);
            res.json(sale);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    router.post('/:id/pay', async (req, res) => {
        try {
            const { method, amount } = req.body;
            const payment = await saleOrchestrator.checkout(req.params.id, method, amount);
            res.json(payment);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    return router;
};
