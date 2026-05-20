const express = require('express');

module.exports = (inventoryOrchestrator) => {
    const router = express.Router();

    router.get('/', async (req, res) => {
        try {
            const products = await inventoryOrchestrator.listProducts();
            res.render('inventory/inventory', { products, error: null });
        } catch (err) {
            res.render('inventory/inventory', { products: [], error: err.message });
        }
    });

    router.post('/', async (req, res) => {
        try {
            const product = await inventoryOrchestrator.createProduct(req.body);
            res.json(product);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    router.put('/:id', async (req, res) => {
        try {
            const product = await inventoryOrchestrator.updateProduct(req.params.id, req.body);
            res.json(product);
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    router.delete('/:id', async (req, res) => {
        try {
            await inventoryOrchestrator.deleteProduct(req.params.id);
            res.status(204).send();
        } catch (err) {
            res.status(422).json({ error: err.message });
        }
    });

    return router;
};
