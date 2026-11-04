
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;

public class ApiServer {

        private static final Gson gson = new Gson();

        public static void main(String[] args) throws IOException {

                HttpServer server = HttpServer.create(
                                new InetSocketAddress(8080), 0);

                // Checking whether the API is working
                server.createContext("/api/health", exchange -> {

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        sendJson(exchange, 200,
                                        "{\"status\":\"running\",\"message\":\"ServeU API is working!\"}");
                });
                // Geting all available service categories
                server.createContext("/api/categories", exchange -> {

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Origin", "*");

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Methods", "GET, OPTIONS");

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        List<Map<String, Object>> categories = new ArrayList<>();

                        String sql = "SELECT category_id, category_name " +
                                        "FROM service_categories ORDER BY category_name";

                        try (Connection connection = DatabaseConnection.getConnection();
                                        PreparedStatement statement = connection.prepareStatement(sql);
                                        ResultSet result = statement.executeQuery()) {

                                while (result.next()) {

                                        Map<String, Object> category = new HashMap<>();

                                        category.put("category_id", result.getInt("category_id"));
                                        category.put("category_name",
                                                        result.getString("category_name"));

                                        categories.add(category);
                                }

                                sendJson(exchange, 200, gson.toJson(categories));

                        } catch (Exception e) {

                                e.printStackTrace();

                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load service categories.\"}");
                        }
                });

                // Customer registration API
                server.createContext("/api/register/customer", exchange -> {

                        // Allows the  frontend requests during development
                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Origin", "*");

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Methods", "POST, OPTIONS");

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Headers", "Content-Type");

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        try {

                                // Read JSON sent by the frontend
                                InputStream input = exchange.getRequestBody();

                                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

                                byte[] bytes = new byte[1024];
                                int length;

                                while ((length = input.read(bytes)) != -1) {
                                        buffer.write(bytes, 0, length);
                                }

                                String jsonData = new String(
                                                buffer.toByteArray(),
                                                StandardCharsets.UTF_8);

                                JsonObject data = JsonParser.parseString(jsonData)
                                                .getAsJsonObject();

                                // Extract customer details
                                String name = data.has("name") && !data.get("name").isJsonNull()
                                                ? data.get("name").getAsString()
                                                : null;

                                String email = data.has("email") && !data.get("email").isJsonNull()
                                                ? data.get("email").getAsString()
                                                : null;

                                String password = data.has("password") && !data.get("password").isJsonNull()
                                                ? data.get("password").getAsString()
                                                : null;

                                String phone = data.has("phone") && !data.get("phone").isJsonNull()
                                                ? data.get("phone").getAsString()
                                                : null;

                                // Call your existing backend method
                                boolean registered = userservices.registerCustomer(
                                                name, email, password, phone);

                                if (registered) {

                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Customer registered successfully!\"}");

                                } else {

                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Registration failed. Check your details or email.\"}");
                                }

                        } catch (Exception e) {

                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid registration request.\"}");
                        }
                });

                // Worker registration API
                server.createContext("/api/register/worker", exchange -> {

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Origin", "*");

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Methods", "POST, OPTIONS");

                        exchange.getResponseHeaders().set(
                                        "Access-Control-Allow-Headers", "Content-Type");

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        try {
                                InputStream input = exchange.getRequestBody();

                                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

                                byte[] bytes = new byte[1024];
                                int length;

                                while ((length = input.read(bytes)) != -1) {
                                        buffer.write(bytes, 0, length);
                                }

                                String jsonData = new String(
                                                buffer.toByteArray(),
                                                StandardCharsets.UTF_8);

                                JsonObject data = JsonParser.parseString(jsonData)
                                                .getAsJsonObject();

                                String name = data.get("name").getAsString();
                                String email = data.get("email").getAsString();
                                String password = data.get("password").getAsString();
                                String phone = data.get("phone").getAsString();
                                String serviceCategory = data.get("serviceCategory").getAsString();
                                int experienceYears = data.get("experienceYears").getAsInt();
                                String description = data.get("description").getAsString();

                                boolean registered = userservices.registerWorker(
                                                name,
                                                email,
                                                password,
                                                phone,
                                                serviceCategory,
                                                experienceYears,
                                                description);

                                if (registered) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Worker registered successfully! Verification pending.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Worker registration failed. Check your details.\"}");
                                }

                        } catch (Exception e) {
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid worker registration request.\"}");
                        }
                });
                // Login API
                server.createContext("/api/login", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                String email = getString(data, "email");
                                String password = getString(data, "password");
                                String role = getString(data, "role");

                                Map<String, Object> user = userservices.authenticateApiUser(email, password, role);

                                if (user == null) {
                                        sendJson(exchange, 401,
                                                        "{\"success\":false,\"message\":\"Invalid email, password, or role.\"}");
                                        return;
                                }

                                String token = UUID.randomUUID().toString();

                                int userId = (Integer) user.get("user_id");
                                String userRole = (String) user.get("role");

                                sessions.put(token, new ApiSession(userId, userRole));

                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("message", "Login successful!");
                                response.put("token", token);
                                response.put("userId", userId);
                                response.put("name", user.get("name"));
                                response.put("role", userRole);

                                sendJson(exchange, 200, gson.toJson(response));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid login request.\"}");
                        }
                });

                // logout API
                server.createContext("/api/logout", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                return;
                        }

                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        String authorization = exchange.getRequestHeaders()
                                        .getFirst("Authorization");

                        if (authorization == null || !authorization.startsWith("Bearer ")) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Not logged in.\"}");
                                return;
                        }

                        String token = authorization.substring(7).trim();

                        if (token.isEmpty() || sessions.remove(token) == null) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Invalid or expired session.\"}");
                                return;
                        }

                        sendJson(exchange, 200,
                                        "{\"success\":true,\"message\":\"Logged out successfully.\"}");
                });

                // CUSTOMER: Create a service booking
                server.createContext("/api/requests", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int workerId = data.get("workerId").getAsInt();
                                int priceId = data.get("priceId").getAsInt();

                                String description = getString(data, "description");
                                String date = getString(data, "appointmentDate");
                                String startTime = getString(data, "startTime");
                                String endTime = getString(data, "endTime");

                                if (workerId <= 0 || priceId <= 0
                                                || description == null || date == null
                                                || startTime == null || endTime == null) {

                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"All booking details are required.\"}");
                                        return;
                                }

                                boolean created = Requests.createWebRequest(
                                                session.userId,
                                                workerId,
                                                priceId,
                                                description,
                                                date,
                                                startTime,
                                                endTime);

                                if (created) {
                                        sendJson(exchange, 201,
                                                        "{\"success\":true,\"message\":\"Service request created successfully!\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Booking failed. Check worker availability, appointment time, and service details.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid booking request.\"}");
                        }
                });
                server.createContext("/api/workers", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        try {
                                String query = exchange.getRequestURI().getRawQuery();

                                if (query == null || !query.startsWith("category=")) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Category is required.\"}");
                                        return;
                                }

                                String category = java.net.URLDecoder.decode(
                                                query.substring("category=".length()), "UTF-8");

                                if (category.trim().isEmpty()) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Category cannot be empty.\"}");
                                        return;
                                }

                                List<Map<String, Object>> workers = workerservices.getApprovedWorkersForApi(category);

                                sendJson(exchange, 200, gson.toJson(workers));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load workers.\"}");
                        }
                });

                // CUSTOMER: View proposed revisit appointments
                server.createContext("/api/customer/proposed-revisits", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                List<Map<String, Object>> revisits = Requests.getProposedRevisitsForApi(session.userId);

                                sendJson(exchange, 200, gson.toJson(revisits));

                        } catch (Exception e) {
                                e.printStackTrace();

                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load proposed revisit appointments.\"}");
                        }
                });
                // CUSTOMER: Approve or reject a proposed revisit appointment
                server.createContext("/api/customer/revisit-response", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                if (!data.has("requestId") || data.get("requestId").isJsonNull()
                                                || !data.has("approve") || data.get("approve").isJsonNull()) {

                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Request ID and approval decision are required.\"}");
                                        return;
                                }

                                int requestId = data.get("requestId").getAsInt();
                                boolean approve = data.get("approve").getAsBoolean();

                                if (requestId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid request ID.\"}");
                                        return;
                                }

                                boolean updated = Requests.respondToRevisitProposalForApi(
                                                session.userId, requestId, approve);

                                if (updated) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Revisit response saved successfully.\"}");
                                } else {
                                        sendJson(exchange, 404,
                                                        "{\"success\":false,\"message\":\"Proposal not found or does not belong to you.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not process revisit response.\"}");
                        }
                });

                // CUSTOMER: Get logged-in customer's service requests
                server.createContext("/api/customer/requests", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        String sql = "SELECT sr.request_id, sr.worker_id, u.name AS worker_name, "
                                        + "sr.service_name, sr.service_description, "
                                        + "sr.service_price, sr.inspection_fee, sr.parts_cost, "
                                        + "sr.appointment_date, sr.preferred_time_start, "
                                        + "sr.preferred_time_end, "
                                        + "sr.revisit_date, sr.revisit_time_start, "
                                        + "sr.revisit_time_end, sr.request_status, "
                                        + "sr.parts_description, sr.parts_status "
                                        + "FROM service_requests sr "
                                        + "JOIN workers w ON sr.worker_id = w.worker_id "
                                        + "JOIN users u ON w.user_id = u.user_id "
                                        + "WHERE sr.customer_id = ? "
                                        + "ORDER BY sr.created_at DESC";

                        List<Map<String, Object>> requests = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql)) {

                                ps.setInt(1, session.userId);

                                try (ResultSet rs = ps.executeQuery()) {

                                        while (rs.next()) {

                                                Map<String, Object> request = new HashMap<>();

                                                request.put("request_id", rs.getInt("request_id"));
                                                request.put("worker_id", rs.getInt("worker_id"));
                                                request.put("worker_name", rs.getString("worker_name"));
                                                request.put("service_name", rs.getString("service_name"));
                                                request.put("service_description",
                                                                rs.getString("service_description"));
                                                request.put("service_price",
                                                                rs.getBigDecimal("service_price"));
                                                request.put("inspection_fee",
                                                                rs.getBigDecimal("inspection_fee"));
                                                request.put("parts_cost",
                                                                rs.getBigDecimal("parts_cost"));
                                                request.put("appointment_date",
                                                                rs.getDate("appointment_date"));
                                                request.put("preferred_time_start",
                                                                rs.getTime("preferred_time_start"));
                                                request.put("revisit_date",
                                                                rs.getDate("revisit_date"));

                                                request.put("revisit_time_start",
                                                                rs.getTime("revisit_time_start"));

                                                request.put("revisit_time_end",
                                                                rs.getTime("revisit_time_end"));
                                                request.put("request_status",
                                                                rs.getString("request_status"));
                                                request.put("parts_description",
                                                                rs.getString("parts_description"));
                                                request.put("parts_status",
                                                                rs.getString("parts_status"));

                                                requests.add(request);
                                        }
                                }

                                sendJson(exchange, 200, gson.toJson(requests));

                        } catch (Exception e) {

                                e.printStackTrace();

                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load service requests.\"}");
                        }
                });

                // WORKER: View incoming booking requests
                server.createContext("/api/worker/requests", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        String sql = "SELECT sr.request_id, sr.service_name, "
                                        + "u.name AS customer_name, "
                                        + "sr.service_description, "
                                        + "sr.service_price, sr.inspection_fee, "
                                        + "sr.appointment_date, "
                                        + "sr.preferred_time_start, sr.preferred_time_end, "
                                        + "sr.request_status, "
                                        + "sr.parts_status, sr.parts_description, sr.parts_cost "
                                        + "FROM service_requests sr "
                                        + "JOIN workers w ON sr.worker_id = w.worker_id "
                                        + "JOIN users u ON sr.customer_id = u.user_id "
                                        + "WHERE w.user_id = ? "
                                        + "ORDER BY sr.created_at DESC";

                        List<Map<String, Object>> requests = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql)) {

                                ps.setInt(1, session.userId);

                                try (ResultSet rs = ps.executeQuery()) {

                                        while (rs.next()) {

                                                Map<String, Object> request = new HashMap<>();

                                                request.put("request_id", rs.getInt("request_id"));
                                                request.put("service_name", rs.getString("service_name"));
                                                request.put("customer_name", rs.getString("customer_name"));
                                                request.put("service_description",
                                                                rs.getString("service_description"));

                                                request.put("service_price",
                                                                rs.getBigDecimal("service_price"));
                                                request.put("inspection_fee",
                                                                rs.getBigDecimal("inspection_fee"));

                                                request.put("appointment_date",
                                                                rs.getDate("appointment_date") != null
                                                                                ? rs.getDate("appointment_date")
                                                                                                .toString()
                                                                                : null);

                                                request.put("preferred_time_start",
                                                                rs.getTime("preferred_time_start") != null
                                                                                ? rs.getTime("preferred_time_start")
                                                                                                .toString()
                                                                                : null);

                                                request.put("preferred_time_end",
                                                                rs.getTime("preferred_time_end") != null
                                                                                ? rs.getTime("preferred_time_end")
                                                                                                .toString()
                                                                                : null);

                                                request.put("request_status",
                                                                rs.getString("request_status"));
                                                request.put("parts_status",
                                                                rs.getString("parts_status"));
                                                request.put("parts_description",
                                                                rs.getString("parts_description"));
                                                request.put("parts_cost",
                                                                rs.getBigDecimal("parts_cost"));

                                                requests.add(request);
                                        }
                                }

                                sendJson(exchange, 200, gson.toJson(requests));

                        } catch (Exception e) {
                                e.printStackTrace();

                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load worker requests.\"}");
                        }
                });

                server.createContext("/api/worker/prices", exchange -> {

                        addCorsHeaders(exchange);

                        String method = exchange.getRequestMethod();

                        if ("OPTIONS".equalsIgnoreCase(method)) {
                                exchange.sendResponseHeaders(204, -1);
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Unauthorized\"}");
                                return;
                        }

                        // GET: Load the worker's service prices
                        if ("GET".equalsIgnoreCase(method)) {

                                try {
                                        List<Map<String, Object>> prices = workerservices
                                                        .getWorkerPricesForApi(session.userId);

                                        sendJson(exchange, 200, gson.toJson(prices));

                                } catch (SQLException e) {
                                        e.printStackTrace();
                                        sendJson(exchange, 500,
                                                        "{\"success\":false,\"message\":\"Could not load service prices\"}");
                                }

                                return;
                        }

                        // POST: Add or save a service price
                        if ("POST".equalsIgnoreCase(method)) {

                                try {
                                        StringBuilder body = new StringBuilder();

                                        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                                                        new java.io.InputStreamReader(
                                                                        exchange.getRequestBody(),
                                                                        java.nio.charset.StandardCharsets.UTF_8))) {

                                                String line;

                                                while ((line = reader.readLine()) != null) {
                                                        body.append(line);
                                                }
                                        }

                                        Map<String, Object> data = gson.fromJson(body.toString(), Map.class);

                                        if (data == null ||
                                                        data.get("serviceName") == null ||
                                                        data.get("servicePrice") == null ||
                                                        data.get("inspectionFee") == null) {

                                                throw new IllegalArgumentException("Missing price details.");
                                        }

                                        String serviceName = data.get("serviceName").toString();

                                        BigDecimal servicePrice = new BigDecimal(data.get("servicePrice").toString());

                                        BigDecimal inspectionFee = new BigDecimal(data.get("inspectionFee").toString());

                                        Object priceIdValue = data.get("priceId");

                                        boolean saved;

                                        if (priceIdValue == null) {

                                                // Add a new service price
                                                saved = workerservices.saveServicePrice(
                                                                session.userId,
                                                                serviceName,
                                                                servicePrice,
                                                                inspectionFee);

                                        } else {

                                                // Update an existing service price
                                                int priceId = new BigDecimal(priceIdValue.toString()).intValueExact();

                                                saved = workerservices.updateServicePrice(
                                                                session.userId,
                                                                priceId,
                                                                serviceName,
                                                                servicePrice,
                                                                inspectionFee);
                                        }

                                        if (saved) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Service price saved or updated successfully.\"}");
                                        } else {
                                                sendJson(exchange, 404,
                                                                "{\"success\":false,\"message\":\"Service price not found or does not belong to this worker.\"}");
                                        }

                                } catch (IllegalArgumentException | NullPointerException e) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid service price details.\"}");

                                } catch (SQLException e) {
                                        e.printStackTrace();
                                        sendJson(exchange, 500,
                                                        "{\"success\":false,\"message\":\"Could not save service price.\"}");
                                }

                                return;
                        }

                        // Any other HTTP method
                        sendJson(exchange, 405,
                                        "{\"success\":false,\"message\":\"Method not allowed\"}");
                });

                server.createContext("/api/worker/prices/delete", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                return;
                        }

                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"message\":\"Unauthorized worker.\"}");
                                return;
                        }

                        try {
                                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

                                byte[] data = new byte[1024];
                                int bytesRead;

                                while ((bytesRead = exchange.getRequestBody().read(data)) != -1) {
                                        buffer.write(data, 0, bytesRead);
                                }

                                String body = new String(
                                                buffer.toByteArray(),
                                                java.nio.charset.StandardCharsets.UTF_8);

                                JsonObject json = JsonParser.parseString(body).getAsJsonObject();
                                int priceId = json.get("priceId").getAsInt();

                                boolean deleted = workerservices.deleteServicePrice(
                                                session.userId, priceId);

                                if (deleted) {
                                        sendJson(exchange, 200,
                                                        "{\"message\":\"Service price deleted successfully.\"}");
                                } else {
                                        sendJson(exchange, 404,
                                                        "{\"message\":\"Service price not found.\"}");
                                }

                        } catch (Exception e) {
                                sendJson(exchange, 500,
                                                "{\"message\":\"Could not delete service price.\"}");
                        }
                });

                // WORKER: Get pending revisit requests
                server.createContext("/api/worker/revisit-requests", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                List<Map<String, Object>> revisits = Requests.getPendingRevisitsForApi(session.userId);
                                sendJson(exchange, 200, gson.toJson(revisits));
                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load revisit requests.\"}");
                        }
                });

                // WORKER: Propose a revisit appointment
                server.createContext("/api/worker/revisit-proposal", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int requestId = data.get("requestId").getAsInt();
                                String date = getString(data, "date");
                                String startTime = getString(data, "startTime");
                                String endTime = getString(data, "endTime");

                                if (requestId <= 0 || date == null
                                                || startTime == null || endTime == null) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Request ID, date, and appointment times are required.\"}");
                                        return;
                                }

                                boolean proposed = Requests.proposeRevisitAppointmentForApi(
                                                session.userId,
                                                requestId,
                                                date,
                                                startTime,
                                                endTime);

                                if (proposed) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Revisit appointment proposed successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Could not propose appointment. Check the date, time, and request status.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid revisit proposal.\"}");
                        }
                });

                // WORKER: Propose additional parts and their cost
                server.createContext("/api/worker/parts-proposal", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int requestId = data.get("requestId").getAsInt();
                                String description = getString(data, "description");
                                java.math.BigDecimal cost = data.get("cost").getAsBigDecimal();

                                if (requestId <= 0 || description == null
                                                || description.trim().isEmpty()
                                                || description.length() > 500
                                                || cost.compareTo(java.math.BigDecimal.ZERO) <= 0) {

                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Valid request ID, description, and cost are required.\"}");
                                        return;
                                }

                                String sql = "UPDATE service_requests sr "
                                                + "JOIN workers w ON sr.worker_id = w.worker_id "
                                                + "SET sr.parts_description = ?, "
                                                + "sr.parts_cost = ?, "
                                                + "sr.parts_status = 'PENDING' "
                                                + "WHERE sr.request_id = ? "
                                                + "AND w.user_id = ? "
                                                + "AND sr.request_status = 'ACCEPTED' "
                                                + "AND sr.parts_status IN ('NOT_REQUIRED', 'REJECTED')";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setString(1, description.trim());
                                        ps.setBigDecimal(2, cost);
                                        ps.setInt(3, requestId);
                                        ps.setInt(4, session.userId);

                                        int updated = ps.executeUpdate();

                                        if (updated > 0) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Parts proposal submitted. Waiting for customer approval.\"}");
                                        } else {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Request not found, not assigned to you, or not eligible for a parts proposal.\"}");
                                        }
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid parts proposal.\"}");
                        }
                });
                // CUSTOMER: Approve or reject additional parts
                server.createContext("/api/customer/parts-response", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int requestId = data.get("requestId").getAsInt();
                                String decision = getString(data, "decision");

                                if (requestId <= 0 || decision == null) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Request ID and decision are required.\"}");
                                        return;
                                }

                                decision = decision.trim().toUpperCase();

                                if (!decision.equals("APPROVED")
                                                && !decision.equals("REJECTED")) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Decision must be APPROVED or REJECTED.\"}");
                                        return;
                                }

                                String sql = "UPDATE service_requests "
                                                + "SET parts_status = ? "
                                                + "WHERE request_id = ? "
                                                + "AND customer_id = ? "
                                                + "AND parts_status = 'PENDING' "
                                                + "AND request_status = 'ACCEPTED'";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setString(1, decision);
                                        ps.setInt(2, requestId);
                                        ps.setInt(3, session.userId);

                                        int updated = ps.executeUpdate();

                                        if (updated > 0) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Parts proposal "
                                                                                + decision.toLowerCase()
                                                                                + " successfully.\"}");
                                        } else {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Proposal not found, not yours, or already processed.\"}");
                                        }
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid parts response.\"}");
                        }
                });

                server.createContext("/api/admin/pending-workers", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        List<Map<String, Object>> workers = Verification.getPendingWorkers();

                        sendJson(exchange, 200, gson.toJson(workers));
                });
                server.createContext("/api/admin/verify-worker", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                if (!data.has("workerId") || !data.has("decision")) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Worker ID and decision are required.\"}");
                                        return;
                                }

                                int workerId = data.get("workerId").getAsInt();
                                String decision = getString(data, "decision");
                                String remarks = getString(data, "adminRemarks");

                                if (decision == null) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Decision is required.\"}");
                                        return;
                                }

                                decision = decision.trim().toUpperCase();

                                if (!decision.equals("APPROVED")
                                                && !decision.equals("REJECTED")) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Decision must be APPROVED or REJECTED.\"}");
                                        return;
                                }

                                boolean updated = Verification.updateWorkerVerification(
                                                workerId, decision, remarks);

                                if (updated) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Worker verification updated successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Could not update worker verification. Check whether the worker is still pending.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid verification request.\"}");
                        }
                });

                // WORKER: Mark an accepted service request as completed
                server.createContext("/api/worker/complete-request", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);
                                int requestId = data.get("requestId").getAsInt();

                                if (requestId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid request ID.\"}");
                                        return;
                                }

                                String sql = "UPDATE service_requests sr "
                                                + "JOIN workers w ON sr.worker_id = w.worker_id "
                                                + "SET sr.request_status = 'WORKER_COMPLETED' "
                                                + "WHERE sr.request_id = ? "
                                                + "AND w.user_id = ? "
                                                + "AND sr.request_status = 'ACCEPTED' "
                                                + "AND sr.parts_status <> 'PENDING'";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setInt(1, requestId);
                                        ps.setInt(2, session.userId);

                                        int updated = ps.executeUpdate();

                                        if (updated > 0) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Job marked as completed. Waiting for customer confirmation.\"}");
                                        } else {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Request not found, not assigned to you, or not ready for completion.\"}");
                                        }
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not complete request.\"}");
                        }
                });
                // WORKER: Confirm payment received
                server.createContext("/api/worker/confirm-payment", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);
                                int paymentId = data.get("paymentId").getAsInt();

                                if (paymentId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid payment ID.\"}");
                                        return;
                                }

                                int workerId = -1;

                                String sql = "SELECT worker_id FROM workers WHERE user_id = ?";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setInt(1, session.userId);

                                        try (ResultSet rs = ps.executeQuery()) {
                                                if (rs.next()) {
                                                        workerId = rs.getInt("worker_id");
                                                }
                                        }
                                }

                                if (workerId == -1) {
                                        sendJson(exchange, 404,
                                                        "{\"success\":false,\"message\":\"Worker profile not found.\"}");
                                        return;
                                }

                                boolean success = PaymentServices.confirmPayment(workerId, paymentId);

                                if (success) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Payment confirmed and commission recorded.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Payment could not be confirmed.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not confirm payment.\"}");
                        }
                });
                // CUSTOMER: View payment history
                server.createContext("/api/customer/payments", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                List<Map<String, Object>> payments = PaymentServices.getPayments(session.userId,
                                                "CUSTOMER");

                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("payments", payments);

                                sendJson(exchange, 200, gson.toJson(response));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not retrieve payments.\"}");
                        }
                });

                // WORKER: View payment history
                server.createContext("/api/worker/payments", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                List<Map<String, Object>> payments = PaymentServices.getPayments(session.userId,
                                                "WORKER");

                                Map<String, Object> response = new HashMap<>();
                                response.put("success", true);
                                response.put("payments", payments);

                                sendJson(exchange, 200, gson.toJson(response));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not retrieve payments.\"}");
                        }
                });

                // CUSTOMER: Confirm that the worker has completed the service
                server.createContext("/api/customer/confirm-completion", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);
                                int requestId = data.get("requestId").getAsInt();

                                if (requestId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid request ID.\"}");
                                        return;
                                }

                                String sql = "UPDATE service_requests "
                                                + "SET request_status = 'COMPLETED' "
                                                + "WHERE request_id = ? "
                                                + "AND customer_id = ? "
                                                + "AND request_status = 'WORKER_COMPLETED'";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setInt(1, requestId);
                                        ps.setInt(2, session.userId);

                                        int updated = ps.executeUpdate();

                                        if (updated > 0) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Service completion confirmed successfully.\"}");
                                        } else {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Request not found, not yours, or not awaiting confirmation.\"}");
                                        }
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not confirm completion.\"}");
                        }
                });
                // CUSTOMER: Submit payment details
                server.createContext("/api/customer/payment", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int requestId = data.get("requestId").getAsInt();
                                String paymentMethod = data.get("paymentMethod").getAsString();

                                String transactionReference = null;

                                if (data.has("transactionReference")
                                                && !data.get("transactionReference").isJsonNull()) {
                                        transactionReference = data.get("transactionReference").getAsString();
                                }

                                if (requestId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid request ID.\"}");
                                        return;
                                }

                                boolean success = PaymentServices.submitPayment(
                                                session.userId,
                                                requestId,
                                                paymentMethod,
                                                transactionReference);

                                if (success) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Payment details submitted successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Payment submission failed. Check request status or payment details.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not submit payment.\"}");
                        }
                });

                // CUSTOMER: Submit a review
                server.createContext("/api/customer/reviews", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Customer login required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                int requestId = data.get("requestId").getAsInt();
                                int rating = data.get("rating").getAsInt();
                                String comment = getString(data, "comment");

                                if (requestId <= 0 || rating < 1 || rating > 5
                                                || (comment != null && comment.length() > 500)) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Enter a valid request ID and a rating from 1 to 5. Comment limit is 500 characters.\"}");
                                        return;
                                }

                                boolean submitted = Requests.submitWebReview(
                                                session.userId, requestId, rating, comment);

                                if (submitted) {
                                        sendJson(exchange, 201,
                                                        "{\"success\":true,\"message\":\"Review submitted successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Review could not be submitted. The request may not be completed, may not belong to you, or may already have a review.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid review request.\"}");
                        }
                });
                // Submit a customer report
                server.createContext("/api/customer/reports", exchange -> {
                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Customer access required.\"}");
                                return;
                        }

                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);
                                int requestId = data.get("requestId").getAsInt();
                                String reason = getString(data, "reason");
                                String description = getString(data, "description");

                                boolean submitted = Requests.submitWebReport(
                                                session.userId, requestId, reason, description);

                                if (submitted) {
                                        sendJson(exchange, 201,
                                                        "{\"success\":true,\"message\":\"Report submitted successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Could not submit report. Check the request ID.\"}");
                                }
                        } catch (Exception e) {
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid report details.\"}");
                        }
                });

                // Get reports submitted by the logged-in customer
                server.createContext("/api/customer/my-reports", exchange -> {
                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"CUSTOMER".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Customer access required.\"}");
                                return;
                        }

                        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        try {
                                List<Map<String, Object>> reports = Requests.getMyReportsForApi(session.userId);

                                sendJson(exchange, 200, new Gson().toJson(reports));
                        } catch (Exception e) {
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load reports.\"}");
                        }
                });

                // WORKER: Get logged-in worker's ID
                server.createContext("/api/worker/profile", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        try {
                                int workerId = Requests.getWorkerIdByUserId(session.userId);

                                if (workerId <= 0) {
                                        sendJson(exchange, 404,
                                                        "{\"success\":false,\"message\":\"Worker profile not found.\"}");
                                        return;
                                }

                                sendJson(exchange, 200,
                                                "{\"workerId\":" + workerId + "}");

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load worker profile.\"}");
                        }
                });

                // PUBLIC: Get reviews for a worker
                server.createContext("/api/reviews", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        try {
                                String query = exchange.getRequestURI().getRawQuery();

                                if (query == null || !query.startsWith("workerId=")) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Worker ID is required.\"}");
                                        return;
                                }

                                int workerId = Integer.parseInt(
                                                query.substring("workerId=".length()));

                                if (workerId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid worker ID.\"}");
                                        return;
                                }

                                List<Map<String, Object>> reviews = Requests.getWorkerReviewsForApi(workerId);

                                sendJson(exchange, 200, gson.toJson(reviews));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load reviews.\"}");
                        }
                });

                // ADMIN: Dashboard statistics
                server.createContext("/api/admin/stats", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT " +
                                        "(SELECT COUNT(*) FROM users WHERE role = 'CUSTOMER') AS customers, " +
                                        "(SELECT COUNT(*) FROM workers) AS workers, " +
                                        "(SELECT COUNT(*) FROM workers WHERE verification_status = 'PENDING') AS pending_workers, "
                                        +
                                        "(SELECT COUNT(*) FROM reports WHERE report_status = 'PENDING') AS pending_reports";

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                if (rs.next()) {
                                        Map<String, Object> stats = new HashMap<>();

                                        stats.put("customers", rs.getInt("customers"));
                                        stats.put("workers", rs.getInt("workers"));
                                        stats.put("pendingWorkers", rs.getInt("pending_workers"));
                                        stats.put("pendingReports", rs.getInt("pending_reports"));

                                        sendJson(exchange, 200, gson.toJson(stats));
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load dashboard statistics.\"}");
                        }
                });

                // ADMIN: List customers
                server.createContext("/api/admin/customers", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT user_id, name, email, phone, account_status, created_at " +
                                        "FROM users WHERE role = 'CUSTOMER' ORDER BY created_at DESC";

                        List<Map<String, Object>> customers = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                while (rs.next()) {
                                        Map<String, Object> customer = new HashMap<>();

                                        customer.put("user_id", rs.getInt("user_id"));
                                        customer.put("name", rs.getString("name"));
                                        customer.put("email", rs.getString("email"));
                                        customer.put("phone", rs.getString("phone"));
                                        customer.put("account_status", rs.getString("account_status"));
                                        customer.put("created_at", rs.getTimestamp("created_at"));

                                        customers.add(customer);
                                }

                                sendJson(exchange, 200, gson.toJson(customers));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load customers.\"}");
                        }
                });

                // ADMIN: List all workers
                server.createContext("/api/admin/workers", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT w.worker_id, u.user_id, u.name, u.email, u.phone, " +
                                        "w.service_category, w.experience_years, w.description, " +
                                        "w.verification_status, u.account_status, u.created_at " +
                                        "FROM workers w " +
                                        "JOIN users u ON w.user_id = u.user_id " +
                                        "ORDER BY u.created_at DESC";

                        List<Map<String, Object>> workers = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                while (rs.next()) {
                                        Map<String, Object> worker = new HashMap<>();

                                        worker.put("worker_id", rs.getInt("worker_id"));
                                        worker.put("user_id", rs.getInt("user_id"));
                                        worker.put("name", rs.getString("name"));
                                        worker.put("email", rs.getString("email"));
                                        worker.put("phone", rs.getString("phone"));
                                        worker.put("service_category", rs.getString("service_category"));
                                        worker.put("experience_years", rs.getInt("experience_years"));
                                        worker.put("description", rs.getString("description"));
                                        worker.put("verification_status", rs.getString("verification_status"));
                                        worker.put("account_status", rs.getString("account_status"));
                                        worker.put("created_at", rs.getTimestamp("created_at"));

                                        workers.add(worker);
                                }

                                sendJson(exchange, 200, gson.toJson(workers));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load workers.\"}");
                        }
                });

                // ADMIN: List all reports
                server.createContext("/api/admin/reports", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("GET")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT r.report_id, r.request_id, r.reporter_id, " +
                                        "reporter.name AS reporter_name, " +
                                        "r.reported_user_id, reported.name AS reported_user_name, " +
                                        "r.report_reason, r.report_description, r.report_status, " +
                                        "r.admin_remarks, r.created_at " +
                                        "FROM reports r " +
                                        "JOIN users reporter ON r.reporter_id = reporter.user_id " +
                                        "JOIN users reported ON r.reported_user_id = reported.user_id " +
                                        "ORDER BY r.created_at DESC";

                        List<Map<String, Object>> reports = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                while (rs.next()) {
                                        Map<String, Object> report = new HashMap<>();

                                        report.put("report_id", rs.getInt("report_id"));
                                        report.put("request_id", rs.getObject("request_id"));
                                        report.put("reporter_id", rs.getInt("reporter_id"));
                                        report.put("reporter_name", rs.getString("reporter_name"));
                                        report.put("reported_user_id", rs.getInt("reported_user_id"));
                                        report.put("reported_user_name", rs.getString("reported_user_name"));
                                        report.put("report_reason", rs.getString("report_reason"));
                                        report.put("report_description", rs.getString("report_description"));
                                        report.put("report_status", rs.getString("report_status"));
                                        report.put("admin_remarks", rs.getString("admin_remarks"));
                                        report.put("created_at", rs.getTimestamp("created_at"));

                                        reports.add(report);
                                }

                                sendJson(exchange, 200, gson.toJson(reports));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load reports.\"}");
                        }
                });

                // ADMIN: Update report status
                server.createContext("/api/admin/update-report", exchange -> {

                        addCorsHeaders(exchange);

                        if (exchange.getRequestMethod().equals("OPTIONS")) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!exchange.getRequestMethod().equals("POST")) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                if (!data.has("reportId") || data.get("reportId").isJsonNull()) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Report ID is required.\"}");
                                        return;
                                }

                                int reportId = data.get("reportId").getAsInt();
                                String decision = getString(data, "decision");
                                String remarks = getString(data, "adminRemarks");

                                if (reportId <= 0 || decision == null) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Valid report ID and status are required.\"}");
                                        return;
                                }

                                boolean updated = Verification.updateReportStatus(
                                                reportId, decision.trim().toUpperCase(), remarks);

                                if (updated) {
                                        sendJson(exchange, 200,
                                                        "{\"success\":true,\"message\":\"Report status updated successfully.\"}");
                                } else {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Could not update report. Check its status and remarks.\"}");
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Invalid report update request.\"}");
                        }
                });

                // WORKER: Availability management
                server.createContext("/api/worker/availability", exchange -> {

                        addCorsHeaders(exchange);

                        String method = exchange.getRequestMethod();

                        if ("OPTIONS".equalsIgnoreCase(method)) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"WORKER".equals(session.role)) {
                                sendJson(exchange, 401,
                                                "{\"success\":false,\"message\":\"Worker login required.\"}");
                                return;
                        }

                        int workerId = workerservices.getWorkerId(session.userId);

                        if (workerId <= 0) {
                                sendJson(exchange, 404,
                                                "{\"success\":false,\"message\":\"Worker profile not found.\"}");
                                return;
                        }

                        String[] validDays = {
                                        "MONDAY", "TUESDAY", "WEDNESDAY",
                                        "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
                        };

                        try {

                                // GET: View availability
                                if ("GET".equalsIgnoreCase(method)) {

                                        List<Map<String, Object>> slots = new ArrayList<>();

                                        String sql = "SELECT availability_id, available_day, "
                                                        + "start_time, end_time, is_available "
                                                        + "FROM worker_availability "
                                                        + "WHERE worker_id = ? "
                                                        + "ORDER BY FIELD(available_day, "
                                                        + "'MONDAY','TUESDAY','WEDNESDAY','THURSDAY',"
                                                        + "'FRIDAY','SATURDAY','SUNDAY'), start_time";

                                        try (Connection conn = DatabaseConnection.getConnection();
                                                        PreparedStatement ps = conn.prepareStatement(sql)) {

                                                ps.setInt(1, workerId);

                                                try (ResultSet rs = ps.executeQuery()) {

                                                        while (rs.next()) {

                                                                Map<String, Object> slot = new HashMap<>();

                                                                slot.put("availabilityId",
                                                                                rs.getInt("availability_id"));
                                                                slot.put("day",
                                                                                rs.getString("available_day"));
                                                                slot.put("startTime",
                                                                                rs.getTime("start_time").toString());
                                                                slot.put("endTime",
                                                                                rs.getTime("end_time").toString());
                                                                slot.put("isAvailable",
                                                                                rs.getBoolean("is_available"));

                                                                slots.add(slot);
                                                        }
                                                }
                                        }

                                        sendJson(exchange, 200, gson.toJson(slots));
                                        return;
                                }

                                // POST: Add availability
                                if ("POST".equalsIgnoreCase(method)
                                                || "PUT".equalsIgnoreCase(method)) {

                                        JsonObject data = readJson(exchange);

                                        String day = getString(data, "day");
                                        String startText = getString(data, "startTime");
                                        String endText = getString(data, "endTime");

                                        if (day == null || startText == null || endText == null) {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Day and both times are required.\"}");
                                                return;
                                        }

                                        day = day.trim().toUpperCase();

                                        boolean validDay = false;
                                        for (String valid : validDays) {
                                                if (valid.equals(day)) {
                                                        validDay = true;
                                                        break;
                                                }
                                        }

                                        if (!validDay) {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Invalid day.\"}");
                                                return;
                                        }

                                        java.sql.Time start;
                                        java.sql.Time end;

                                        try {
                                                start = java.sql.Time.valueOf(startText.trim() + ":00");
                                                end = java.sql.Time.valueOf(endText.trim() + ":00");

                                                if (!start.before(end)) {
                                                        sendJson(exchange, 400,
                                                                        "{\"success\":false,\"message\":\"End time must be later than start time.\"}");
                                                        return;
                                                }
                                        } catch (IllegalArgumentException e) {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Invalid time. Use HH:mm format.\"}");
                                                return;
                                        }

                                        boolean updating = "PUT".equalsIgnoreCase(method);
                                        int availabilityId = 0;

                                        if (updating) {
                                                if (!data.has("availabilityId")
                                                                || data.get("availabilityId").isJsonNull()) {
                                                        sendJson(exchange, 400,
                                                                        "{\"success\":false,\"message\":\"Availability ID is required.\"}");
                                                        return;
                                                }

                                                availabilityId = data.get("availabilityId").getAsInt();

                                                if (availabilityId <= 0) {
                                                        sendJson(exchange, 400,
                                                                        "{\"success\":false,\"message\":\"Invalid availability ID.\"}");
                                                        return;
                                                }
                                        }

                                        try (Connection conn = DatabaseConnection.getConnection()) {

                                                String checkSql = "SELECT availability_id "
                                                                + "FROM worker_availability "
                                                                + "WHERE worker_id = ? "
                                                                + "AND available_day = ? "
                                                                + "AND is_available = 1 "
                                                                + "AND start_time < ? "
                                                                + "AND end_time > ? "
                                                                + (updating ? "AND availability_id <> ?" : "");

                                                try (PreparedStatement check = conn.prepareStatement(checkSql)) {

                                                        check.setInt(1, workerId);
                                                        check.setString(2, day);
                                                        check.setTime(3, end);
                                                        check.setTime(4, start);

                                                        if (updating) {
                                                                check.setInt(5, availabilityId);
                                                        }

                                                        try (ResultSet rs = check.executeQuery()) {
                                                                if (rs.next()) {
                                                                        sendJson(exchange, 409,
                                                                                        "{\"success\":false,\"message\":\"This time slot overlaps with another availability.\"}");
                                                                        return;
                                                                }
                                                        }
                                                }

                                                if (updating) {

                                                        String sql = "UPDATE worker_availability "
                                                                        + "SET available_day = ?, start_time = ?, end_time = ? "
                                                                        + "WHERE availability_id = ? AND worker_id = ?";

                                                        try (PreparedStatement ps = conn.prepareStatement(sql)) {

                                                                ps.setString(1, day);
                                                                ps.setTime(2, start);
                                                                ps.setTime(3, end);
                                                                ps.setInt(4, availabilityId);
                                                                ps.setInt(5, workerId);

                                                                if (ps.executeUpdate() > 0) {
                                                                        sendJson(exchange, 200,
                                                                                        "{\"success\":true,\"message\":\"Availability updated successfully.\"}");
                                                                } else {
                                                                        sendJson(exchange, 404,
                                                                                        "{\"success\":false,\"message\":\"Availability not found or not yours.\"}");
                                                                }
                                                        }

                                                } else {

                                                        String sql = "INSERT INTO worker_availability "
                                                                        + "(worker_id, available_day, start_time, end_time) "
                                                                        + "VALUES (?, ?, ?, ?)";

                                                        try (PreparedStatement ps = conn.prepareStatement(sql)) {

                                                                ps.setInt(1, workerId);
                                                                ps.setString(2, day);
                                                                ps.setTime(3, start);
                                                                ps.setTime(4, end);

                                                                ps.executeUpdate();

                                                                sendJson(exchange, 201,
                                                                                "{\"success\":true,\"message\":\"Availability added successfully.\"}");
                                                        }
                                                }
                                        }

                                        return;
                                }

                                // DELETE: Remove availability
                                if ("DELETE".equalsIgnoreCase(method)) {

                                        JsonObject data = readJson(exchange);

                                        if (!data.has("availabilityId")
                                                        || data.get("availabilityId").isJsonNull()) {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Availability ID is required.\"}");
                                                return;
                                        }

                                        int availabilityId = data.get("availabilityId").getAsInt();

                                        if (availabilityId <= 0) {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Invalid availability ID.\"}");
                                                return;
                                        }

                                        String sql = "DELETE FROM worker_availability "
                                                        + "WHERE availability_id = ? AND worker_id = ?";

                                        try (Connection conn = DatabaseConnection.getConnection();
                                                        PreparedStatement ps = conn.prepareStatement(sql)) {

                                                ps.setInt(1, availabilityId);
                                                ps.setInt(2, workerId);

                                                if (ps.executeUpdate() > 0) {
                                                        sendJson(exchange, 200,
                                                                        "{\"success\":true,\"message\":\"Availability deleted successfully.\"}");
                                                } else {
                                                        sendJson(exchange, 404,
                                                                        "{\"success\":false,\"message\":\"Availability not found or not yours.\"}");
                                                }
                                        }

                                        return;
                                }

                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");

                        } catch (Exception e) {

                                e.printStackTrace();

                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Availability operation failed.\"}");
                        }
                });

                // ADMIN: View all worker commissions
                server.createContext("/api/admin/commissions", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT wc.commission_id, wc.payment_id, "
                                        + "wc.worker_id, u.name AS worker_name, "
                                        + "wc.commission_rate, wc.commission_amount, "
                                        + "wc.commission_status, wc.created_at, wc.collected_at, "
                                        + "p.request_id, p.total_amount, p.payment_method "
                                        + "FROM worker_commissions wc "
                                        + "JOIN workers w ON wc.worker_id = w.worker_id "
                                        + "JOIN users u ON w.user_id = u.user_id "
                                        + "JOIN payments p ON wc.payment_id = p.payment_id "
                                        + "ORDER BY wc.created_at DESC";

                        List<Map<String, Object>> commissions = new ArrayList<>();

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                while (rs.next()) {

                                        Map<String, Object> commission = new HashMap<>();

                                        commission.put("commission_id", rs.getInt("commission_id"));
                                        commission.put("payment_id", rs.getInt("payment_id"));
                                        commission.put("worker_id", rs.getInt("worker_id"));
                                        commission.put("worker_name", rs.getString("worker_name"));
                                        commission.put("commission_rate", rs.getBigDecimal("commission_rate"));
                                        commission.put("commission_amount", rs.getBigDecimal("commission_amount"));
                                        commission.put("commission_status", rs.getString("commission_status"));
                                        commission.put("created_at", rs.getTimestamp("created_at"));
                                        commission.put("collected_at", rs.getTimestamp("collected_at"));
                                        commission.put("request_id", rs.getInt("request_id"));
                                        commission.put("total_amount", rs.getBigDecimal("total_amount"));
                                        commission.put("payment_method", rs.getString("payment_method"));

                                        commissions.add(commission);
                                }

                                sendJson(exchange, 200, gson.toJson(commissions));

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load commissions.\"}");
                        }
                });

                // ADMIN: Get commission summary
                server.createContext("/api/admin/commission-summary", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        String sql = "SELECT "
                                        + "COALESCE(SUM(CASE WHEN commission_status = 'PENDING' "
                                        + "THEN commission_amount ELSE 0 END), 0) AS pending_amount, "
                                        + "COALESCE(SUM(CASE WHEN commission_status = 'PAID' "
                                        + "THEN commission_amount ELSE 0 END), 0) AS collected_amount, "
                                        + "COUNT(*) AS total_commissions "
                                        + "FROM worker_commissions";

                        try (Connection conn = DatabaseConnection.getConnection();
                                        PreparedStatement ps = conn.prepareStatement(sql);
                                        ResultSet rs = ps.executeQuery()) {

                                if (rs.next()) {

                                        Map<String, Object> summary = new HashMap<>();

                                        summary.put("pending_amount", rs.getBigDecimal("pending_amount"));
                                        summary.put("collected_amount", rs.getBigDecimal("collected_amount"));
                                        summary.put("total_commissions", rs.getInt("total_commissions"));

                                        sendJson(exchange, 200, gson.toJson(summary));
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 500,
                                                "{\"success\":false,\"message\":\"Could not load commission summary.\"}");
                        }
                });

                // ADMIN: Mark a commission as collected
                server.createContext("/api/admin/collect-commission", exchange -> {

                        addCorsHeaders(exchange);

                        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                                exchange.sendResponseHeaders(204, -1);
                                exchange.close();
                                return;
                        }

                        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                                sendJson(exchange, 405,
                                                "{\"success\":false,\"message\":\"Method not allowed.\"}");
                                return;
                        }

                        ApiSession session = getSession(exchange);

                        if (session == null || !"ADMIN".equals(session.role)) {
                                sendJson(exchange, 403,
                                                "{\"success\":false,\"message\":\"Admin access required.\"}");
                                return;
                        }

                        try {
                                JsonObject data = readJson(exchange);

                                if (!data.has("commissionId")
                                                || data.get("commissionId").isJsonNull()) {

                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Commission ID is required.\"}");
                                        return;
                                }

                                int commissionId = data.get("commissionId").getAsInt();

                                if (commissionId <= 0) {
                                        sendJson(exchange, 400,
                                                        "{\"success\":false,\"message\":\"Invalid commission ID.\"}");
                                        return;
                                }

                                String sql = "UPDATE worker_commissions "
                                                + "SET commission_status = 'PAID', "
                                                + "collected_at = CURRENT_TIMESTAMP "
                                                + "WHERE commission_id = ? "
                                                + "AND commission_status = 'PENDING'";

                                try (Connection conn = DatabaseConnection.getConnection();
                                                PreparedStatement ps = conn.prepareStatement(sql)) {

                                        ps.setInt(1, commissionId);

                                        int updated = ps.executeUpdate();

                                        if (updated > 0) {
                                                sendJson(exchange, 200,
                                                                "{\"success\":true,\"message\":\"Commission marked as collected successfully.\"}");
                                        } else {
                                                sendJson(exchange, 400,
                                                                "{\"success\":false,\"message\":\"Commission not found or already collected.\"}");
                                        }
                                }

                        } catch (Exception e) {
                                e.printStackTrace();
                                sendJson(exchange, 400,
                                                "{\"success\":false,\"message\":\"Could not collect commission.\"}");
                        }
                });

                server.start();

                System.out.println("ServeU API started!");
                System.out.println("Health: http://localhost:8080/api/health");
                System.out.println("Customer registration: http://localhost:8080/api/register/customer");
        }

        private static void addCorsHeaders(HttpExchange exchange) {

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Origin", "*");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Methods",
                                "GET, POST, PUT, DELETE, OPTIONS");

                exchange.getResponseHeaders().set(
                                "Access-Control-Allow-Headers", "Content-Type, Authorization");
        }

        private static JsonObject readJson(HttpExchange exchange)
                        throws IOException {

                InputStream input = exchange.getRequestBody();

                java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();

                byte[] bytes = new byte[1024];
                int length;

                while ((length = input.read(bytes)) != -1) {
                        buffer.write(bytes, 0, length);
                }

                String jsonData = new String(
                                buffer.toByteArray(),
                                StandardCharsets.UTF_8);

                return JsonParser.parseString(jsonData).getAsJsonObject();
        }

        private static String getString(JsonObject data, String key) {

                if (data == null || !data.has(key) || data.get(key).isJsonNull()) {
                        return null;
                }

                return data.get(key).getAsString();
        }

        private static ApiSession getSession(HttpExchange exchange) {

                String authorization = exchange.getRequestHeaders()
                                .getFirst("Authorization");

                if (authorization == null || !authorization.startsWith("Bearer ")) {
                        return null;
                }

                String token = authorization.substring(7).trim();

                if (token.isEmpty()) {
                        return null;
                }

                ApiSession session = sessions.get(token);

                if (session == null) {
                        return null;
                }

                if (System.currentTimeMillis() > session.expiresAt) {
                        sessions.remove(token);
                        return null;
                }

                return session;
        }

        // Common method for sending JSON responses
        private static void sendJson(
                        HttpExchange exchange,
                        int statusCode,
                        String response) throws IOException {

                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                exchange.getResponseHeaders().set(
                                "Content-Type", "application/json; charset=UTF-8");

                exchange.sendResponseHeaders(statusCode, responseBytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                        os.write(responseBytes);
                }
        }

        private static final ConcurrentHashMap<String, ApiSession> sessions = new ConcurrentHashMap<>();

        private static class ApiSession {

                int userId;
                String role;
                long expiresAt;

                ApiSession(int userId, String role) {
                        this.userId = userId;
                        this.role = role;

                        // Session expires after 30 minutes.
                        this.expiresAt = System.currentTimeMillis()
                                        + (30 * 60 * 1000);
                }
        }
}
