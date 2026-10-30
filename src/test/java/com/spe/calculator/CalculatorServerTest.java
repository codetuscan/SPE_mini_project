package com.spe.calculator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CalculatorServerTest {

    private static CalculatorServer server;
    private static HttpClient client;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws IOException {
        server = new CalculatorServer(0);
        server.start();
        client = HttpClient.newHttpClient();
        baseUrl = "http://localhost:" + server.getPort();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    private HttpResponse<String> request(String method, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .method(method, HttpRequest.BodyPublishers.noBody())
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws Exception {
        return request("GET", path);
    }

    @Test
    void add() throws Exception {
        HttpResponse<String> response = get("/api/v1/add?a=2&b=3");
        assertEquals(200, response.statusCode());
        assertEquals("{\"operation\":\"add\",\"a\":2,\"b\":3,\"result\":5}", response.body().strip());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    @Test
    void subtract() throws Exception {
        assertEquals("{\"operation\":\"subtract\",\"a\":2,\"b\":3,\"result\":-1}", get("/api/v1/subtract?a=2&b=3").body().strip());
    }

    @Test
    void multiply() throws Exception {
        assertEquals("{\"operation\":\"multiply\",\"a\":2.5,\"b\":4,\"result\":10}", get("/api/v1/multiply?a=2.5&b=4").body().strip());
    }

    @Test
    void divide() throws Exception {
        assertEquals("{\"operation\":\"divide\",\"a\":5,\"b\":2,\"result\":2.5}", get("/api/v1/divide?a=5&b=2").body().strip());
    }

    @Test
    void divideByZero() throws Exception {
        HttpResponse<String> response = get("/api/v1/divide?a=5&b=0");
        assertEquals(400, response.statusCode());
        assertEquals("{\"error\":\"Division by zero\"}", response.body().strip());
    }

    @Test
    void missingParameter() throws Exception {
        HttpResponse<String> response = get("/api/v1/add?a=2");
        assertEquals(400, response.statusCode());
        assertEquals("{\"error\":\"Missing parameter: b\"}", response.body().strip());
    }

    @Test
    void invalidParameter() throws Exception {
        HttpResponse<String> response = get("/api/v1/add?a=abc&b=3");
        assertEquals(400, response.statusCode());
        assertEquals("{\"error\":\"Invalid number for parameter a\"}", response.body().strip());
    }

    @Test
    void nonFiniteParameter() throws Exception {
        assertEquals(400, get("/api/v1/add?a=NaN&b=3").statusCode());
        assertEquals(400, get("/api/v1/add?a=Infinity&b=3").statusCode());
    }

    @Test
    void health() throws Exception {
        HttpResponse<String> response = get("/actuator/health");
        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"UP\"}", response.body().strip());
    }

    @Test
    void unknownPath() throws Exception {
        assertEquals(404, get("/api/v1/power?a=2&b=3").statusCode());
    }

    @Test
    void wrongMethod() throws Exception {
        assertEquals(405, request("POST", "/api/v1/add?a=2&b=3").statusCode());
    }
}
