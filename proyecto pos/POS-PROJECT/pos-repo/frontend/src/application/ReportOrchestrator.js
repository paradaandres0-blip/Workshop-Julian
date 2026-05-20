class ReportOrchestrator {
    /** @param {import('../domain/service/IReportService')} reportService */
    constructor(reportService) {
        this._reportService = reportService;
    }

    async getSalesReport(from, to) {
        return this._reportService.getSalesReport(from, to);
    }

    async getTopProducts(from, to) {
        return this._reportService.getTopProducts(from, to);
    }

    async getInventoryReport() {
        return this._reportService.getInventoryReport();
    }
}
module.exports = ReportOrchestrator;
