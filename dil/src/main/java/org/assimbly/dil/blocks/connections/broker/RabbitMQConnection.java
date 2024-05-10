package org.assimbly.dil.blocks.connections.broker;

import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.activemq.ActiveMQConfiguration;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.Connection;


public class RabbitMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;
    private CachingConnectionFactory rabbitMQConnectionFactory;
    private String url;
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
            if (url != null) {
                setConnection();
            }else{
                throw new Exception("Unknown url. Broker url is required");
            }
        } else {
            log.error("RabbitMQ connection parameters are invalid.");
            throw new Exception("RabbitMQ connection parameters are invalid.\n");
        }
    }

    private void setFields(){

        url = properties.getProperty("connection." + connectionId + ".url");
        virtualHost = properties.getProperty("connection." + connectionId + ".vhost");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }



    private void setConnection() throws JMSException {

        rabbitMQConnectionFactory.setUri(url);
        rabbitMQConnectionFactory.setVirtualHost(virtualHost);
        if(username!=null && password != null) {
            rabbitMQConnectionFactory.setUsername(username);
            rabbitMQConnectionFactory.setPassword(password);
        }

        //Connection connection = rabbitMQConnectionFactory.createConnection();
        rabbitMQConnectionFactory.start();

        context.getRegistry().bind(connectionId,rabbitMQConnectionFactory);

    }

}
