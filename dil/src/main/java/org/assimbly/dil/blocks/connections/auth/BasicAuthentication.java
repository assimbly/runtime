package org.assimbly.dil.blocks.connections.auth;

import org.apache.camel.CamelContext;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.server.Handler;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicAuthentication {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String SOURCE_STEP_TYPE = "source";

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;
    private final String stepType;
    private final String stepId;

    private String username;
    private String password;

    public BasicAuthentication(CamelContext context, EncryptableProperties properties, String connectionId,
                               String stepType, String stepId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.stepType = stepType;
        this.stepId = stepId;
    }

    public void start() throws Exception {

        log.info("Setting Basic Authentication for connection={}", connectionId);

        setFields();

        if (username == null || password == null) {
            throw new Exception("Basic Authentication: Username/password are required");
        }

        if (!SOURCE_STEP_TYPE.equalsIgnoreCase(stepType)) {
            log.warn("Basic Authentication for connection {} is only supported on source steps (was {})",
                    connectionId, stepType);
            return;
        }

        addInboundHandlerToRegistry();
    }

    private void setFields() {
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");
    }

    private void addInboundHandlerToRegistry() throws Exception {

        String uri = properties.getProperty(stepType + "." + stepId + ".uri");
        if (uri == null) {
            throw new Exception("Basic Authentication: No uri found for " + stepType + " step " + stepId);
        }

        String path = getPath(uri);
        boolean matchOnUriPrefix = getOption(uri, "matchOnUriPrefix");

        BasicAuthHandler handler = BasicAuthHandler.getInstance(connectionId);
        handler.configure(path, matchOnUriPrefix, username, password);

        context.getRegistry().bind(connectionId, Handler.class, handler);

        Object isRegistered = context.getRegistry().lookupByName(connectionId);
        if (isRegistered == null) {
            throw new Exception("Basic Authentication for connection " + connectionId
                    + " cannot be registered. Handler is null");
        }

        log.info("Basic Authentication for connection {} is registered", connectionId);
    }

    // Returns the path of an uri like https://0.0.0.0:9001/path/?options
    private String getPath(String uri) {

        String path = StringUtils.substringBefore(uri, "?");

        if (path.contains("://")) {
            path = StringUtils.substringAfter(path, "://");
            path = path.contains("/") ? path.substring(path.indexOf("/")) : "/";
        }

        return path;
    }

    private boolean getOption(String uri, String option) {

        String query = StringUtils.substringAfter(uri, "?");

        for (String parameter : query.split("&")) {
            if (parameter.equalsIgnoreCase(option + "=true")) {
                return true;
            }
        }

        return false;
    }

}
