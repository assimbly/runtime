package org.assimbly.integrationrest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/integration/llm")
public class LlmProviderRuntime {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderRuntime.class);

    private static final Duration HTTP_CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration HTTP_REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";

    private static final List<String> SUPPORTED_PROVIDERS = List.of(
            "openai",
            "google-gemini",
            "mistral",
            "groq",
            "ollama",
            "anthropic"
    );

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_CONNECT_TIMEOUT)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getSupportedProviders() {
        return ResponseEntity.ok(SUPPORTED_PROVIDERS);
    }

    @PostMapping(
            value = "/validate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ObjectNode> validateCredentials(
            @RequestBody Map<String, String> payload)
            throws InterruptedException {

        String provider = payload.get("provider");
        String apiKey = payload.get("apiKey");
        String baseUrl = payload.get("baseUrl");

        if (provider == null || provider.isBlank()) {
            return badRequest("valid", false, "Provider is required");
        }

        try {
            List<String> models = fetchModelsFromProvider(provider, apiKey, baseUrl);

            ObjectNode response = objectMapper.createObjectNode();
            response.put("valid", true);
            response.put("message", "Successfully connected to " + provider);
            response.put("modelsCount", models.size());

            return ResponseEntity.ok(response);

        } catch (IOException | RuntimeException e) {
            log.warn(
                    "Validation failed for provider {}: {}",
                    provider,
                    e.getMessage()
            );

            ObjectNode response = objectMapper.createObjectNode();
            response.put("valid", false);
            response.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping(
            value = "/models",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<List<String>> getAvailableModels(
            @RequestBody Map<String, String> payload)
            throws InterruptedException {

        String provider = payload.get("provider");
        String apiKey = payload.get("apiKey");
        String baseUrl = payload.get("baseUrl");

        if (provider == null || provider.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            List<String> models = fetchModelsFromProvider(provider, apiKey, baseUrl);
            return ResponseEntity.ok(models);

        } catch (IOException | RuntimeException e) {
            log.error(
                    "Failed to fetch models for provider {}: {}",
                    provider,
                    e.getMessage()
            );

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    private List<String> fetchModelsFromProvider(
            String provider,
            String apiKey,
            String baseUrl)
            throws IOException, InterruptedException {

        String normalizedProvider = normalizeProvider(provider);
        URI targetUri = buildModelsUri(normalizedProvider, apiKey, baseUrl);
        HttpRequest request = buildModelsRequest(
                normalizedProvider,
                targetUri,
                apiKey
        );

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        validateResponse(response);

        return parseModelIds(normalizedProvider, response.body());
    }

    private String normalizeProvider(String provider) {
        return provider.toLowerCase(Locale.ROOT);
    }

    private URI buildModelsUri(
            String provider,
            String apiKey,
            String baseUrl) {

        return switch (provider) {
            case "openai" ->
                    URI.create("https://api.openai.com/v1/models");

            case "groq" ->
                    URI.create("https://api.groq.com/openai/v1/models");

            case "mistral" ->
                    URI.create("https://api.mistral.ai/v1/models");

            case "google-gemini", "gemini" ->
                    URI.create(
                            "https://generativelanguage.googleapis.com/v1beta/models?key="
                                    + requireApiKey(provider, apiKey)
                    );

            case "anthropic" ->
                    URI.create("https://api.anthropic.com/v1/models");

            case "ollama" -> {
                String base = baseUrl == null || baseUrl.isBlank()
                        ? DEFAULT_OLLAMA_URL
                        : baseUrl;

                yield URI.create(stripTrailingSlashes(base) + "/api/tags");
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported LLM provider: " + provider
                    );
        };
    }

    private HttpRequest buildModelsRequest(
            String provider,
            URI targetUri,
            String apiKey) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(targetUri)
                .timeout(HTTP_REQUEST_TIMEOUT)
                .GET();

        switch (provider) {
            case "openai", "groq", "mistral" ->
                    builder.header(
                            "Authorization",
                            "Bearer " + requireApiKey(provider, apiKey)
                    );

            case "anthropic" -> {
                builder.header(
                        "x-api-key",
                        requireApiKey(provider, apiKey)
                );
                builder.header(
                        "anthropic-version",
                        "2023-06-01"
                );
            }

            case "google-gemini", "gemini", "ollama" -> {
                // Gemini authentication is part of the URL.
                // Ollama does not require authentication by default.
            }

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported LLM provider: " + provider
                    );
        }

        return builder.build();
    }

    private String requireApiKey(String provider, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "apiKey is required for " + provider
            );
        }

        return apiKey;
    }

    private void validateResponse(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            throw new IllegalStateException(
                    "Provider API returned HTTP "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }
    }

    private List<String> parseModelIds(String provider, String jsonBody) {

        JsonNode root = objectMapper.readTree(jsonBody);

        List<String> modelIds = switch (provider) {
            case "google-gemini", "gemini" ->
                    parseGeminiModelIds(root);

            case "ollama" ->
                    parseOllamaModelIds(root);

            default ->
                    parseOpenAiModelIds(root);
        };

        modelIds.sort(String::compareTo);
        return modelIds;
    }

    private List<String> parseGeminiModelIds(JsonNode root) {
        List<String> modelIds = new ArrayList<>();
        JsonNode modelsNode = root.get("models");

        if (modelsNode == null || !modelsNode.isArray()) {
            return modelIds;
        }

        for (JsonNode model : modelsNode) {
            JsonNode nameNode = model.get("name");

            if (nameNode != null && nameNode.isTextual()) {
                String name = nameNode.textValue();

                if (name.startsWith("models/")) {
                    name = name.substring("models/".length());
                }

                if (name.startsWith("gemini") || name.startsWith("gemma")) {
                    modelIds.add(name);
                }
            }

        }

        return modelIds;
    }

    private List<String> parseOllamaModelIds(JsonNode root) {
        List<String> modelIds = new ArrayList<>();
        JsonNode modelsNode = root.get("models");

        if (modelsNode == null || !modelsNode.isArray()) {
            return modelIds;
        }

        for (JsonNode model : modelsNode) {
            JsonNode nameNode = model.get("name");

            if (nameNode != null) {
                modelIds.add(nameNode.asText());
            }
        }

        return modelIds;
    }

    private List<String> parseOpenAiModelIds(JsonNode root) {
        List<String> modelIds = new ArrayList<>();
        JsonNode dataNode = root.get("data");

        if (dataNode == null || !dataNode.isArray()) {
            return modelIds;
        }

        for (JsonNode model : dataNode) {
            JsonNode idNode = model.get("id");

            if (idNode != null) {
                modelIds.add(idNode.asText());
            }
        }

        return modelIds;
    }

    private ResponseEntity<ObjectNode> badRequest(
            String field,
            boolean value,
            String message) {

        ObjectNode response = objectMapper.createObjectNode();
        response.put(field, value);
        response.put("message", message);

        return ResponseEntity.badRequest().body(response);
    }

    private static String stripTrailingSlashes(String value) {
        int end = value.length();

        while (end > 0 && value.charAt(end - 1) == '/') {
            end--;
        }

        return value.substring(0, end);
    }
}