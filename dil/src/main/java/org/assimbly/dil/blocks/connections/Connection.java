package org.assimbly.dil.blocks.connections;

import org.apache.camel.CamelContext;
import org.apache.commons.collections4.MapUtils;
import org.assimbly.dil.blocks.connections.broker.*;
import org.assimbly.dil.blocks.connections.database.JDBCConnection;
import org.assimbly.util.EncryptionUtil;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class Connection {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String uri;
	private String connectionId;
	private String connectionIdValue;
	private String flowId;
	private final TreeMap<String, String> properties;
	private final String key;
	private final CamelContext context;
	private String stepType;
    private Object stepId;
    private String connectionType;
    private static final Random RANDOM = new Random();

	public Connection(CamelContext context, TreeMap<String, String> properties, String key) {
		this.context = context;
		this.properties = properties;
		this.key = key;
	}

	public void start() throws Exception{

        connectionId = properties.get(key);
        connectionType = properties.get("connection." + connectionId + ".type" );

        stepType = key.split("\\.")[0]; 
        stepId = key.split("\\.")[1]; 

        connectionIdValue = properties.get(stepType + "." + stepId + ".connection.id");
        flowId = properties.get("id");

        if(connectionIdValue!=null) {
            startConnection();        
        }

	}

	private void startConnection() throws Exception{

        EncryptableProperties decryptedProperties = decryptProperties(properties);

        if(connectionType==null){
            connectionType = "unconfigured";
        }

        switch (connectionType.toLowerCase()) {
            case "activemq":
                new ActiveMQConnection(context, decryptedProperties, connectionId, "activeMQ").start();
                break;
            case "amazonmq":
                new ActiveMQConnection(context, decryptedProperties, connectionId, "amazonmq").start();
                break;
            case "sonicmq":
                String connectId = stepType + connectionIdValue + RANDOM.nextInt(1000000);
                new SonicMQConnection(context, decryptedProperties, connectionId, "sonicmq").start(flowId, connectId, connectionIdValue);
                uri = uri.replace("sonicmq:", "sonicmq." + flowId + connectId + ":");
                properties.put(stepType + "." + stepId + ".uri", uri);						
                break;
            case "mq":
                new MQConnection(context, decryptedProperties, connectionId, "sjms").start();
                break;
            case "amqps":
                new AMQPConnection(context, decryptedProperties, connectionId, "amqps").start(true);
                break;
            case "amqp":
                new AMQPConnection(context, decryptedProperties, connectionId, "amqp").start(false);
                break;
            case "ibmq":
                new IBMMQConnection(context, decryptedProperties, connectionId, "ibmmq").start();
                break;
            case "rabbitmq", "spring-rabbitmq":
                new RabbitMQConnection(context, decryptedProperties, connectionId, "spring-rabbitmq").start();
                break;
            case "jdbc":
                new JDBCConnection(context, decryptedProperties, connectionId).start(stepType, stepId);
                break;
            default:
                log.error("Connection parameters for connection " + connectionType + " are not implemented");
                throw new IllegalArgumentException("Connection parameters for connection " + connectionType + " are not implemented");
        }

	}

    private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {

        EncryptableProperties encryptableProperties = (EncryptableProperties) context.getRegistry().lookupByName("encryptableProperties");
        EncryptionUtil encryptionUtil = (EncryptionUtil) context.getRegistry().lookupByName("encryptionUtil");

        for (Map.Entry<String,String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if(isEncodedString(value)){
                value = encryptionUtil.decrypt(value);
            }

            MapUtils.safeAddToMap(encryptableProperties, key, value);
        }

        return encryptableProperties;

    }

    private boolean isEncodedString(String input) {
        return input.startsWith("ENC(") && input.endsWith(")");
    }

}