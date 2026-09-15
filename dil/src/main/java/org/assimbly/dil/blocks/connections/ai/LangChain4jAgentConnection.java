package org.assimbly.dil.blocks.connections.ai;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchTool;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import dev.langchain4j.memory.ChatMemory;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class LangChain4jAgentConnection {

    protected static Logger log = LoggerFactory.getLogger(LangChain4jAgentConnection.class);

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String provider;
    private String apiKey;
    private String modelName;
    private String timeout;
    private String webSearchApiKey;
    private String temperature;
    private String maxMessages;
    private String maxTokens;
    private String thinking;
    private String baseUrl;

    public LangChain4jAgentConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (context.getRegistry().lookupByName(connectionId) != null) {
            log.info("Updating existing LangChain4j Agent connection with id={}, provider={}", connectionId, provider);
            context.getRegistry().unbind(connectionId);
        } else {
            log.info("Creating new LangChain4j Agent connection with id={}, provider={}", connectionId, provider);
        }
        setConnection();

    }

    private void setFields() {
        provider = properties.getProperty("connection." + connectionId + ".provider");
        apiKey = properties.getProperty("connection." + connectionId + ".apikey");
        modelName = properties.getProperty("connection." + connectionId + ".modelname");
        timeout = properties.getProperty("connection." + connectionId + ".timeout");
        webSearchApiKey = properties.getProperty("connection." + connectionId + ".websearchapikey");
        temperature = properties.getProperty("connection." + connectionId + ".temperature");
        maxMessages = properties.getProperty("connection." + connectionId + ".maxmessages");
        maxTokens = properties.getProperty("connection." + connectionId + ".maxtokens");
        thinking = properties.getProperty("connection." + connectionId + ".thinking");
        baseUrl = properties.getProperty("connection." + connectionId + ".baseurl");

        if (provider == null || provider.isEmpty()) {
            if (apiKey != null && apiKey.startsWith("sk-")) {
                provider = "openai";
            } else {
                provider = "google-gemini";
            }
        }
    }

    private void setConnection() {

        log.info("Setting up LangChain4j Agent connection for provider={}. API Key length: {}, prefix: {}",
                provider,
                apiKey != null ? apiKey.length() : 0,
                apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) : "N/A");

        long resolvedTimeout = parseOrDefault(timeout, 10L, Long::parseLong, "timeout");
        double resolvedTemp = parseOrDefault(temperature, 0.7, Double::parseDouble, "temperature");
        int resolvedMaxMessages = parseOrDefault(maxMessages, 100, Integer::parseInt, "maxMessages");
        int resolvedMaxTokens = parseOrDefault(maxTokens, 1000, Integer::parseInt, "maxTokens");

        Boolean resolvedThinking = (thinking != null && !thinking.isEmpty())
                ? Boolean.parseBoolean(thinking)
                : null;

        ChatModel chatModel = buildChatModel(provider, resolvedTimeout, resolvedTemp, resolvedMaxTokens, resolvedThinking);

        InMemoryChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();
        Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
        ChatMemoryProvider chatMemoryProvider = memoryId -> memories.computeIfAbsent(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(resolvedMaxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build()
        );

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(chatModel)
                .withChatMemoryProvider(chatMemoryProvider);

        java.util.Set<WebSearchEngine> searchEngines = context.getRegistry().findByType(WebSearchEngine.class);
        if (!searchEngines.isEmpty()) {
            log.info("Attaching registered WebSearchEngine to LangChain4j Agent with connection id={}", connectionId);
            WebSearchTool webSearchTool = WebSearchTool.from(searchEngines.iterator().next());
            config.withCustomTools(List.of(webSearchTool));
        } else if (webSearchApiKey != null && !webSearchApiKey.isEmpty()) {
            log.info("Attaching Tavily WebSearchTool to LangChain4j Agent with connection id={}", connectionId);
            WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                    .apiKey(webSearchApiKey)
                    .build();
            WebSearchTool webSearchTool = WebSearchTool.from(webSearchEngine);
            config.withCustomTools(List.of(webSearchTool));
        }

        Agent agent = new AgentWithMemory(config);

        context.getRegistry().bind(connectionId, agent);
        log.info("Successfully bound LangChain4j Agent bean with id={} to the Camel registry", connectionId);
    }

    private ChatModel buildChatModel(String provider, long timeoutSec, double temp, int maxTokens, Boolean thinking) {
        String p = provider.toLowerCase();

        return switch (p) {
            case "openai" -> buildOpenAiChatModel(timeoutSec, temp, maxTokens);
            case "groq" -> buildGroqChatModel(timeoutSec, temp, maxTokens);
            case "mistral" -> buildMistralChatModel(timeoutSec, temp, maxTokens);
            case "ollama" -> buildOllamaChatModel(timeoutSec, temp, maxTokens);
            default -> buildGeminiChatModel(timeoutSec, temp, maxTokens, thinking);
        };
    }

    private ChatModel buildOpenAiChatModel(long timeoutSec, double temp, int maxTokens) {
        String model = modelName != null && !modelName.isEmpty()
                ? modelName
                : "gpt-4o-mini";

        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temp)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSec));

        if (baseUrl != null && !baseUrl.isEmpty()) {
            builder.baseUrl(baseUrl);
        }

        return builder.build();
    }

    private ChatModel buildGroqChatModel(long timeoutSec, double temp, int maxTokens) {
        String model = modelName != null && !modelName.isEmpty()
                ? modelName
                : "llama-3.3-70b-versatile";

        String targetUrl = baseUrl != null && !baseUrl.isEmpty()
                ? baseUrl
                : "https://api.groq.com/openai/v1";

        return OpenAiChatModel.builder()
                .baseUrl(targetUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temp)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSec))
                .build();
    }

    private ChatModel buildMistralChatModel(long timeoutSec, double temp, int maxTokens) {
        String model = modelName != null && !modelName.isEmpty()
                ? modelName
                : "mistral-small-latest";

        String targetUrl = baseUrl != null && !baseUrl.isEmpty()
                ? baseUrl
                : "https://api.mistral.ai/v1";

        return OpenAiChatModel.builder()
                .baseUrl(targetUrl)
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temp)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSec))
                .build();
    }

    private ChatModel buildOllamaChatModel(long timeoutSec, double temp, int maxTokens) {
        String model = modelName != null && !modelName.isEmpty()
                ? modelName
                : "llama3";

        String targetUrl = baseUrl != null && !baseUrl.isEmpty()
                ? baseUrl
                : "http://localhost:11434/v1";

        return OpenAiChatModel.builder()
                .baseUrl(targetUrl)
                .apiKey(apiKey != null ? apiKey : "ollama")
                .modelName(model)
                .temperature(temp)
                .maxTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSec))
                .build();
    }

    private ChatModel buildGeminiChatModel(long timeoutSec, double temp, int maxTokens, Boolean thinking) {
        String model = modelName != null && !modelName.isEmpty()
                ? modelName
                : "gemini-1.5-flash";

        boolean enableThinking = thinking == null || thinking;

        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .temperature(temp)
                .maxOutputTokens(maxTokens)
                .timeout(Duration.ofSeconds(timeoutSec))
                .returnThinking(enableThinking)
                .sendThinking(enableThinking)
                .build();
    }

    private static <T> T parseOrDefault(String value, T defaultValue, Function<String, T> parser, String label) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return parser.apply(value);
        } catch (NumberFormatException _) {
            log.warn("Invalid {} value '{}', using default {}", label, value, defaultValue);
            return defaultValue;
        }
    }

}