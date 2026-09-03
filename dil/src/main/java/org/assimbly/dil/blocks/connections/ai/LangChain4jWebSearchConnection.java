package org.assimbly.dil.blocks.connections.ai;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;

public class LangChain4jWebSearchConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String apiKey;

    public LangChain4jWebSearchConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (checkConnection()) {
            log.info("Creating new LangChain4j Web Search connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reuse LangChain4j Web Search connection with id={}", connectionId);
        }
    }

    private void setFields() {
        apiKey = properties.getProperty("connection." + connectionId + ".apikey");
    }

    private boolean checkConnection() {
        Object isRegistered = context.getRegistry().lookupByName(connectionId);
        if (isRegistered != null) {
            return false;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalArgumentException("LangChain4j Web Search connection parameters are invalid. apikey is required");
        }

        return true;
    }

    private void setConnection() {
        log.info("Setting up Tavily WebSearchEngine connection with id={}", connectionId);

        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(apiKey)
                .build();

        context.getRegistry().bind(connectionId, webSearchEngine);
        log.info("Successfully bound WebSearchEngine bean with id={} to the Camel registry", connectionId);
    }
}
