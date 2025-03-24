package org.assimbly.dil.blocks.connections.broker;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.ClassicJmsHeaderFilterStrategy;
import org.apache.camel.component.jms.JmsComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String componentName;
    private final String connectionId;
    private String url;
    private String username;
    private String password;
    private String jmsProvider;

    public MQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }

    public void start() throws Exception {

        setFields();

        log.info("Setting up sjms client connection for " + jmsProvider);

        if (url != null) {
            setConnection();
        }else{
            throw new Exception("Unknown url. Broker url is required");
        }
    }

    private void setFields(){

        url = properties.getProperty("connection." + connectionId + ".url");
        jmsProvider = properties.getProperty("connection." + connectionId + ".jmsprovider");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }


    private void setConnection() throws Exception {

        if(jmsProvider.equalsIgnoreCase("AMQ") || jmsProvider.equalsIgnoreCase("ActiveMQ Artemis")){
            startActiveMQArtemisConnection();
        }else if (jmsProvider.equalsIgnoreCase("ActiveMQ Classic") || jmsProvider.equalsIgnoreCase("AmazonMQ")){
            startActiveMQClassicConnection();
        }else{
            throw new Exception("Unknown jms provider (valid are ActiveMQ Classic, AmazonMQ ActiveMQ Artemis, AMQ).\n");
        }

    }

    private void startActiveMQArtemisConnection() {

        org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory cf;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url);
        } else {
            cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url, username, password);
        }
        cf.setConnectionTTL(-1);
        cf.setReconnectAttempts(-1);
        cf.setRetryInterval(1000);
        cf.setRetryIntervalMultiplier(2.0);
        cf.setMaxRetryInterval(3600000);

        createJmsComponent((ConnectionFactory) cf);

    }

    private void startActiveMQClassicConnection() {

        ActiveMQConnectionFactory cf;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            cf = new ActiveMQConnectionFactory(url);
        } else {
            cf = new ActiveMQConnectionFactory(username, password, url);
        }

        createJmsComponent(cf);

    }

    private void createJmsComponent(ConnectionFactory connectionFactory) {

        if (context.hasComponent(componentName) != null) {
            context.removeComponent(componentName);
        }

        PooledConnectionFactory pooledConnectionFactory = createPooledConnectionFactory(connectionFactory);

        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(pooledConnectionFactory);
        jmsComponent.setHeaderFilterStrategy(new ClassicJmsHeaderFilterStrategy());
        jmsComponent.setIncludeCorrelationIDAsBytes(false);
        jmsComponent.setConcurrentConsumers(10);
        jmsComponent.setArtemisStreamingEnabled(true);

        this.context.addComponent(componentName, jmsComponent);

    }

    private PooledConnectionFactory createPooledConnectionFactory(ConnectionFactory connectionFactory) {

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        pooledConnectionFactory.setCreateConnectionOnStartup(true);
        pooledConnectionFactory.setMaxConnections(500);
        pooledConnectionFactory.setMaximumActiveSessionPerConnection(100);
        pooledConnectionFactory.setIdleTimeout(30000);

        return pooledConnectionFactory;

    }

}
