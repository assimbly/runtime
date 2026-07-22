package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.genai.Client;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;

public class SpringAiConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;
    
    private String apiKey;
    private String modelName;
    private String temperature;

    public SpringAiConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (checkConnection()) {
            log.info("Creating new Spring AI GoogleGenAiChatModel connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reuse Spring AI GoogleGenAiChatModel connection with id={}", connectionId);
        }
    }

    private void setFields() {
        apiKey = properties.getProperty("connection." + connectionId + ".apikey");
        modelName = properties.getProperty("connection." + connectionId + ".modelname");
        temperature = properties.getProperty("connection." + connectionId + ".temperature");
    }

    private boolean checkConnection() {
        Object isRegistered = context.getRegistry().lookupByName(connectionId);
        if (isRegistered != null) {
            return false;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("Spring AI connection parameters are invalid. apikey is required");
        }

        return true;
    }

    private void setConnection() {
        log.info("Setting up Spring AI GoogleGenAiChatModel connection. API Key length: {}, prefix: {}", 
                 apiKey != null ? apiKey.length() : 0, 
                 apiKey != null && apiKey.length() >= 5 ? apiKey.substring(0, 5) : "N/A");

        Client genAiClient = Client.builder()
                .apiKey(apiKey)
                .build();

        String resolvedModel = (modelName != null && !modelName.isEmpty()) ? modelName : "gemini-2.5-flash";
        double resolvedTemp = 0.7;
        if (temperature != null && !temperature.isEmpty()) {
            try {
                resolvedTemp = Double.parseDouble(temperature);
            } catch (NumberFormatException e) {
                log.warn("Invalid temperature value '{}', using default 0.7", temperature);
            }
        }

        var options = GoogleGenAiChatOptions.builder()
                .model(resolvedModel)
                .temperature(resolvedTemp)
                .build();

        GoogleGenAiChatModel chatModel = GoogleGenAiChatModel.builder()
                .genAiClient(genAiClient)
                .defaultOptions(options)
                .build();

        context.getRegistry().bind(connectionId, chatModel);
        log.info("Successfully bound Spring AI ChatModel bean with id={} to the Camel registry", connectionId);

        org.springframework.ai.chat.memory.ChatMemoryRepository memoryRepository = new org.springframework.ai.chat.memory.InMemoryChatMemoryRepository();
        org.springframework.ai.chat.memory.ChatMemory chatMemory = org.springframework.ai.chat.memory.MessageWindowChatMemory.builder()
                .chatMemoryRepository(memoryRepository)
                .build();
        context.getRegistry().bind(connectionId + "-memory", chatMemory);
        log.info("Successfully bound Spring AI MessageWindowChatMemory (with InMemoryChatMemoryRepository) bean with id={}-memory to the Camel registry", connectionId);
    }
}
