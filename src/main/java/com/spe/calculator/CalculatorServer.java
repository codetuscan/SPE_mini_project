package com.spe.calculator;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CalculatorServer {

    private final Calculator calculator = new Calculator();
    private final HttpServer server;

    public CalculatorServer(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handle);
    }

    public void start() {
        server.start();
    }

    public void stop() {
        server.stop(0);
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            boolean known = path.equals("/actuator/health") || path.matches("/api/v1/(add|subtract|multiply|divide)");
            if (!known) {
                send(exchange, 404, error("Not found"));
                return;
            }
            if (!exchange.getRequestMethod().equals("GET")) {
                send(exchange, 405, error("Method not allowed"));
                return;
            }
            if (path.equals("/actuator/health")) {
                send(exchange, 200, "{\"status\":\"UP\"}");
                return;
            }
            String operation = path.substring("/api/v1/".length());
            Map<String, String> params = parseQuery(exchange.getRequestURI().getRawQuery());
            double a = parseNumber(params, "a");
            double b = parseNumber(params, "b");
            double result = calculate(operation, a, b);
            if (!Double.isFinite(result)) {
                throw new IllegalArgumentException("Result out of range");
            }
            send(exchange, 200, "{\"operation\":\"" + operation + "\",\"a\":" + format(a)
                    + ",\"b\":" + format(b) + ",\"result\":" + format(result) + "}");
        } catch (IllegalArgumentException | ArithmeticException e) {
            send(exchange, 400, error(e.getMessage()));
        } catch (RuntimeException e) {
            send(exchange, 500, error("Internal server error"));
        }
    }

    private double calculate(String operation, double a, double b) {
        return switch (operation) {
            case "add" -> calculator.add(a, b);
            case "subtract" -> calculator.subtract(a, b);
            case "multiply" -> calculator.multiply(a, b);
            default -> calculator.divide(a, b);
        };
    }

    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }
        for (String pair : rawQuery.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            String value = eq < 0 ? "" : pair.substring(eq + 1);
            params.putIfAbsent(URLDecoder.decode(key, StandardCharsets.UTF_8),
                    URLDecoder.decode(value, StandardCharsets.UTF_8));
        }
        return params;
    }

    private double parseNumber(Map<String, String> params, String name) {
        String value = params.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing parameter: " + name);
        }
        double number;
        try {
            number = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for parameter " + name);
        }
        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException("Invalid number for parameter " + name);
        }
        return number;
    }

    private String format(double value) {
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private String error(String message) {
        return "{\"error\":\"" + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
