package org.assimbly.dil.blocks.connections.broker;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ActiveMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String componentName;
    private final String connectionId;
    private String url;
    private String username;
    private String password;

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
        password = properties.getProperty("connection." + connectionId + ".password");

        String conType = properties.getProperty("connection." + connectionId + ".conType");

        if (conType == null) {
            log.info("No connection type specified. Setting up basic connection for activemq.");
        } else if (!conType.equals("basic") && !conType.equals("pooled")) {
            log.info("Invalid connection type specified. Setting up basic connection for activemq.");
        }
    }



    private void setConnection() {

        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            new ActiveMQConnectionFactory(url);
        } else {
            new ActiveMQConnectionFactory(username, password, url);
        }

    }

}
