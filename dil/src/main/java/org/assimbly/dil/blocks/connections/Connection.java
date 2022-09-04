package org.assimbly.dil.blocks.connections;

import org.apache.camel.CamelContext;
import org.apache.camel.component.properties.PropertiesComponent;
import org.assimbly.dil.blocks.connections.broker.*;
import org.assimbly.dil.blocks.connections.database.JDBCConnection;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class Connection {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String uri;
	private String connectionId;
	private String connectionIdValue;
	private String flowId;
	private TreeMap<String, String> properties;
	private String key;
	private CamelContext context;
	private String connectId;
	private String stepType;
    private Object stepId;

    private String connectionType;

	public Connection(CamelContext context, TreeMap<String, String> properties, String key) {
		this.context = context;
		this.properties = properties;
		this.key = key;
	}

	public TreeMap<String, String> start() throws Exception{

        connectionId = properties.get(key);
        connectionType = properties.get("connection." + connectionId + ".type" );

        stepType = key.split("\\.")[0]; 
        stepId = key.split("\\.")[1]; 

        connectionIdValue = properties.get(stepType + "." + stepId + ".connection.id");
        flowId = properties.get("id");

        if(connectionIdValue!=null) {
            startConnection();        
        }


		return properties;

	}

	private void startConnection() throws Exception{

        EncryptableProperties decryptedProperties = decryptProperties(properties);

        switch (connectionType) {
            case "ActiveMQ":
                new ActiveMQConnection(context, decryptedProperties, connectionId, "activeMQ").start();
                break;
            case "AmazonMQ":
                new ActiveMQConnection(context, decryptedProperties, connectionId, "amazonmq").start();
                break;
            case "SonicMQ":
                connectId = stepType + connectionIdValue + new Random().nextInt(1000000);
                new SonicMQConnection(context, decryptedProperties, connectionId, "sonicmq").start(flowId, connectId, connectionIdValue);
                uri = uri.replace("sonicmq:", "sonicmq." + flowId + connectId + ":");
                properties.put(stepType + "." + stepId + ".uri", uri);						
                break;
            case "MQ":
                new MQConnection(context, decryptedProperties, connectionId, "sjms").start(stepType, stepId);
                break;
            case "AMQPS":
                new AMQPConnection(context, decryptedProperties, connectionId, "amqps").start(true);
                break;
            case "AMQP":
                new AMQPConnection(context, decryptedProperties, connectionId, "amqp").start(false);
                break;
            case "IBMMQ":
                new IBMMQConnection(context, decryptedProperties, connectionId, "ibmmq").start(stepType, stepId);
                break;
            case "JDBC":
                new JDBCConnection(context, decryptedProperties, connectionId, "sql").start(stepType, stepId);
                break;
            default:
                log.error("Connection parameters for connection " + connectionType + " are not implemented");
                throw new Exception("Connection parameters for connection " + connectionType + " are not implemented");
        }

	}

    private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {
        EncryptableProperties decryptedProperties = (EncryptableProperties) ((PropertiesComponent) context.getPropertiesComponent()).getInitialProperties();
        decryptedProperties.putAll(properties);
        return decryptedProperties;
    }

}