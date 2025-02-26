package org.assimbly.dil.blocks.connections.broker;

import jakarta.jms.JMSException;
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
    private CachingConnectionFactory rabbitMQConnectionFactory;
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

        log.info("Setting up AMQP client connection for RabbitMQ.");

        setFields();

        if (context.hasComponent(componentName) == null) {
            if (uri != null) {
                setConnection();
            }else if(host!=null && port!=null){
                setConnection();
            }else{
                throw new Exception("RabbitMQ connection parameters are invalid. Broker uri or host/port are required");
            }
        } else {
            log.error("RabbitMQ connection parameters are invalid.");
            throw new Exception("RabbitMQ connection parameters are invalid.\n");
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

    private void setConnection() throws JMSException {

        if(context.getRegistry().lookupByName(connectionId) == null) {

            log.info("Create new rabbitMQ Connection with connection-id: " + connectionId);

            rabbitMQConnectionFactory = new CachingConnectionFactory();

            if (uri != null) {
                rabbitMQConnectionFactory.setUri(uri);
            } else if (host != null && port != null) {
                rabbitMQConnectionFactory.setHost(host);
                rabbitMQConnectionFactory.setPort(Integer.parseInt(port));
            } else {
                rabbitMQConnectionFactory.setHost("localhost");
                rabbitMQConnectionFactory.setPort(5672);
            }

            if (virtualHost != null) {
                rabbitMQConnectionFactory.setVirtualHost(virtualHost);
            } else {
                rabbitMQConnectionFactory.setVirtualHost("/");
            }

            if (username != null && password != null) {
                rabbitMQConnectionFactory.setUsername(username);
                rabbitMQConnectionFactory.setPassword(password);
            }

            rabbitMQConnectionFactory.start();

            context.getRegistry().bind(connectionId, rabbitMQConnectionFactory);
        }else{
            log.info("Reuse RabbitMQ Connection with connection-id: " + connectionId);
        }

    }

}
