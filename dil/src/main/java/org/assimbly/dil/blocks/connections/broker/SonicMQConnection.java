package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.apache.camel.component.sjms.SjmsComponent;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import progress.message.jclient.ConnectionFactory;

import jakarta.jms.JMSException;

//to do in Jakarta/Camel4 migration (does SonicMQ support jakarta?)

public class SonicMQConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private CamelContext context;
    private EncryptableProperties properties;
    private String componentName;
    private String connectionId;
    private String url;
    private String username;
    private String password;
    private boolean faultTolerant;

    public SonicMQConnection(CamelContext context, EncryptableProperties properties, String connectionId, String componentName) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
        this.componentName = componentName;
    }

    public void start(String flowId, String connectId, String connectionIdValue) throws Exception {

        log.info("Setting up jms client connection for ActiveMQ.");

        setFields(flowId, connectId);

        if (context.hasComponent(componentName) == null) {
            if (url != null) {
                setConnection(flowId, connectId, connectionIdValue);
            }else{
                throw new Exception("Unknown url. Broker url is required");
            }
        } else {
            log.error("ActiveMQ connection parameters are invalid.");
            throw new Exception("ActiveMQ connection parameters are invalid.\n");
        }
    }

    private void setFields(String flowId, String connectId) throws Exception {

        componentName = "sonicmq." + flowId + connectId;
        url = properties.getProperty("connection." + connectionId + ".url");
        username = properties.getProperty("connection." + connectionId + ".username");
        password = properties.getProperty("connection." + connectionId + ".password");

        if (properties.getProperty("connection." + connectionId + ".faultTolerant") != null) {
            try {
                Boolean.parseBoolean(properties.getProperty("connection." + connectionId + ".faultTolerant"));
            } catch (Exception e) {
                faultTolerant = true;
            }
        } else {
            faultTolerant = true;
        }

        if (url == null || username == null || password == null) {
            log.error("SonicMQ connection parameters are invalid or missing");
            if (url == null) {
                log.error("SonicMQ connection required parameter 'url' isn't set");
            }
            if (username == null) {
                log.error("SonicMQ connection required parameter 'username' isn't set");
            }
            if (password == null) {
                log.error("SonicMQ connection required parameter 'password' isn't set");
            }
            throw new Exception("SonicMQ connection parameters are invalid or missing.\n");
        }

    }



    private void setConnection(String flowId, String connectId, String connectionIdValue) throws JMSException, javax.jms.JMSException {

        ConnectionFactory connection = new ConnectionFactory(url, username, password);
        connection.setConnectID("Assimbly/Gateway/" + connectionIdValue + "/Flow/" + flowId + "/" + connectId);
        connection.setPrefetchCount(10);
        connection.setReconnectInterval(60);
        connection.setFaultTolerant(faultTolerant);
        connection.setFaultTolerantReconnectTimeout(3600);
        connection.setInitialConnectTimeout(15);

        log.info("Connecting to SonicMQ broker (connection time is set to 15 seconds)");

        SjmsComponent jms = new SjmsComponent();
        jms.setConnectionFactory((jakarta.jms.ConnectionFactory) connection);
        //jms.setConnectionClientId("Assimbly/Gateway/" + connectionIdValue + "/Flow/" + flowId + "/" + connectId);
        jms.setCamelContext(context);
        jms.start();

        context.addComponent(componentName, jms);

    }

}
