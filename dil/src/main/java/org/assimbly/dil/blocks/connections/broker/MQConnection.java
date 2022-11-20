package org.assimbly.dil.blocks.connections.broker;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.component.activemq.ActiveMQComponent;
import org.apache.camel.component.activemq.ActiveMQConfiguration;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;

    private String url;
    private String username;
    private String password;
    private String jmsProvider;

    private SjmsComponent sjmsComponent;

    public MQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;

    }


    public void start(String direction, Object stepId) throws Exception {

        setFields(direction, stepId);

        log.info("Setting up sjms client connection for " + jmsProvider);

        if (url != null) {
            setConnection();
        }else{
            throw new Exception("Unknown url. Broker url is required");
        }
    }

    private void setFields(String direction, Object stepId){

        url = properties.getProperty("connection." + connectionId + ".url");
        jmsProvider = properties.getProperty("connection." + connectionId + ".jmsprovider");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

    }


    private void setConnection() throws Exception {

        if(jmsProvider.equalsIgnoreCase("AMQ") || jmsProvider.equalsIgnoreCase("ActiveMQ Artemis")){
            startActiveMQArtemisConnection();
        }else if (jmsProvider.equalsIgnoreCase("ActiveMQ Classic")){
            startActiveMQClassicConnection();
        }else{
            throw new Exception("Unknown jms provider (valid are ActiveMQ Classic, AcitveMQ Artemis, AMQ).\n");
        }

    }

    private void startActiveMQArtemisConnection(){

        org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory cf = null;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url);
            cf.setConnectionTTL(-1);
            cf.setReconnectAttempts(-1);
            cf.setRetryInterval(1000);
            cf.setRetryIntervalMultiplier(2.0);
            cf.setMaxRetryInterval(3600000);
        } else {
            cf = new org.apache.activemq.artemis.jms.client.ActiveMQJMSConnectionFactory(url, username, password);
            cf.setConnectionTTL(-1);
            cf.setReconnectAttempts(-1);
            cf.setRetryInterval(1000);
            cf.setRetryIntervalMultiplier(2.0);
            cf.setMaxRetryInterval(3600000);
        }

        if (context.hasComponent(componentName) == null) {
            sjmsComponent = new SjmsComponent();
            sjmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, sjmsComponent);
        } else {
            context.removeComponent(componentName);
            sjmsComponent = new SjmsComponent();
            sjmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, sjmsComponent);
        }

    }

    private void startActiveMQClassicConnection(){

        ActiveMQConnectionFactory cf = null;

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            cf = new ActiveMQConnectionFactory(url);
        } else {
            cf = new ActiveMQConnectionFactory(username, password, url);
        }

        if (context.hasComponent(componentName) == null) {
            sjmsComponent = new SjmsComponent();
            sjmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, sjmsComponent);
        } else {
            context.removeComponent(componentName);
            sjmsComponent = new SjmsComponent();
            sjmsComponent.setConnectionFactory(cf);
            context.addComponent(componentName, sjmsComponent);
        }

    }

}
