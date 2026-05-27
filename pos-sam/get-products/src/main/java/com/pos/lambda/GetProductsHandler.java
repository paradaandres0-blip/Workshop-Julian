package com.pos.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Estructura DynamoDB Productos:
 * {
 *   "id": "uuid",          ← Partition Key
 *   "code": "7702001001",
 *   "producto": {
 *     "name": "...",
 *     "price": 1.50,
 *     "stock_level": 100,
 *     "low_stock_threshold": 10
 *   }
 * }
 */
public class GetProductsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Map<String, String> JSON_HEADERS = Map.of(
            "Content-Type", "application/json",
            "Access-Control-Allow-Origin", "*"
    );

    private final AmazonDynamoDB dynamoClient;
    private final String tableName;
    private final ObjectMapper objectMapper;

    public GetProductsHandler() {
        this.dynamoClient = AmazonDynamoDBClientBuilder.standard().build();
        this.tableName = System.getenv("PRODUCTOS_TABLE");
        this.objectMapper = new ObjectMapper();
    }

    GetProductsHandler(AmazonDynamoDB dynamoClient, String tableName) {
        this.dynamoClient = dynamoClient;
        this.tableName = tableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String method = input.getHttpMethod();
        String path   = input.getPath();
        Map<String, String> pathParams = input.getPathParameters();

        try {
            if ("GET".equals(method) && path != null && path.endsWith("/search")) {
                return handleSearch(input, context);
            }
            if (pathParams != null && pathParams.containsKey("id")) {
                String id = pathParams.get("id");
                if ("GET".equals(method))    return handleGetById(id, context);
                if ("PUT".equals(method))    return handleUpdate(id, input, context);
                if ("DELETE".equals(method)) return handleDelete(id, context);
            }
            if ("GET".equals(method))  return handleGetAll(context);
            if ("POST".equals(method)) return handleCreate(input, context);
            return response(405, "{\"error\":\"Method not allowed\"}");
        } catch (AmazonDynamoDBException e) {
            context.getLogger().log("DynamoDB error: " + e.getMessage());
            return response(500, "{\"error\":\"Error al acceder a la base de datos\"}");
        } catch (Exception e) {
            context.getLogger().log("Unexpected error: " + e.getMessage());
            return response(500, "{\"error\":\"Error interno del servidor\"}");
        }
    }

    // ── GET all ──────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleGetAll(Context context) throws Exception {
        ScanResult result = dynamoClient.scan(new ScanRequest().withTableName(tableName));
        List<Map<String, Object>> products = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            products.add(toProduct(item));
        }
        return response(200, objectMapper.writeValueAsString(products));
    }

    // ── GET search ───────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleSearch(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        Map<String, String> qp = input.getQueryStringParameters();
        String q = qp != null ? qp.get("q") : null;
        if (q == null || q.isBlank()) return response(200, "[]");

        List<Map<String, Object>> products = new ArrayList<>();

        if (BarcodeDetector.isBarcode(q)) {
            // Search by code field
            ScanResult sr = dynamoClient.scan(new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression("#c = :q")
                    .withExpressionAttributeNames(Map.of("#c", "code"))
                    .withExpressionAttributeValues(Map.of(":q", new AttributeValue(q))));
            for (Map<String, AttributeValue> item : sr.getItems()) products.add(toProduct(item));

            // If barcode search returns nothing → error message
            if (products.isEmpty()) {
                return response(404, "{\"error\":\"Código incorrecto, vuelve a intentarlo\"}");
            }
        } else {
            // Search by name inside producto map
            ScanResult sr = dynamoClient.scan(new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression("contains(producto.#n, :q)")
                    .withExpressionAttributeNames(Map.of("#n", "name"))
                    .withExpressionAttributeValues(Map.of(":q", new AttributeValue(q))));
            for (Map<String, AttributeValue> item : sr.getItems()) products.add(toProduct(item));
        }

        return response(200, objectMapper.writeValueAsString(products));
    }

    // ── GET by id ────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleGetById(String id, Context context) throws Exception {
        GetItemResult r = dynamoClient.getItem(new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("id", new AttributeValue(id))));
        if (r.getItem() == null || r.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Producto no encontrado\"}");
        }
        return response(200, objectMapper.writeValueAsString(toProduct(r.getItem())));
    }

    // ── POST create ──────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleCreate(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"El cuerpo es requerido\"}");

        @SuppressWarnings("unchecked")
        Map<String, Object> req = objectMapper.readValue(body, Map.class);

        String code = str(req.get("code"));
        String name = str(req.get("name"));
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            return response(400, "{\"error\":\"Los campos code y name son requeridos\"}");
        }

        // Check duplicate code
        ScanResult existing = dynamoClient.scan(new ScanRequest()
                .withTableName(tableName)
                .withFilterExpression("#c = :code")
                .withExpressionAttributeNames(Map.of("#c", "code"))
                .withExpressionAttributeValues(Map.of(":code", new AttributeValue(code))));
        if (!existing.getItems().isEmpty()) {
            return response(409, "{\"error_code\":\"DUPLICATE_PRODUCT\",\"message\":\"El código de producto ya existe\"}");
        }

        String id = UUID.randomUUID().toString();
        double price = toDouble(req.get("price"));
        int stockLevel = toInt(req.get("stock_level"));
        int lowThreshold = req.containsKey("low_stock_threshold") ? toInt(req.get("low_stock_threshold")) : 5;

        // Build producto map
        Map<String, AttributeValue> productoMap = new HashMap<>();
        productoMap.put("name",                new AttributeValue(name));
        productoMap.put("price",               new AttributeValue().withN(String.valueOf(price)));
        productoMap.put("stock_level",         new AttributeValue().withN(String.valueOf(stockLevel)));
        productoMap.put("low_stock_threshold", new AttributeValue().withN(String.valueOf(lowThreshold)));

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id",       new AttributeValue(id));
        item.put("code",     new AttributeValue(code));
        item.put("producto", new AttributeValue().withM(productoMap));

        dynamoClient.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
        return response(201, objectMapper.writeValueAsString(toProduct(item)));
    }

    // ── PUT update ───────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleUpdate(String id, APIGatewayProxyRequestEvent input, Context context) throws Exception {
        GetItemResult existing = dynamoClient.getItem(new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("id", new AttributeValue(id))));
        if (existing.getItem() == null || existing.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Producto no encontrado\"}");
        }

        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"El cuerpo es requerido\"}");

        @SuppressWarnings("unchecked")
        Map<String, Object> req = objectMapper.readValue(body, Map.class);

        Map<String, AttributeValue> item = new HashMap<>(existing.getItem());

        // Get existing producto map or create new
        Map<String, AttributeValue> productoMap = item.containsKey("producto") && item.get("producto").getM() != null
                ? new HashMap<>(item.get("producto").getM())
                : new HashMap<>();

        if (req.containsKey("name"))               productoMap.put("name",                new AttributeValue(str(req.get("name"))));
        if (req.containsKey("price"))              productoMap.put("price",               new AttributeValue().withN(String.valueOf(toDouble(req.get("price")))));
        if (req.containsKey("stock_level"))        productoMap.put("stock_level",         new AttributeValue().withN(String.valueOf(toInt(req.get("stock_level")))));
        if (req.containsKey("low_stock_threshold"))productoMap.put("low_stock_threshold", new AttributeValue().withN(String.valueOf(toInt(req.get("low_stock_threshold")))));

        item.put("producto", new AttributeValue().withM(productoMap));
        dynamoClient.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
        return response(200, objectMapper.writeValueAsString(toProduct(item)));
    }

    // ── DELETE ───────────────────────────────────────────────────────────────

    private APIGatewayProxyResponseEvent handleDelete(String id, Context context) throws Exception {
        GetItemResult existing = dynamoClient.getItem(new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("id", new AttributeValue(id))));
        if (existing.getItem() == null || existing.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Producto no encontrado\"}");
        }
        dynamoClient.deleteItem(new DeleteItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("id", new AttributeValue(id))));
        return response(204, "");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Converts DynamoDB item to flat product map for API response.
     * Reads name/price/stock from nested "producto" map.
     */
    Map<String, Object> toProduct(Map<String, AttributeValue> item) {
        Map<String, Object> p = new HashMap<>();
        String id = getS(item, "id");
        String code = getS(item, "code");

        // Read from nested "producto" map
        Map<String, AttributeValue> productoMap = item.containsKey("producto") && item.get("producto").getM() != null
                ? item.get("producto").getM()
                : item; // fallback for old format

        double price     = getN(productoMap, "price");
        int stockLevel   = (int) getN(productoMap, "stock_level");
        int lowThreshold = (int) getN(productoMap, "low_stock_threshold");
        if (lowThreshold == 0) lowThreshold = 5;
        String name = getS(productoMap, "name");

        p.put("id",                id);
        p.put("code",              code);
        p.put("name",              name);
        p.put("price",             price);
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

    private String str(Object o) { return o != null ? o.toString() : null; }
    private double toDouble(Object o) { try { return o != null ? Double.parseDouble(o.toString()) : 0; } catch (Exception e) { return 0; } }
    private int toInt(Object o) { try { return o != null ? (int) Double.parseDouble(o.toString()) : 0; } catch (Exception e) { return 0; } }

    private APIGatewayProxyResponseEvent response(int status, String body) {
        return new APIGatewayProxyResponseEvent().withStatusCode(status).withHeaders(JSON_HEADERS).withBody(body);
    }
}
