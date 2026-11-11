package org.assimbly.integrationrest;

import com.sun.net.httpserver.HttpServer;
import org.assimbly.integrationrest.exception.RuntimeExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regression tests for the defects fixed in the integration REST review round. */
class IntegrationRestReviewRegressionTest {

    @Test
    void keystoreNameIsAFileNameAndNotAPath() {
        assertDoesNotThrow(() -> CertificateManagerRuntime.requireKeystoreName("outbound-truststore.p12"));

        for (String keystoreName : new String[]{"../outside.p12", "..\\outside.p12", "sub/keystore.p12", "/etc/keystore.p12", "..", " "}) {
            assertThrows(IllegalArgumentException.class, () -> CertificateManagerRuntime.requireKeystoreName(keystoreName), keystoreName);
        }
    }

    @Test
    void requestWithoutAcceptHeaderStillGetsTheErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/integration/flow/1/start");

        ResponseEntity<String> response = new RuntimeExceptionHandler()
                .integrationErrorHandler(new IllegalStateException("Flow failed"), new ServletWebRequest(request));

        assertTrue(response.getStatusCode().is4xxClientError());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("Flow failed"), response.getBody());
    }

    @Test
    void geminiApiKeyIsSentAsHeaderAndNotInTheUrl() {
        URI uri = URI.create("https://generativelanguage.googleapis.com/v1beta/models");

        HttpRequest request = new LlmProviderRuntime().buildModelsRequest("google-gemini", uri, "secret-key");

        assertFalse(request.uri().toString().contains("secret-key"));
        assertEquals("secret-key", request.headers().firstValue("x-goog-api-key").orElseThrow());
    }

    @Test
    void providerErrorIsNotEchoedInFull() throws Exception {
        byte[] body = "e".repeat(20_000).getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/tags", exchange -> {
            exchange.sendResponseHeaders(500, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        try {
            String baseUrl = "http://localhost:" + server.getAddress().getPort();
            LlmProviderRuntime.CredentialValidationResult result = new LlmProviderRuntime()
                    .validateCredentials(Map.of("provider", "ollama", "baseUrl", baseUrl)).getBody();

            assertNotNull(result);
            assertFalse(result.valid());
            assertTrue(result.message().contains("HTTP 500"), result.message());
            assertTrue(result.message().length() < 1_000, "length " + result.message().length());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void ollamaBaseUrlMustBeHttp() throws Exception {
        LlmProviderRuntime.CredentialValidationResult result = new LlmProviderRuntime()
                .validateCredentials(Map.of("provider", "ollama", "baseUrl", "file:///etc")).getBody();

        assertNotNull(result);
        assertFalse(result.valid());
        assertTrue(result.message().contains("http"), result.message());
    }

    @Test
    void serviceKeysMayHoldValuesThatAreNotText() {
        TreeMap<String, Object> serviceKeys = new TreeMap<>(Map.of("port", 5672, "ssl", true, "host", "localhost"));

        assertEquals(Map.of("port", "5672", "ssl", "true", "host", "localhost"), MessageManagerRuntime.toStringTreeMap(serviceKeys));
    }

}
