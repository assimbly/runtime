package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;


public class RabbitMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String componentName;
    private final String connectionId;
    private String uri;
    private String host;
    private String port;
    private String username;
    private String password;
    private String virtualHost;

    public RabbitMQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }

    public void start() throws Exception {

        setFields();

        if(checkConnection()){
            log.info("Creating new AMQP client connection for RabbitMQ.");
            setConnection();
        }else{
            log.info("Reuse AMQP client connection for RabbitMQ.");
        }

    }

    private void setFields(){

        uri = properties.getProperty("connection." + connectionId + ".uri");
        host = properties.getProperty("connection." + connectionId + ".host");
        port = properties.getProperty("connection." + connectionId + ".port");
        virtualHost = properties.getProperty("connection." + connectionId + ".vhost");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }

    private boolean checkConnection() throws Exception {

        Object isRegistered = context.getRegistry().lookupByName(connectionId);

        if(isRegistered != null){
            return false;
        }

        if (uri == null && (host == null && port == null)) {
            throw new IllegalArgumentException(componentName + " connection parameters are invalid. Broker uri or host/port are required");
        }

        return true;

    }


    private void setConnection() {

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory();

        if (uri != null) {
            connectionFactory.setUri(uri);
        } else if (host != null && port != null) {
            connectionFactory.setHost(host);
            connectionFactory.setPort(Integer.parseInt(port));
        } else {
            connectionFactory.setHost("localhost");
            connectionFactory.setPort(5672);
        }

        if (virtualHost != null) {
            connectionFactory.setVirtualHost(virtualHost);
        } else {
            connectionFactory.setVirtualHost("/");
        }

        if (username != null && password != null) {
            connectionFactory.setUsername(username);
            connectionFactory.setPassword(password);
        }

        connectionFactory.start();
        context.getRegistry().bind(connectionId, connectionFactory);

    }

}
