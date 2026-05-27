package com.pos.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.*;

/**
 * Estructura DynamoDB Ventas:
 * {
 *   "id": "uuid",          ← Partition Key
 *   "detalle": {
 *     "status": "PAID",
 *     "total": 6.20,
 *     "createdAt": "...",
 *     "paymentMethod": "CASH",
 *     "amountPaid": 10.00,
 *     "change": 3.80,
 *     "items": [
 *       { "productId": "...", "productName": "...", "unitPrice": 2.20, "quantity": 1, "subtotal": 2.20 }
 *     ]
 *   }
 * }
 */
public class SaveSaleHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> JSON_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final AmazonDynamoDB dynamoClient;
    private final String ventasTable;
    private final String productosTable;
    private final ObjectMapper objectMapper;

    public SaveSaleHandler() {
        this.dynamoClient   = AmazonDynamoDBClientBuilder.standard().build();
        this.ventasTable    = System.getenv("VENTAS_TABLE");
        this.productosTable = System.getenv("PRODUCTOS_TABLE");
        this.objectMapper   = new ObjectMapper();
    }

    SaveSaleHandler(AmazonDynamoDB dynamoClient, String ventasTable, String productosTable) {
        this.dynamoClient   = dynamoClient;
        this.ventasTable    = ventasTable;
        this.productosTable = productosTable;
        this.objectMapper   = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String method = input.getHttpMethod();
        String path   = input.getPath() != null ? input.getPath() : "";
        Map<String, String> pathParams = input.getPathParameters();

        try {
            if (path.contains("/reports/sales"))        return handleSalesReport(input, context);
            if (path.contains("/reports/top-products")) return handleTopProducts(input, context);
            if (path.contains("/reports/inventory"))    return handleInventoryReport(context);
            if (path.contains("/payments") && "POST".equals(method)) return handlePayment(input, context);

            if (pathParams != null && pathParams.containsKey("id")) {
                String id = pathParams.get("id");
                if (path.endsWith("/items")   && "POST".equals(method)) return handleAddItem(id, input, context);
                if (path.endsWith("/confirm") && "POST".equals(method)) return handleConfirm(id, context);
                if ("GET".equals(method))                                return handleGetSale(id, context);
            }

            if ("POST".equals(method)) return handleCreateSale(context);
            return response(405, "{\"error\":\"Method not allowed\"}");

        } catch (AmazonDynamoDBException e) {
            context.getLogger().log("DynamoDB error: " + e.getMessage());
            return response(500, "{\"error\":\"Error al acceder a la base de datos\"}");
        } catch (Exception e) {
            context.getLogger().log("Unexpected error: " + e.getMessage());
            return response(500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    // ── Create sale ───────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleCreateSale(Context context) throws Exception {
        String saleId    = UUID.randomUUID().toString();
        String createdAt = Instant.now().toString();

        // Build detalle map
        Map<String, AttributeValue> detalleMap = new HashMap<>();
        detalleMap.put("status",    new AttributeValue("OPEN"));
        detalleMap.put("createdAt", new AttributeValue(createdAt));
        detalleMap.put("total",     new AttributeValue().withN("0"));
        detalleMap.put("items",     new AttributeValue().withL(new ArrayList<>()));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id",      new AttributeValue(saleId));
        item.put("detalle", new AttributeValue().withM(detalleMap));

        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(item));
        return response(201, objectMapper.writeValueAsString(toSale(item)));
    }

    // ── Get sale ──────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleGetSale(String id, Context context) throws Exception {
        GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(id))));
        if (r.getItem() == null || r.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Venta no encontrada\"}");
        }
        return response(200, objectMapper.writeValueAsString(toSale(r.getItem())));
    }

    // ── Add item ──────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleAddItem(String saleId, APIGatewayProxyRequestEvent input, Context context) throws Exception {
        GetItemResult saleResult = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (saleResult.getItem() == null || saleResult.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Venta no encontrada\"}");
        }

        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"Body requerido\"}");

        @SuppressWarnings("unchecked")
        Map<String, Object> req = objectMapper.readValue(body, Map.class);
        String productRef = str(req.get("product_id"));
        int quantity      = toInt(req.get("quantity"));
        if (quantity <= 0) quantity = 1;

        Map<String, Object> product = findProduct(productRef);
        if (product == null) return response(422, "{\"error_code\":\"PRODUCT_NOT_FOUND\",\"message\":\"Producto no encontrado\"}");

        int stockLevel = toInt(product.get("stockLevel"));
        if (stockLevel == 0) return response(422, "{\"error_code\":\"INSUFFICIENT_STOCK\",\"message\":\"Stock insuficiente\"}");

        double price    = toDouble(product.get("price"));
        double subtotal = price * quantity;

        Map<String, AttributeValue> itemMap = new HashMap<>();
        itemMap.put("productId",   new AttributeValue(str(product.get("id"))));
        itemMap.put("productName", new AttributeValue(str(product.get("name"))));
        itemMap.put("unitPrice",   new AttributeValue().withN(String.valueOf(price)));
        itemMap.put("quantity",    new AttributeValue().withN(String.valueOf(quantity)));
        itemMap.put("subtotal",    new AttributeValue().withN(String.valueOf(subtotal)));

        // Get detalle map
        Map<String, AttributeValue> saleItem = saleResult.getItem();
        Map<String, AttributeValue> detalleMap = saleItem.containsKey("detalle") && saleItem.get("detalle").getM() != null
                ? new HashMap<>(saleItem.get("detalle").getM())
                : new HashMap<>();

        List<AttributeValue> existingItems = detalleMap.containsKey("items") && detalleMap.get("items").getL() != null
                ? new ArrayList<>(detalleMap.get("items").getL())
                : new ArrayList<>();

        // Check if product already in sale
        boolean found = false;
        for (AttributeValue av : existingItems) {
            Map<String, AttributeValue> m = av.getM();
            if (m != null && str(m.get("productId") != null ? m.get("productId").getS() : null).equals(str(product.get("id")))) {
                int newQty = toInt(m.get("quantity") != null ? m.get("quantity").getN() : "0") + quantity;
                m.put("quantity", new AttributeValue().withN(String.valueOf(newQty)));
                m.put("subtotal", new AttributeValue().withN(String.valueOf(price * newQty)));
                found = true;
                break;
            }
        }
        if (!found) existingItems.add(new AttributeValue().withM(itemMap));

        // Recalc total
        double total = 0;
        for (AttributeValue av : existingItems) {
            Map<String, AttributeValue> m = av.getM();
            if (m != null && m.get("subtotal") != null) total += toDouble(m.get("subtotal").getN());
        }

        detalleMap.put("items", new AttributeValue().withL(existingItems));
        detalleMap.put("total", new AttributeValue().withN(String.valueOf(total)));

        Map<String, AttributeValue> updatedSale = new HashMap<>(saleItem);
        updatedSale.put("detalle", new AttributeValue().withM(detalleMap));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        return response(200, objectMapper.writeValueAsString(toSale(updatedSale)));
    }

    // ── Confirm sale ──────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleConfirm(String saleId, Context context) throws Exception {
        GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (r.getItem() == null || r.getItem().isEmpty()) return response(404, "{\"error\":\"Venta no encontrada\"}");

        Map<String, AttributeValue> saleItem = r.getItem();
        Map<String, AttributeValue> detalleMap = saleItem.containsKey("detalle") && saleItem.get("detalle").getM() != null
                ? new HashMap<>(saleItem.get("detalle").getM()) : new HashMap<>();
        detalleMap.put("status", new AttributeValue("CONFIRMED"));

        Map<String, AttributeValue> updatedSale = new HashMap<>(saleItem);
        updatedSale.put("detalle", new AttributeValue().withM(detalleMap));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        return response(200, objectMapper.writeValueAsString(toSale(updatedSale)));
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handlePayment(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"Body requerido\"}");

        @SuppressWarnings("unchecked")
        Map<String, Object> req = objectMapper.readValue(body, Map.class);
        String saleId  = str(req.get("sale_id"));
        String method  = str(req.get("method"));
        double amount  = toDouble(req.get("amount"));

        if (saleId == null || saleId.isBlank()) return response(400, "{\"error\":\"sale_id es requerido\"}");

        GetItemResult saleResult = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (saleResult.getItem() == null || saleResult.getItem().isEmpty()) return response(404, "{\"error\":\"Venta no encontrada\"}");

        Map<String, AttributeValue> saleItem = saleResult.getItem();
        Map<String, AttributeValue> detalleMap = saleItem.containsKey("detalle") && saleItem.get("detalle").getM() != null
                ? new HashMap<>(saleItem.get("detalle").getM()) : new HashMap<>();

        double total = detalleMap.containsKey("total") && detalleMap.get("total").getN() != null
                ? toDouble(detalleMap.get("total").getN()) : 0;

        if (amount < total) return response(422, "{\"error_code\":\"INVALID_PAYMENT\",\"message\":\"Monto insuficiente\"}");

        detalleMap.put("status",        new AttributeValue("PAID"));
        detalleMap.put("paymentMethod", new AttributeValue(method != null ? method : "CASH"));
        detalleMap.put("amountPaid",    new AttributeValue().withN(String.valueOf(amount)));
        detalleMap.put("change",        new AttributeValue().withN(String.valueOf(amount - total)));

        Map<String, AttributeValue> updatedSale = new HashMap<>(saleItem);
        updatedSale.put("detalle", new AttributeValue().withM(detalleMap));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        // Decrease stock
        List<AttributeValue> items = detalleMap.containsKey("items") && detalleMap.get("items").getL() != null
                ? detalleMap.get("items").getL() : new ArrayList<>();
        for (AttributeValue av : items) {
            Map<String, AttributeValue> m = av.getM();
            if (m == null) continue;
            String productId = m.get("productId") != null ? m.get("productId").getS() : null;
            int qty = (int) toDouble(m.get("quantity") != null ? m.get("quantity").getN() : "0");
            if (productId != null && qty > 0) decreaseStock(productId, qty);
        }

        String paymentId = UUID.randomUUID().toString();
        Map<String, Object> paymentResponse = new HashMap<>();
        paymentResponse.put("id",     paymentId);
        paymentResponse.put("saleId", saleId);
        paymentResponse.put("method", method);
        paymentResponse.put("amount", amount);
        paymentResponse.put("change", amount - total);
        paymentResponse.put("status", "COMPLETED");

        return response(201, objectMapper.writeValueAsString(paymentResponse));
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleSalesReport(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, String> qp = input.getQueryStringParameters();
        String from = qp != null ? qp.get("from") : null;
        String to   = qp != null ? qp.get("to")   : null;

        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(ventasTable)
                .withFilterExpression("detalle.#s = :paid")
                .withExpressionAttributeNames(Map.of("#s", "status"))
                .withExpressionAttributeValues(Map.of(":paid", new AttributeValue("PAID"))));

        double totalAmount = 0;
        int totalSales = 0;
        Map<String, Double> byMethod = new HashMap<>();
        byMethod.put("CASH", 0.0); byMethod.put("CREDIT_CARD", 0.0); byMethod.put("DEBIT_CARD", 0.0);

        for (Map<String, AttributeValue> item : result.getItems()) {
            Map<String, AttributeValue> d = getDetalleMap(item);
            String createdAt = getS(d, "createdAt");
            if (from != null && to != null && !createdAt.isEmpty()) {
                String date = createdAt.substring(0, 10);
                if (date.compareTo(from) < 0 || date.compareTo(to) > 0) continue;
            }
            totalSales++;
            double amt = getN(d, "total");
            totalAmount += amt;
            String pm = getS(d, "paymentMethod");
            if (pm.isEmpty()) pm = "CASH";
            byMethod.put(pm, byMethod.getOrDefault(pm, 0.0) + amt);
        }

        Map<String, Object> report = new HashMap<>();
        report.put("totalSales",  totalSales);
        report.put("totalAmount", String.format("%.2f", totalAmount));
        report.put("byMethod",    byMethod);
        return response(200, objectMapper.writeValueAsString(report));
    }

    private APIGatewayProxyResponseEvent handleTopProducts(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, String> qp = input.getQueryStringParameters();
        String from = qp != null ? qp.get("from") : null;
        String to   = qp != null ? qp.get("to")   : null;

        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(ventasTable)
                .withFilterExpression("detalle.#s = :paid")
                .withExpressionAttributeNames(Map.of("#s", "status"))
                .withExpressionAttributeValues(Map.of(":paid", new AttributeValue("PAID"))));

        Map<String, Map<String, Object>> productTotals = new HashMap<>();
        for (Map<String, AttributeValue> sale : result.getItems()) {
            Map<String, AttributeValue> d = getDetalleMap(sale);
            String createdAt = getS(d, "createdAt");
            if (from != null && to != null && !createdAt.isEmpty()) {
                String date = createdAt.substring(0, 10);
                if (date.compareTo(from) < 0 || date.compareTo(to) > 0) continue;
            }
            List<AttributeValue> items = d.containsKey("items") && d.get("items").getL() != null
                    ? d.get("items").getL() : new ArrayList<>();
            for (AttributeValue av : items) {
                Map<String, AttributeValue> m = av.getM();
                if (m == null) continue;
                String pid   = m.get("productId")   != null ? m.get("productId").getS()   : "";
                String pname = m.get("productName") != null ? m.get("productName").getS() : "";
                int qty      = (int) toDouble(m.get("quantity") != null ? m.get("quantity").getN() : "0");
                if (pid.isEmpty()) continue;
                Map<String, Object> entry = productTotals.computeIfAbsent(pid, k -> {
                    Map<String, Object> e = new HashMap<>();
                    e.put("productCode", pid); e.put("productName", pname); e.put("totalQuantity", 0);
                    return e;
                });
                entry.put("totalQuantity", toInt(entry.get("totalQuantity")) + qty);
            }
        }

        List<Map<String, Object>> top = new ArrayList<>(productTotals.values());
        top.sort((a, b) -> toInt(b.get("totalQuantity")) - toInt(a.get("totalQuantity")));
        if (top.size() > 10) top = top.subList(0, 10);
        return response(200, objectMapper.writeValueAsString(top));
    }

    private APIGatewayProxyResponseEvent handleInventoryReport(Context context) throws Exception {
        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(productosTable));
        List<Map<String, Object>> products = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            products.add(toProductView(item));
        }
        return response(200, objectMapper.writeValueAsString(products));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, AttributeValue> getDetalleMap(Map<String, AttributeValue> item) {
        return item.containsKey("detalle") && item.get("detalle").getM() != null
                ? item.get("detalle").getM() : new HashMap<>();
    }

    private Map<String, Object> findProduct(String productRef) {
        if (productRef == null || productRef.isBlank()) return null;
        // Try by id
        GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                .withTableName(productosTable)
                .withKey(Map.of("id", new AttributeValue(productRef))));
        if (r.getItem() != null && !r.getItem().isEmpty()) return toProductView(r.getItem());
        // Try by code
        ScanResult sr = dynamoClient.scan(new ScanRequest()
                .withTableName(productosTable)
                .withFilterExpression("#c = :ref")
                .withExpressionAttributeNames(Map.of("#c", "code"))
                .withExpressionAttributeValues(Map.of(":ref", new AttributeValue(productRef))));
        if (!sr.getItems().isEmpty()) return toProductView(sr.getItems().get(0));
        // Try by name
        ScanResult sr2 = dynamoClient.scan(new ScanRequest()
                .withTableName(productosTable)
                .withFilterExpression("contains(producto.#n, :ref)")
                .withExpressionAttributeNames(Map.of("#n", "name"))
                .withExpressionAttributeValues(Map.of(":ref", new AttributeValue(productRef))));
        if (!sr2.getItems().isEmpty()) return toProductView(sr2.getItems().get(0));
        return null;
    }

    private void decreaseStock(String productId, int qty) {
        try {
            GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                    .withTableName(productosTable)
                    .withKey(Map.of("id", new AttributeValue(productId))));
            if (r.getItem() == null || r.getItem().isEmpty()) return;
            Map<String, AttributeValue> item = new HashMap<>(r.getItem());
            Map<String, AttributeValue> productoMap = item.containsKey("producto") && item.get("producto").getM() != null
                    ? new HashMap<>(item.get("producto").getM()) : new HashMap<>();
            int current  = (int) getN(productoMap, "stock_level");
            int newStock = Math.max(0, current - qty);
            productoMap.put("stock_level", new AttributeValue().withN(String.valueOf(newStock)));
            item.put("producto", new AttributeValue().withM(productoMap));
            dynamoClient.putItem(new PutItemRequest().withTableName(productosTable).withItem(item));
        } catch (Exception ignored) {}
    }

    private Map<String, Object> toSale(Map<String, AttributeValue> item) {
        Map<String, Object> s = new HashMap<>();
        s.put("id", getS(item, "id"));
        Map<String, AttributeValue> d = getDetalleMap(item);
        s.put("status", getS(d, "status"));
        s.put("total",  getN(d, "total"));
        List<Map<String, Object>> items = new ArrayList<>();
        if (d.containsKey("items") && d.get("items").getL() != null) {
            for (AttributeValue av : d.get("items").getL()) {
                Map<String, AttributeValue> m = av.getM();
                if (m == null) continue;
                Map<String, Object> it = new HashMap<>();
                it.put("productId",   m.get("productId")   != null ? m.get("productId").getS()   : "");
                it.put("productName", m.get("productName") != null ? m.get("productName").getS() : "");
                it.put("unitPrice",   toDouble(m.get("unitPrice")  != null ? m.get("unitPrice").getN()  : "0"));
                it.put("quantity",    (int) toDouble(m.get("quantity") != null ? m.get("quantity").getN() : "0"));
                it.put("subtotal",    toDouble(m.get("subtotal")   != null ? m.get("subtotal").getN()   : "0"));
                items.add(it);
            }
        }
        s.put("items", items);
        return s;
    }

    private Map<String, Object> toProductView(Map<String, AttributeValue> item) {
        Map<String, Object> p = new HashMap<>();
        Map<String, AttributeValue> productoMap = item.containsKey("producto") && item.get("producto").getM() != null
                ? item.get("producto").getM() : item;
        int stockLevel   = (int) getN(productoMap, "stock_level");
        int lowThreshold = (int) getN(productoMap, "low_stock_threshold");
        if (lowThreshold == 0) lowThreshold = 5;
        p.put("id",                getS(item, "id"));
        p.put("code",              getS(item, "code"));
        p.put("name",              getS(productoMap, "name"));
        p.put("price",             getN(productoMap, "price"));
        p.put("stockLevel",        stockLevel);
        p.put("lowStockThreshold", lowThreshold);
        p.put("outOfStock",        stockLevel == 0);
        p.put("lowStock",          stockLevel > 0 && stockLevel <= lowThreshold);
        return p;
    }

    private String getS(Map<String, AttributeValue> item, String key) {
        AttributeValue av = item.get(key);
        return av != null && av.getS() != null ? av.getS() : "";
    }

    private double getN(Map<String, AttributeValue> item, String key) {
        AttributeValue av = item.get(key);
        if (av != null) {
            if (av.getN() != null) { try { return Double.parseDouble(av.getN()); } catch (Exception ignored) {} }
            if (av.getS() != null) { try { return Double.parseDouble(av.getS()); } catch (Exception ignored) {} }
        }
        return 0;
    }

    private String str(Object o) { return o != null ? o.toString() : ""; }
    private double toDouble(Object o) { try { return o != null ? Double.parseDouble(o.toString()) : 0; } catch (Exception e) { return 0; } }
    private int toInt(Object o) { try { return o != null ? (int) Double.parseDouble(o.toString()) : 0; } catch (Exception e) { return 0; } }

    private APIGatewayProxyResponseEvent response(int status, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(status).withHeaders(JSON_HEADERS).withBody(body);
    }
}
