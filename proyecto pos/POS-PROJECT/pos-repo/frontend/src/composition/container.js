require('dotenv').config();
const ApiClient = require('../infrastructure/api/ApiClient');
const ProductApiService = require('../infrastructure/api/ProductApiService');
const SaleApiService = require('../infrastructure/api/SaleApiService');
const PaymentApiService = require('../infrastructure/api/PaymentApiService');
const ReportApiService = require('../infrastructure/api/ReportApiService');
const SaleOrchestrator = require('../application/SaleOrchestrator');
const InventoryOrchestrator = require('../application/InventoryOrchestrator');
const ReportOrchestrator = require('../application/ReportOrchestrator');

const apiClient = new ApiClient(process.env.BACKEND_URL);

const productService = new ProductApiService(apiClient);
const saleService = new SaleApiService(apiClient);
const paymentService = new PaymentApiService(apiClient);
const reportService = new ReportApiService(apiClient);

const saleOrchestrator = new SaleOrchestrator(saleService, paymentService);
const inventoryOrchestrator = new InventoryOrchestrator(productService);
const reportOrchestrator = new ReportOrchestrator(reportService);

module.exports = { saleOrchestrator, inventoryOrchestrator, reportOrchestrator };
