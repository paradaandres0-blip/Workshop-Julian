const express = require('express');

module.exports = (reportOrchestrator) => {
    const router = express.Router();

    router.get('/', async (req, res) => {
        const { from, to } = req.query;
        try {
            const [salesReport, topProducts, inventoryReport] = await Promise.all([
                from && to ? reportOrchestrator.getSalesReport(from, to) : null,
                from && to ? reportOrchestrator.getTopProducts(from, to) : null,
                reportOrchestrator.getInventoryReport(),
            ]);
            res.render('reports/reports', {
                salesReport,
                topProducts,
                inventoryReport,
                from: from || '',
                to: to || '',
                error: null,
            });
        } catch (err) {
            res.render('reports/reports', {
                salesReport: null,
                topProducts: null,
                inventoryReport: null,
                from: from || '',
                to: to || '',
                error: err.message,
            });
        }
    });

    return router;
};
