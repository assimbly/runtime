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
import dev.langchain4j.memory.ChatMemory;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LangChain4jAgentConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String provider;
    private String apiKey;
    private String modelName;
    private String timeout;
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

        if (checkConnection()) {
            log.info("Creating new LangChain4j Agent connection with id={}, provider={}", connectionId, provider);
            setConnection();
        } else {
            log.info("Reuse LangChain4j Agent connection with id={}", connectionId);
        }
    }

    private void setFields() {
        provider = properties.getProperty("connection." + connectionId + ".provider");
        apiKey = properties.getProperty("connection." + connectionId + ".apikey");
        modelName = properties.getProperty("connection." + connectionId + ".modelname");
        timeout = properties.getProperty("connection." + connectionId + ".timeout");
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

    private boolean checkConnection() {
        Object isRegistered = context.getRegistry().lookupByName(connectionId);
        if (isRegistered != null) {
            return false;
        }

        if (!"ollama".equalsIgnoreCase(provider) && (apiKey == null || apiKey.isEmpty())) {
            throw new IllegalArgumentException("LangChain4j agent connection parameters are invalid. apikey is required for provider " + provider);
        }

        return true;
    }

    private void setConnection() {
        log.info("Setting up LangChain4j Agent connection for provider={}. API Key length: {}, prefix: {}",
                provider,
                apiKey != null ? apiKey.length() : 0,
                apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) : "N/A");

        long resolvedTimeout = 10;
        if (timeout != null && !timeout.isEmpty()) {
            try {
                resolvedTimeout = Long.parseLong(timeout);
            } catch (NumberFormatException e) {
                log.warn("Invalid timeout value '{}', using default 10s", timeout);
            }
        }

        double resolvedTemp = 0.7;
        if (temperature != null && !temperature.isEmpty()) {
            try {
                resolvedTemp = Double.parseDouble(temperature);
            } catch (NumberFormatException e) {
                log.warn("Invalid temperature value '{}', using default 0.7", temperature);
            }
        }

        int resolvedMaxMessages = 100;
        if (maxMessages != null && !maxMessages.isEmpty()) {
            try {
                resolvedMaxMessages = Integer.parseInt(maxMessages);
            } catch (NumberFormatException e) {
                log.warn("Invalid maxMessages value '{}', using default 100", maxMessages);
            }
        }

        int resolvedMaxTokens = 1000;
        if (maxTokens != null && !maxTokens.isEmpty()) {
            try {
                resolvedMaxTokens = Integer.parseInt(maxTokens);
            } catch (NumberFormatException e) {
                log.warn("Invalid maxTokens value '{}', using default 1000", maxTokens);
            }
        }

        boolean resolvedThinking = Boolean.parseBoolean(thinking);

        ChatModel chatModel = buildChatModel(provider, resolvedTimeout, resolvedTemp, resolvedMaxTokens, resolvedThinking);

        final int finalMaxMessages = resolvedMaxMessages;
        InMemoryChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();
        Map<Object, ChatMemory> memories = new ConcurrentHashMap<>();
        ChatMemoryProvider chatMemoryProvider = memoryId -> memories.computeIfAbsent(memoryId, id ->
                MessageWindowChatMemory.builder()
                        .id(id)
                        .maxMessages(finalMaxMessages)
                        .chatMemoryStore(chatMemoryStore)
                        .build()
        );

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(chatModel)
                .withChatMemoryProvider(chatMemoryProvider);

        Agent agent = new AgentWithMemory(config);

        context.getRegistry().bind(connectionId, agent);
        log.info("Successfully bound LangChain4j Agent bean with id={} to the Camel registry", connectionId);
    }

    private ChatModel buildChatModel(String provider, long timeoutSec, double temp, int maxTokens, boolean thinking) {
        String p = provider.toLowerCase();
        return switch (p) {
            case "openai" -> {
                String model = (modelName != null && !modelName.isEmpty()) ? modelName : "gpt-4o-mini";
                var builder = OpenAiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temp)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(timeoutSec));
                if (baseUrl != null && !baseUrl.isEmpty()) {
                    builder.baseUrl(baseUrl);
                }
                yield builder.build();
            }
            case "groq" -> {
                String model = (modelName != null && !modelName.isEmpty()) ? modelName : "llama-3.3-70b-versatile";
                String targetUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://api.groq.com/openai/v1";
                yield OpenAiChatModel.builder()
                        .baseUrl(targetUrl)
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temp)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .build();
            }
            case "mistral" -> {
                String model = (modelName != null && !modelName.isEmpty()) ? modelName : "mistral-small-latest";
                String targetUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "https://api.mistral.ai/v1";
                yield OpenAiChatModel.builder()
                        .baseUrl(targetUrl)
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temp)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .build();
            }
            case "ollama" -> {
                String model = (modelName != null && !modelName.isEmpty()) ? modelName : "llama3";
                String targetUrl = (baseUrl != null && !baseUrl.isEmpty()) ? baseUrl : "http://localhost:11434/v1";
                yield OpenAiChatModel.builder()
                        .baseUrl(targetUrl)
                        .apiKey(apiKey != null ? apiKey : "ollama")
                        .modelName(model)
                        .temperature(temp)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .build();
            }
            default -> { // google-gemini
                String model = (modelName != null && !modelName.isEmpty()) ? modelName : "gemini-1.5-flash";
                yield GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(model)
                        .temperature(temp)
                        .maxOutputTokens(maxTokens)
                        .timeout(Duration.ofSeconds(timeoutSec))
                        .build();
            }
        };
    }
}