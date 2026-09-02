package org.assimbly.integrationrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/integration/llm")
public class LlmProviderRuntime {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderRuntime.class);
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSupportedProviders() {
        List<String> providers = List.of("openai", "google-gemini", "mistral", "groq", "ollama", "anthropic");
        return ResponseEntity.ok(providers);
    }

    @PostMapping(value = "/validate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ObjectNode> validateCredentials(@RequestBody Map<String, String> payload) {
        ObjectNode response = objectMapper.createObjectNode();
        String provider = payload.get("provider");
        String apiKey = payload.get("apiKey");
        String baseUrl = payload.get("baseUrl");

        if (provider == null || provider.isEmpty()) {
            response.put("valid", false);
            response.put("message", "Provider is required");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            List<String> models = fetchModelsFromProvider(provider, apiKey, baseUrl);
            response.put("valid", true);
            response.put("message", "Successfully connected to " + provider);
            response.put("modelsCount", models.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.warn("Validation failed for provider {}: {}", provider, e.getMessage());
            response.put("valid", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping(value = "/models", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAvailableModels(@RequestBody Map<String, String> payload) {
        String provider = payload.get("provider");
        String apiKey = payload.get("apiKey");
        String baseUrl = payload.get("baseUrl");

        if (provider == null || provider.isEmpty()) {
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", "Provider parameter is required");
            return ResponseEntity.badRequest().body(err);
        }

        try {
            List<String> models = fetchModelsFromProvider(provider, apiKey, baseUrl);
            return ResponseEntity.ok(models);
        } catch (Exception e) {
            log.error("Failed to fetch models for provider {}: {}", provider, e.getMessage());
            ObjectNode err = objectMapper.createObjectNode();
            err.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
        }
    }

    private List<String> fetchModelsFromProvider(String provider, String apiKey, String baseUrl) throws Exception {
        String p = provider.toLowerCase();
        HttpRequest.Builder reqBuilder = HttpRequest.newBuilder().timeout(Duration.ofSeconds(15));

        String targetUrl;
        switch (p) {
            case "openai" -> {
                targetUrl = "https://api.openai.com/v1/models";
                if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey is required for OpenAI");
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            case "groq" -> {
                targetUrl = "https://api.groq.com/openai/v1/models";
                if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey is required for Groq");
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            case "mistral" -> {
                targetUrl = "https://api.mistral.ai/v1/models";
                if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey is required for Mistral");
                reqBuilder.header("Authorization", "Bearer " + apiKey);
            }
            case "google-gemini", "gemini" -> {
                if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey is required for Gemini");
                targetUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=" + apiKey;
            }
            case "anthropic" -> {
                targetUrl = "https://api.anthropic.com/v1/models";
                if (apiKey == null || apiKey.isEmpty()) throw new IllegalArgumentException("apiKey is required for Anthropic");
                reqBuilder.header("x-api-key", apiKey);
                reqBuilder.header("anthropic-version", "2023-06-01");
            }
            case "ollama" -> {
                String base = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "http://localhost:11434";
                targetUrl = base.replaceAll("/+$", "") + "/api/tags";
            }
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        }

        HttpRequest request = reqBuilder.uri(URI.create(targetUrl)).GET().build();
        HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (httpResponse.statusCode() != 200) {
            throw new IllegalStateException("Provider API returned HTTP " + httpResponse.statusCode() + ": " + httpResponse.body());
        }

        return parseModelIds(p, httpResponse.body());
    }

    private List<String> parseModelIds(String provider, String jsonBody) throws Exception {
        JsonNode root = objectMapper.readTree(jsonBody);
        List<String> modelIds = new ArrayList<>();

        if ("google-gemini".equals(provider) || "gemini".equals(provider)) {
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode m : modelsNode) {
                    JsonNode nameNode = m.get("name");
                    if (nameNode != null) {
                        String name = nameNode.asText();
                        if (name.startsWith("models/")) {
                            name = name.substring("models/".length());
                        }
                        // Filter out non-chat models like embedding
                        if (name.startsWith("gemini") || name.startsWith("gemma")) {
                            modelIds.add(name);
                        }
                    }
                }
            }
        } else if ("ollama".equals(provider)) {
            JsonNode modelsNode = root.get("models");
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode m : modelsNode) {
                    JsonNode nameNode = m.get("name");
                    if (nameNode != null) {
                        modelIds.add(nameNode.asText());
                    }
                }
            }
        } else {
            // Standard OpenAI-compatible format: { "data": [ { "id": "gpt-4o" }, ... ] }
            JsonNode dataNode = root.get("data");
            if (dataNode != null && dataNode.isArray()) {
                for (JsonNode m : dataNode) {
                    JsonNode idNode = m.get("id");
                    if (idNode != null) {
                        modelIds.add(idNode.asText());
                    }
                }
            }
        }

        Collections.sort(modelIds);
        return modelIds;
    }
}
