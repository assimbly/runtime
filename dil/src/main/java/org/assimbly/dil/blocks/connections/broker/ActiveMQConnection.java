package org.assimbly.dil.blocks.connections.broker;

import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
//import org.apache.camel.component.activemq.ActiveMQComponent;
//import org.apache.camel.component.activemq.ActiveMQConfiguration;
import org.apache.camel.component.jms.JmsComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ActiveMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;
    private ActiveMQConnectionFactory activeMQConnectionFactory;
    private String url;
    private String username;
    private String password;
    private String conType;
    private String maxConnections;
    private String concurentConsumers;

    public ActiveMQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;

    }


    public void start() throws Exception {

        log.info("Setting up jms client connection for ActiveMQ.");

        setFields();

        if (context.hasComponent(componentName) == null) {
            if (url != null) {
                setConnection();
            }else{
                throw new Exception("Unknown url. Broker url is required");
            }
        } else {
            log.error("ActiveMQ connection parameters are invalid.");
            throw new Exception("ActiveMQ connection parameters are invalid.\n");
        }
    }

    private void setFields(){

        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");//properties.get("connection." + connectionId + ".password");

        conType = properties.getProperty("connection." + connectionId + ".conType");
        maxConnections = properties.getProperty("connection." + connectionId + "connection.maxConnections");
        concurentConsumers = properties.getProperty("connection." + connectionId + "connection.concurentConsumers");

        if (conType == null) {
            log.info("No connection type specified. Setting up basic connection for activemq.");
            conType = "basic";
        } else if (!conType.equals("basic") && !conType.equals("pooled")) {
            log.info("Invalid connection type specified. Setting up basic connection for activemq.");
            conType = "basic";
        } else if (conType.equals("pooled")) {

            if (maxConnections == null) {
                maxConnections = "10";
            }
            if (concurentConsumers == null) {
                concurentConsumers = "10";
            }

        }
    }



    private void setConnection() throws JMSException {

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            activeMQConnectionFactory = new ActiveMQConnectionFactory(url);
        } else {
            activeMQConnectionFactory = new ActiveMQConnectionFactory(username, password, url);
        }

        if (conType.equals("basic")) {
            //startBasicConnection();
        } else {
            //startPooledConnection();
        }

    }

    /*
    private void startBasicConnection() throws JMSException {

        org.apache.activemq.ActiveMQConnection connection = (org.apache.activemq.ActiveMQConnection) activeMQConnectionFactory.createConnection();
        connection.start();

        context.addComponent(componentName, JmsComponent.jmsComponentAutoAcknowledge(activeMQConnectionFactory));
        log.info("Started basic connection for ActiveMQ.");

    }

    private void startPooledConnection(){

        try {
            PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
            pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory);
            pooledConnectionFactory.setMaxConnections(Integer.parseInt(maxConnections));

            ActiveMQConfiguration configuration = new ActiveMQConfiguration();

            configuration.setConnectionFactory(pooledConnectionFactory);
            configuration.setConcurrentConsumers(Integer.parseInt(concurentConsumers));
            configuration.setUsePooledConnection(true);

            //ActiveMQComponent component = new ActiveMQComponent(configuration);
            //context.addComponent(componentName, component);
            log.info("Started pooled connection for ActiveMQ.");
            log.info("Maximum connections: " + maxConnections + " - concurentConsumers: " + concurentConsumers);
        } catch (Exception e) {
            log.error("Failed to start pooled connection. Reason:", e);
        }

    }
    */

}
