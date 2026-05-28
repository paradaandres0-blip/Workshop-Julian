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
 * Estructura DynamoDB Productos — columna "producto" guardada como JSON string plano:
 * {
 *   "id":      "uuid",
 *   "code":    "100006",
 *   "producto": "{\"name\":\"Red Bull 250ml\",\"price\":3500,\"stock_level\":40,\"low_stock_threshold\":5}"
 * }
 *
 * Esto hace que la consola de AWS muestre el JSON legible directamente en la columna "producto".
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
        this.tableName    = System.getenv("PRODUCTOS_TABLE");
        this.objectMapper = new ObjectMapper();
    }

    GetProductsHandler(AmazonDynamoDB dynamoClient, String tableName) {
        this.dynamoClient = dynamoClient;
        this.tableName    = tableName;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent input, Context context) {
        String method     = input.getHttpMethod();
        String path       = input.getPath();
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
            // Search by code field (exact match)
            ScanResult sr = dynamoClient.scan(new ScanRequest()
                    .withTableName(tableName)
                    .withFilterExpression("#c = :q")
                    .withExpressionAttributeNames(Map.of("#c", "code"))
                    .withExpressionAttributeValues(Map.of(":q", new AttributeValue(q))));
            for (Map<String, AttributeValue> item : sr.getItems()) products.add(toProduct(item));

            if (products.isEmpty()) {
                return response(404, "{\"error\":\"Código incorrecto, vuelve a intentarlo\"}");
            }
        } else {
            // Text search: scan all and filter in-Lambda (case-insensitive match on name)
            // DynamoDB contains() is case-sensitive, so we do the filtering here.
            String qLower = q.toLowerCase();
            ScanResult sr = dynamoClient.scan(new ScanRequest().withTableName(tableName));
            for (Map<String, AttributeValue> item : sr.getItems()) {
                Map<String, Object> producto = readProducto(item);
                String name = str(producto.get("name")).toLowerCase();
                if (name.contains(qLower)) {
                    products.add(toProduct(item));
                }
            }
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

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleCreate(APIGatewayProxyRequestEvent input, Context context) throws Exception {
        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"El cuerpo es requerido\"}");

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

        String id        = UUID.randomUUID().toString();
        double price     = toDouble(req.get("price"));
        int stockLevel   = toInt(req.get("stock_level"));
        int lowThreshold = req.containsKey("low_stock_threshold") ? toInt(req.get("low_stock_threshold")) : 5;

        // Build producto as plain JSON object
        Map<String, Object> productoObj = new LinkedHashMap<>();
        productoObj.put("name",                name);
        productoObj.put("price",               price);
        productoObj.put("stock_level",         stockLevel);
        productoObj.put("low_stock_threshold", lowThreshold);

        Map<String, AttributeValue> item = new HashMap<>();
        item.put("id",       new AttributeValue(id));
        item.put("code",     new AttributeValue(code));
        item.put("producto", new AttributeValue(objectMapper.writeValueAsString(productoObj)));

        dynamoClient.putItem(new PutItemRequest().withTableName(tableName).withItem(item));
        return response(201, objectMapper.writeValueAsString(toProduct(item)));
    }

    // ── PUT update ───────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private APIGatewayProxyResponseEvent handleUpdate(String id, APIGatewayProxyRequestEvent input, Context context) throws Exception {
        GetItemResult existing = dynamoClient.getItem(new GetItemRequest()
                .withTableName(tableName)
                .withKey(Map.of("id", new AttributeValue(id))));
        if (existing.getItem() == null || existing.getItem().isEmpty()) {
            return response(404, "{\"error\":\"Producto no encontrado\"}");
        }

        String body = input.getBody();
        if (body == null || body.isBlank()) return response(400, "{\"error\":\"El cuerpo es requerido\"}");

        Map<String, Object> req = objectMapper.readValue(body, Map.class);

        // Read existing producto
        Map<String, Object> productoObj = readProducto(existing.getItem());

        if (req.containsKey("name"))                productoObj.put("name",                str(req.get("name")));
        if (req.containsKey("price"))               productoObj.put("price",               toDouble(req.get("price")));
        if (req.containsKey("stock_level"))         productoObj.put("stock_level",         toInt(req.get("stock_level")));
        if (req.containsKey("low_stock_threshold")) productoObj.put("low_stock_threshold", toInt(req.get("low_stock_threshold")));

        Map<String, AttributeValue> item = new HashMap<>(existing.getItem());
        item.put("producto", new AttributeValue(objectMapper.writeValueAsString(productoObj)));
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
     * Reads the "producto" column from a DynamoDB item.
     * Supports two storage formats:
     *   1. JSON string (new format): producto stored as plain JSON string → parse directly.
     *   2. Native DynamoDB Map (boto3.resource inserts this way) → convert to plain map.
     */
    @SuppressWarnings("unchecked")
    Map<String, Object> readProducto(Map<String, AttributeValue> item) {
        AttributeValue av = item.get("producto");
        if (av == null) return new LinkedHashMap<>();

        // Format 1: JSON string (preferred — shows as plain JSON in AWS console)
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

    /**
     * Converts a DynamoDB item to a flat product map for the API response.
     */
    Map<String, Object> toProduct(Map<String, AttributeValue> item) {
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
