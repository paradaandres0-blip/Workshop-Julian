const express = require('express');

module.exports = (saleOrchestrator) => {
    const router = express.Router();

    router.get('/', async (req, res) => {
        res.render('sale/sale', { sale: null, error: null });
    });

    router.get('/search-products', async (req, res) => {
        try {
            const { q } = req.query;
            if (!q || q.trim().length === 0) return res.json([]);
            const results = await saleOrchestrator.searchProducts(q);
            res.json(results);
        } catch (err) {
            res.json([]);
        }
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
            const productId = req.body.product_id || req.body.productId;
            const quantity = req.body.quantity;
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
