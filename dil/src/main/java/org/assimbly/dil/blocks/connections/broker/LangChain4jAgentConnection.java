package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.apache.camel.component.langchain4j.agent.api.Agent;
import org.apache.camel.component.langchain4j.agent.api.AgentConfiguration;
import org.apache.camel.component.langchain4j.agent.api.AgentWithMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import java.time.Duration;

public class LangChain4jAgentConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String apiKey;
    private String modelName;
    private String timeout;

    public LangChain4jAgentConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (checkConnection()) {
            log.info("Creating new LangChain4j Agent connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reuse LangChain4j Agent connection with id={}", connectionId);
        }
    }

    private void setFields() {
        apiKey = properties.getProperty("connection." + connectionId + ".apikey");
        modelName = properties.getProperty("connection." + connectionId + ".modelname");
        timeout = properties.getProperty("connection." + connectionId + ".timeout");
    }

    private boolean checkConnection() {
        Object isRegistered = context.getRegistry().lookupByName(connectionId);
        if (isRegistered != null) {
            return false;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("LangChain4j agent connection parameters are invalid. apikey is required");
        }

        return true;
    }

    private void setConnection() {
        log.info("Setting up LangChain4j Agent connection. API Key length: {}, prefix: {}",
                apiKey != null ? apiKey.length() : 0,
                apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) : "N/A");

        String resolvedModel = (modelName != null && !modelName.isEmpty()) ? modelName : "gemini-2.5-flash";
        long resolvedTimeout = 10;
        if (timeout != null && !timeout.isEmpty()) {
            try {
                resolvedTimeout = Long.parseLong(timeout);
            } catch (NumberFormatException e) {
                log.warn("Invalid timeout value '{}', using default 10s", timeout);
            }
        }

        ChatModel chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(resolvedModel)
                .timeout(Duration.ofSeconds(resolvedTimeout))
                .build();

        InMemoryChatMemoryStore chatMemoryStore = new InMemoryChatMemoryStore();
        ChatMemoryProvider chatMemoryProvider = memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(100)
                .chatMemoryStore(chatMemoryStore)
                .build();

        AgentConfiguration config = new AgentConfiguration()
                .withChatModel(chatModel)
                .withChatMemoryProvider(chatMemoryProvider);

        Agent agent = new AgentWithMemory(config);

        context.getRegistry().bind(connectionId, agent);
        log.info("Successfully bound LangChain4j Agent bean with id={} to the Camel registry", connectionId);
    }
}