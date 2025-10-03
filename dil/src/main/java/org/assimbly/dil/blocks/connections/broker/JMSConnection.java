
package org.assimbly.dil.blocks.connections.broker;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jms.ClassicJmsHeaderFilterStrategy;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms2.Sjms2Component;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JMSConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;
    private final String componentName;
    private String url;
    private String username;
    private String password;
    private String jmsProvider;
    private String pooled;

    public JMSConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }

    public void start() throws Exception {

        setFields();

        if(checkConnection()){
            log.info("Creating new {} client connection with id={}", componentName, connectionId);
            setConnection();
        }else{
            log.info("Reusing {} client connection with id={}", componentName, connectionId);
        }

    }

    private void setFields(){

        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");
        pooled = properties.getProperty("connection." + connectionId + ".pooled");
        jmsProvider = properties.getProperty("connection." + connectionId + ".jmsprovider");

    }

    private boolean checkConnection() {

        Object isRegistered = context.getRegistry().lookupByName(connectionId);

        if(isRegistered != null){
            return false;
        }

        if (url == null) {
            throw new IllegalArgumentException(componentName + " connection parameters are invalid. Broker url is required");
        }

        if (jmsProvider == null) {
            throw new IllegalArgumentException(componentName + " connection parameters are invalid. JMS Provider is required");
        }

        return true;

    }

    private void setConnection() throws Exception {

        ConnectionFactory connectionFactory = setConnectionFactory();

        if(pooled != null && pooled.equalsIgnoreCase("true")) {
            connectionFactory = createPooledConnectionFactory(connectionFactory);
        }

        switch (componentName) {
            case "activemq", "amazonmq", "jms" -> createJmsComponent(connectionFactory);
            case "sjms" -> createSjmsComponent(connectionFactory);
            case "sjms2" -> createSjms2Component(connectionFactory);
            default -> throw new Exception("Unknown component name: " + componentName);
        }

    }

    private ConnectionFactory setConnectionFactory() {

        ConnectionFactory connectionFactory;

        if (jmsProvider.equalsIgnoreCase("amq") || jmsProvider.equalsIgnoreCase("artemis") || jmsProvider.equalsIgnoreCase("activemq artemis")) {
            connectionFactory = setActiveMQArtemisConnectionFactory();
        } else {
            connectionFactory = setActiveMQClassicConnectionFactory();
        }

        return connectionFactory;

    }

    private ConnectionFactory setActiveMQArtemisConnectionFactory() {

        org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory connectionFactory;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            connectionFactory = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(url);
        } else {
            connectionFactory = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory(url, username, password);
        }

        connectionFactory.setConnectionTTL(-1);
        connectionFactory.setReconnectAttempts(1);
        connectionFactory.setRetryInterval(3000);
        connectionFactory.setMaxRetryInterval(1); // Disable reconnection
        connectionFactory.setRetryIntervalMultiplier(2.0);

        return connectionFactory;

    }

    private ConnectionFactory setActiveMQClassicConnectionFactory() {

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);

        if (username != null && !username.isEmpty()){
            connectionFactory.setUserName(username);
        }

        if(password != null && !password.isEmpty()) {
            connectionFactory.setPassword(password);
        }

        return connectionFactory;

    }

    private PooledConnectionFactory createPooledConnectionFactory(ConnectionFactory connectionFactory) {

        log.info("Setting pooled connectionFactory for {}", componentName);

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        pooledConnectionFactory.setCreateConnectionOnStartup(true);
        pooledConnectionFactory.setBlockIfSessionPoolIsFull(true);
        pooledConnectionFactory.setMaxConnections(500);
        pooledConnectionFactory.setMaximumActiveSessionPerConnection(500);
        pooledConnectionFactory.setIdleTimeout(30000);

        return pooledConnectionFactory;

    }

    private void createJmsComponent(ConnectionFactory connectionFactory) {

        JmsComponent jmsComponent = context.getComponent(componentName, JmsComponent.class);


        if(jmsComponent == null){
            System.out.println("----> Yes it's null");
        }else{
            System.out.println("----> No it's not null");
        }

        if(jmsComponent != null){
            jmsComponent.setHeaderFilterStrategy(new ClassicJmsHeaderFilterStrategy());
            jmsComponent.setIncludeCorrelationIDAsBytes(false);
            jmsComponent.setConcurrentConsumers(10);
            jmsComponent.setMaxConcurrentConsumers(50);
            jmsComponent.setArtemisStreamingEnabled(true);
            jmsComponent.setTestConnectionOnStartup(true);
        }

        context.getRegistry().bind(connectionId, connectionFactory);

    }

    private void createSjmsComponent(ConnectionFactory connectionFactory) {

        SjmsComponent sjmsComponent = context.getComponent(componentName, SjmsComponent.class);
        sjmsComponent.setHeaderFilterStrategy(new ClassicJmsHeaderFilterStrategy());

        context.getRegistry().bind(connectionId, connectionFactory);

    }

    private void createSjms2Component(ConnectionFactory connectionFactory) {

        Sjms2Component sjms2Component = context.getComponent(componentName, Sjms2Component.class);
        sjms2Component.setHeaderFilterStrategy(new ClassicJmsHeaderFilterStrategy());

        context.getRegistry().bind(connectionId, connectionFactory);

    }

}
