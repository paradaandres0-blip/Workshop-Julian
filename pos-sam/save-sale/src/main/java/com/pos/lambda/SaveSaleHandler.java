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
 * Estructura DynamoDB Ventas — columna "detalle" guardada como JSON string plano:
 * {
 *   "id": "uuid",
 *   "detalle": "{\"status\":\"PAID\",\"total\":6.20,\"createdAt\":\"...\",\"paymentMethod\":\"CASH\",\"amountPaid\":10.00,\"change\":3.80,\"items\":[...]}"
 * }
 *
 * Esto hace que la consola de AWS muestre el JSON legible directamente en la columna "detalle".
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

        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("status",    "OPEN");
        detalle.put("createdAt", createdAt);
        detalle.put("total",     0);
        detalle.put("items",     new ArrayList<>());

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id",      new AttributeValue(saleId));
        item.put("detalle", new AttributeValue(objectMapper.writeValueAsString(detalle)));

        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(item));

        Map<String, Object> saleResponse = new LinkedHashMap<>();
        saleResponse.put("id",     saleId);
        saleResponse.put("status", "OPEN");
        saleResponse.put("total",  0);
        saleResponse.put("items",  new ArrayList<>());
        return response(201, objectMapper.writeValueAsString(saleResponse));
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

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleAddItem(String saleId, APIGatewayProxyRequestEvent input, Context context) throws Exception {
        GetItemResult saleResult = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (saleResult.getItem() == null || saleResult.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Venta no encontrada\"}");
        }

        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"Body requerido\"}");

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

        // Read existing detalle
        Map<String, Object> detalle = readDetalle(saleResult.getItem());
        List<Map<String, Object>> items = (List<Map<String, Object>>) detalle.getOrDefault("items", new ArrayList<>());

        // Check if product already in sale — update quantity
        boolean found = false;
        for (Map<String, Object> it : items) {
            if (str(it.get("productId")).equals(str(product.get("id")))) {
                int newQty = toInt(it.get("quantity")) + quantity;
                it.put("quantity", newQty);
                it.put("subtotal", price * newQty);
                found = true;
                break;
            }
        }
        if (!found) {
            Map<String, Object> newItem = new LinkedHashMap<>();
            newItem.put("productId",   str(product.get("id")));
            newItem.put("productName", str(product.get("name")));
            newItem.put("unitPrice",   price);
            newItem.put("quantity",    quantity);
            newItem.put("subtotal",    subtotal);
            items.add(newItem);
        }

        // Recalc total
        double total = items.stream().mapToDouble(it -> toDouble(it.get("subtotal"))).sum();
        detalle.put("items", items);
        detalle.put("total", total);

        // Save back as JSON string
        Map<String, AttributeValue> updatedSale = new HashMap<>();
        updatedSale.put("id",      new AttributeValue(saleId));
        updatedSale.put("detalle", new AttributeValue(objectMapper.writeValueAsString(detalle)));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        Map<String, Object> saleResponse = new LinkedHashMap<>();
        saleResponse.put("id",     saleId);
        saleResponse.put("status", detalle.get("status"));
        saleResponse.put("total",  total);
        saleResponse.put("items",  items);
        return response(200, objectMapper.writeValueAsString(saleResponse));
    }

    // ── Confirm sale ──────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleConfirm(String saleId, Context context) throws Exception {
        GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (r.getItem() == null || r.getItem().isEmpty()) return response(404, "{\"error\":\"Venta no encontrada\"}");

        Map<String, Object> detalle = readDetalle(r.getItem());
        detalle.put("status", "CONFIRMED");

        Map<String, AttributeValue> updatedSale = new HashMap<>();
        updatedSale.put("id",      new AttributeValue(saleId));
        updatedSale.put("detalle", new AttributeValue(objectMapper.writeValueAsString(detalle)));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        Map<String, Object> saleResponse = new LinkedHashMap<>();
        saleResponse.put("id",     saleId);
        saleResponse.put("status", "CONFIRMED");
        saleResponse.put("total",  detalle.get("total"));
        saleResponse.put("items",  detalle.getOrDefault("items", new ArrayList<>()));
        return response(200, objectMapper.writeValueAsString(saleResponse));
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handlePayment(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"Body requerido\"}");

        Map<String, Object> req = objectMapper.readValue(body, Map.class);
        String saleId = str(req.get("sale_id"));
        String method = str(req.get("method"));
        double amount = toDouble(req.get("amount"));

        if (saleId == null || saleId.isBlank()) return response(400, "{\"error\":\"sale_id es requerido\"}");

        GetItemResult saleResult = dynamoClient.getItem(new GetItemRequest()
                .withTableName(ventasTable)
                .withKey(Map.of("id", new AttributeValue(saleId))));
        if (saleResult.getItem() == null || saleResult.getItem().isEmpty()) return response(404, "{\"error\":\"Venta no encontrada\"}");

        Map<String, Object> detalle = readDetalle(saleResult.getItem());
        double total = toDouble(detalle.get("total"));

        if (amount < total) return response(422, "{\"error_code\":\"INVALID_PAYMENT\",\"message\":\"Monto insuficiente\"}");

        detalle.put("status",        "PAID");
        detalle.put("paymentMethod", method != null && !method.isBlank() ? method : "CASH");
        detalle.put("amountPaid",    amount);
        detalle.put("change",        amount - total);

        Map<String, AttributeValue> updatedSale = new HashMap<>();
        updatedSale.put("id",      new AttributeValue(saleId));
        updatedSale.put("detalle", new AttributeValue(objectMapper.writeValueAsString(detalle)));
        dynamoClient.putItem(new PutItemRequest().withTableName(ventasTable).withItem(updatedSale));

        // Decrease stock for each item
        List<Map<String, Object>> items = (List<Map<String, Object>>) detalle.getOrDefault("items", new ArrayList<>());
        for (Map<String, Object> it : items) {
            String productId = str(it.get("productId"));
            int qty          = toInt(it.get("quantity"));
            if (!productId.isBlank() && qty > 0) decreaseStock(productId, qty);
        }

        String paymentId = UUID.randomUUID().toString();
        Map<String, Object> paymentResponse = new LinkedHashMap<>();
        paymentResponse.put("id",     paymentId);
        paymentResponse.put("saleId", saleId);
        paymentResponse.put("method", method);
        paymentResponse.put("amount", amount);
        paymentResponse.put("change", amount - total);
        paymentResponse.put("status", "COMPLETED");
        return response(201, objectMapper.writeValueAsString(paymentResponse));
    }

    // ── Reports ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleSalesReport(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, String> qp = input.getQueryStringParameters();
        String from = qp != null ? qp.get("from") : null;
        String to   = qp != null ? qp.get("to")   : null;

        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(ventasTable));

        double totalAmount = 0;
        int totalSales = 0;
        Map<String, Double> byMethod = new LinkedHashMap<>();
        byMethod.put("CASH", 0.0); byMethod.put("CREDIT_CARD", 0.0); byMethod.put("DEBIT_CARD", 0.0);

        for (Map<String, AttributeValue> item : result.getItems()) {
            Map<String, Object> d = readDetalle(item);
            if (!"PAID".equals(d.get("status"))) continue;
            String createdAt = str(d.get("createdAt"));
            if (from != null && to != null && createdAt.length() >= 10) {
                String date = createdAt.substring(0, 10);
                if (date.compareTo(from) < 0 || date.compareTo(to) > 0) continue;
            }
            totalSales++;
            double amt = toDouble(d.get("total"));
            totalAmount += amt;
            String pm = str(d.get("paymentMethod"));
            if (pm.isBlank()) pm = "CASH";
            byMethod.put(pm, byMethod.getOrDefault(pm, 0.0) + amt);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalSales",  totalSales);
        report.put("totalAmount", String.format("%.2f", totalAmount));
        report.put("byMethod",    byMethod);
        return response(200, objectMapper.writeValueAsString(report));
    }

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleTopProducts(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, String> qp = input.getQueryStringParameters();
        String from = qp != null ? qp.get("from") : null;
        String to   = qp != null ? qp.get("to")   : null;

        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(ventasTable));

        Map<String, Map<String, Object>> productTotals = new HashMap<>();
        for (Map<String, AttributeValue> sale : result.getItems()) {
            Map<String, Object> d = readDetalle(sale);
            if (!"PAID".equals(d.get("status"))) continue;
            String createdAt = str(d.get("createdAt"));
            if (from != null && to != null && createdAt.length() >= 10) {
                String date = createdAt.substring(0, 10);
                if (date.compareTo(from) < 0 || date.compareTo(to) > 0) continue;
            }
            List<Map<String, Object>> items = (List<Map<String, Object>>) d.getOrDefault("items", new ArrayList<>());
            for (Map<String, Object> it : items) {
                String pid   = str(it.get("productId"));
                String pname = str(it.get("productName"));
                int qty      = toInt(it.get("quantity"));
                if (pid.isBlank()) continue;
                Map<String, Object> entry = productTotals.computeIfAbsent(pid, k -> {
                    Map<String, Object> e = new LinkedHashMap<>();
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

    /**
     * Reads the "detalle" column from a DynamoDB item.
     * Supports two storage formats:
     *   1. JSON string (new format): detalle stored as a plain JSON string → parse directly.
     *   2. Native DynamoDB Map (legacy): detalle stored as AttributeValue Map → convert to plain map.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readDetalle(Map<String, AttributeValue> item) {
        AttributeValue av = item.get("detalle");
        if (av == null) return new LinkedHashMap<>();

        // Format 1: JSON string (preferred — shows as plain JSON in AWS console)
        if (av.getS() != null) {
            try {
                return objectMapper.readValue(av.getS(), Map.class);
            } catch (Exception ignored) {}
        }

        // Format 2: native DynamoDB Map (legacy records)
        if (av.getM() != null) {
            Map<String, Object> result = new LinkedHashMap<>();
            Map<String, AttributeValue> m = av.getM();
            result.put("status",        getS(m, "status"));
            result.put("createdAt",     getS(m, "createdAt"));
            result.put("total",         getN(m, "total"));
            result.put("paymentMethod", getS(m, "paymentMethod"));
            result.put("amountPaid",    getN(m, "amountPaid"));
            result.put("change",        getN(m, "change"));
            // Convert items list
            List<Map<String, Object>> items = new ArrayList<>();
            if (m.containsKey("items") && m.get("items").getL() != null) {
                for (AttributeValue itemAv : m.get("items").getL()) {
                    Map<String, AttributeValue> im = itemAv.getM();
                    if (im == null) continue;
                    Map<String, Object> it = new LinkedHashMap<>();
                    it.put("productId",   getS(im, "productId"));
                    it.put("productName", getS(im, "productName"));
                    it.put("unitPrice",   getN(im, "unitPrice"));
                    it.put("quantity",    (int) getN(im, "quantity"));
                    it.put("subtotal",    getN(im, "subtotal"));
                    items.add(it);
                }
            }
            result.put("items", items);
            return result;
        }

        return new LinkedHashMap<>();
    }

    /**
     * Reads the "producto" column from a DynamoDB item (Productos table).
     * Supports JSON string format and native DynamoDB Map format.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> readProducto(Map<String, AttributeValue> item) {
        AttributeValue av = item.get("producto");
        if (av == null) return new LinkedHashMap<>();

        // Format 1: JSON string
        if (av.getS() != null) {
            try {
                return objectMapper.readValue(av.getS(), Map.class);
            } catch (Exception ignored) {}
        }

        // Format 2: native DynamoDB Map (boto3.resource inserts this way)
        if (av.getM() != null) {
            Map<String, AttributeValue> m = av.getM();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name",                getS(m, "name"));
            result.put("price",               getN(m, "price"));
            result.put("stock_level",         (int) getN(m, "stock_level"));
            result.put("low_stock_threshold", (int) getN(m, "low_stock_threshold"));
            return result;
        }

        return new LinkedHashMap<>();
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
            Map<String, Object> producto = readProducto(item);
            int current  = toInt(producto.get("stock_level"));
            int newStock = Math.max(0, current - qty);
            producto.put("stock_level", newStock);

            item.put("producto", new AttributeValue(objectMapper.writeValueAsString(producto)));
            dynamoClient.putItem(new PutItemRequest().withTableName(productosTable).withItem(item));
        } catch (Exception ignored) {}
    }

    private Map<String, Object> toSale(Map<String, AttributeValue> item) {
        Map<String, Object> detalle = readDetalle(item);
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id",     getS(item, "id"));
        s.put("status", detalle.getOrDefault("status", ""));
        s.put("total",  detalle.getOrDefault("total",  0));
        s.put("items",  detalle.getOrDefault("items",  new ArrayList<>()));
        return s;
    }

    private Map<String, Object> toProductView(Map<String, AttributeValue> item) {
        Map<String, Object> producto = readProducto(item);
        int stockLevel   = toInt(producto.get("stock_level"));
        int lowThreshold = toInt(producto.get("low_stock_threshold"));
        if (lowThreshold == 0) lowThreshold = 5;

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id",                getS(item, "id"));
        p.put("code",              getS(item, "code"));
        p.put("name",              str(producto.get("name")));
        p.put("price",             toDouble(producto.get("price")));
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
