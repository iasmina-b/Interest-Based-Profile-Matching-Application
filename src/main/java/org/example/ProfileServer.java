package org.example;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.util.concurrent.Executors;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.List;
import java.nio.charset.StandardCharsets;

public class ProfileServer {
    private static final int PORT = 8000;
    private final ProfileManager manager;

    public ProfileServer(ProfileManager manager) {
        this.manager = manager;
    }

    public void start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            server.createContext("/api/profiles", new ProfileHandler());
            server.createContext("/api/interests", new InterestHandler());

            server.createContext("/api/admin/role", new AdminHandler());

            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            System.out.println(">> Web API Server started on port " + PORT);
            System.out.println(">> Ready to accept React requests.");
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }

    class ProfileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("GET".equals(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                List<Profile> profiles = manager.getAllProfiles();
                for (int i = 0; i < profiles.size(); i++) {
                    Profile p = profiles.get(i);
                    json.append(String.format("{\"username\":\"%s\", \"age\":%d, \"interest\":\"%s\"}",
                            p.getUsername(), p.getAge(), p.getPrimaryInterest().getName()));
                    if (i < profiles.size() - 1) json.append(",");
                }
                json.append("]");

                sendResponse(exchange, json.toString(), 200);

            } else if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                try {
                    String name = extractValue(body, "username");
                    String ageStr = extractValue(body, "age");
                    String interestName = extractValue(body, "interest");

                    int age = Integer.parseInt(ageStr);
                    manager.addProfile(new Profile(name, age, new Interest(interestName), new java.util.ArrayList<>()));

                    sendResponse(exchange, "{\"message\": \"Profile created\"}", 201);
                    System.out.println("[API] Created profile: " + name);
                } catch (Exception e) {
                    sendResponse(exchange, "{\"error\": \"Invalid Data\"}", 400);
                    e.printStackTrace();
                }
            } else if ("PUT".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String currentName = "";
                String newName = "";

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            if (pair[0].equals("currentName")) currentName = pair[1];
                            if (pair[0].equals("newName")) newName = pair[1];
                        }
                    }
                }

                boolean found = false;
                for (Profile p : manager.getAllProfiles()) {
                    if (p.getUsername().equalsIgnoreCase(currentName)) {
                        p.setUsername(newName);
                        manager.saveState();
                        found = true;
                        break;
                    }
                }

                if (found) {
                    sendResponse(exchange, "{\"message\": \"Renamed successfully\"}", 200);
                } else {
                    sendResponse(exchange, "{\"error\": \"User not found\"}", 404);
                }
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String nameToDelete = "";

                if (query != null && query.startsWith("username=")) {
                    nameToDelete = query.split("=")[1];
                }

                boolean success = manager.deleteProfile(nameToDelete);

                if (success) {
                    sendResponse(exchange, "{\"message\": \"Deleted successfully\"}", 200);
                    System.out.println("[API] Deleted profile: " + nameToDelete);
                } else {
                    sendResponse(exchange, "{\"error\": \"User not found\"}", 404);
                }

            }
            else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String role = extractValue(body, "role");

                if ("admin".equals(role)) {
                    Database.setCredentials("postgres", "1234");
                    System.out.println("[API] Switched to ADMIN role.");
                    sendResponse(exchange, "{\"status\":\"Switched to Admin\"}", 200);
                } else {
                    Database.setCredentials("guest", "guest123");
                    System.out.println("[API] Switched to GUEST role.");
                    sendResponse(exchange, "{\"status\":\"Switched to Guest\"}", 200);
                }
            } else {
                sendResponse(exchange, "Method not allowed", 405);
            }
        }
    }

    class InterestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");

            if ("GET".equals(exchange.getRequestMethod())) {
                String json = "[\"Hiking\", \"Gaming\", \"Reading\", \"Cooking\", \"Fitness\"]";
                sendResponse(exchange, json, 200);
            }
        }
    }

    private void sendResponse(HttpExchange exchange, String response, int statusCode) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private String extractValue(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();

        if (json.charAt(start) == '"') {
            start++;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } else {
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            return json.substring(start, end).trim();
        }
    }
}