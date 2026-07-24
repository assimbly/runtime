package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.time.Duration;

public class LangChain4jConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;
    
    private String apiKey;
    private String modelName;
    private String timeout;

    public LangChain4jConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (checkConnection()) {
            log.info("Creating new LangChain4j GoogleAiGeminiChatModel connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reuse LangChain4j GoogleAiGeminiChatModel connection with id={}", connectionId);
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
            throw new IllegalArgumentException("LangChain4j connection parameters are invalid. apikey is required");
        }

        return true;
    }

    private void setConnection() {
        log.info("Setting up LangChain4j GoogleAiGeminiChatModel connection. API Key length: {}, prefix: {}", 
                 apiKey != null ? apiKey.length() : 0, 
                 apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) : "N/A");

        String resolvedModel = (modelName != null && !modelName.isEmpty()) ? modelName : "gemini-3-flash-preview";
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

        context.getRegistry().bind(connectionId, chatModel);
        log.info("Successfully bound LangChain4j ChatModel bean with id={} to the Camel registry", connectionId);
    }
}
