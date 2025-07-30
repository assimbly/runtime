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
import java.util.TreeMap;

public class Connection {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	private String connectionId;
	private final TreeMap<String, String> properties;
	private final String key;
	private final CamelContext context;
	private String stepType;
    private Object stepId;
    private String connectionType;

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

        String connectionIdValue = properties.get(stepType + "." + stepId + ".connection.id");

        if(connectionIdValue!=null) {
            startConnection();        
        }

	}

	private void startConnection() throws Exception{

        EncryptableProperties decryptedProperties = decryptProperties(properties);

        if(connectionType==null){
            connectionType = "unconfigured";
        }else{
            connectionType = connectionType.toLowerCase();
        }

        switch (connectionType) {

            case "activemq", "amazonmq", "jms", "sjms", "sjms2" ->
                    new JMSConnection(context, decryptedProperties, connectionId, connectionType).start();

            case "amqp", "amqps" ->
                    new AMQPConnection(context, decryptedProperties, connectionId, connectionType).start();

            case "rabbitmq", "spring-rabbitmq" ->
                    new RabbitMQConnection(context, decryptedProperties, connectionId, "spring-rabbitmq").start();

            case "ibmq" ->
                    new IBMMQConnection(context, decryptedProperties, connectionId, connectionType).start();

            case "jdbc" ->
                    new JDBCConnection(context, decryptedProperties, connectionId).start(stepType, stepId);

            default -> throw new IllegalArgumentException("Connection parameters for connection " + connectionType + " are not implemented");

        }

	}

    private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {

        EncryptableProperties encryptableProperties = (EncryptableProperties) context.getRegistry().lookupByName("encryptableProperties");
        EncryptionUtil encryptionUtil = (EncryptionUtil) context.getRegistry().lookupByName("encryptionUtil");

        for (Map.Entry<String,String> entry : properties.entrySet()) {
            String propertyKey = entry.getKey();
            String propertyValue = entry.getValue();
            if(isEncodedString(propertyValue)){
                propertyValue = encryptionUtil.decrypt(propertyValue);
            }

            MapUtils.safeAddToMap(encryptableProperties, propertyKey, propertyValue);
        }

        return encryptableProperties;

    }

    private boolean isEncodedString(String input) {
        return input.startsWith("ENC(") && input.endsWith(")");
    }

}