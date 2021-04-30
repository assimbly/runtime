package org.assimbly.broker;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Arrays.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.management.impl.ActiveMQServerControlImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.ConnectorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;

import static java.util.stream.Collectors.toList;

public class ActiveMQArtemis implements Broker {

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.broker.BrokerArtemis");

	EmbeddedActiveMQ broker;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

	File brokerFile = new File(baseDir + "/broker/broker.xml");
	private ActiveMQServerControlImpl manageBroker;

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}

	//See docs https://activemq.apache.org/components/artemis/documentation/javadocs/javadoc-latest/org/apache/activemq/artemis/api/core/management/QueueControl.html

	public String start() throws Exception {

		broker = new EmbeddedActiveMQ();

		if(brokerFile.exists()) {
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			logger.info("Using config file 'broker.xml'. Loaded from " + brokerFile.getAbsolutePath());
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			broker.setConfigResourcePath(fileConfig);
		}else {
			
			this.setFileConfiguration("");
			logger.warn("No config file 'broker.xml' found.");
			logger.info("Created default 'broker.xml' stored in following directory: " + baseDir + "/broker");			
			logger.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
			logger.info("");
			logger.info("Start broker in local mode on url: tcp://127.0.0.1:61616");
			
			String fileConfig = "file:///" + brokerFile.getAbsolutePath();
			broker.setConfigResourcePath(fileConfig);
		}		
		
		broker.start();

		setManageBroker();

		return status();
	}


	public String startEmbedded() throws Exception {

			logger.warn("Start embedded broker in local mode on url: tcp://127.0.0.1:61616");

			Configuration config = new ConfigurationImpl();
			config.addAcceptorConfiguration("in-vm", "vm://0");
			config.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616");
			config.setSecurityEnabled(false);

			broker = new EmbeddedActiveMQ();
			broker.setConfiguration(config);
			broker.start();

			return status();
	}

	
	public String stop() throws Exception {
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		
		if(activeBroker!=null) {
			SimpleString nodeID= activeBroker.getNodeID();
			logger.info("Broker with nodeId '" + nodeID + "' is stopping. Uptime=" + activeBroker.getUptime());
			broker.stop();
			logger.info("Broker with nodeId '" + nodeID + "' is stopped.");
		}
		
		return status();
		
	}

	public String restart() throws Exception {
		this.stop();
		this.start();
		
		return status();
	}

	
	public String restartEmbedded() throws Exception {
		this.stop();
		this.startEmbedded();
		
		return status();
		
	}	
	
	public String status() throws Exception {
		String status = "stopped";
		if(broker==null) {
			broker = new EmbeddedActiveMQ();
		}
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		if(activeBroker!=null) {
			if(activeBroker.isActive()) {
				status = "started";	
			}		
		}		
		return status;
	}

	public String info() throws Exception {
		
		if(status().equals("started")) {
			ActiveMQServer activeBroker = broker.getActiveMQServer();
			String info = "uptime="+ activeBroker.getUptime() 
					 + ",totalConnections=" + activeBroker.getTotalConnectionCount()
					 + ",totalConsumers=" + activeBroker.getTotalConsumerCount()
					 + ",totalMessages=" + activeBroker.getTotalMessageCount()
					 + ",nodeId=" + activeBroker.getNodeID()
					 + ",state=" + activeBroker.getState()
					 + ",version=" + activeBroker.getVersion().getFullVersion()
					 + ",type=ActiveMQ Artemis";
			return info;
		}else {
			return "no info. broker not running";
		}
		
		
	}
	
	
	public String getFileConfiguration() throws IOException {

		if(!brokerFile.exists()) {
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			
    		try {
    			FileUtils.touch(brokerFile);
    			InputStream is = classloader.getResourceAsStream("broker.xml");
    			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        		is.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    	
		return FileUtils.readFileToString(brokerFile, StandardCharsets.UTF_8);
					
	}
	
	public String setFileConfiguration(String brokerConfiguration) throws IOException {
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		
		if(brokerFile.exists() || !brokerConfiguration.isEmpty()) {

			URL schemaFile = classloader.getResource("broker.xsd");
			String xmlValidation = ConnectorUtil.isValidXML(schemaFile, brokerConfiguration);
			if(!xmlValidation.equals("xml is valid")) {
				return xmlValidation;
			} 
			FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
		}else {
			FileUtils.touch(brokerFile);
			InputStream is = classloader.getResourceAsStream("broker.xml");
			Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}

		return "configuration set";
	}

	/*
	public String deleteTopic(String topicName) throws Exception {
		manageBroker.deleteAddress(topicName,true);
		return "success";
	}

	public String createTopic(String topicName) throws Exception {
		manageBroker.createQueue(topicName, "MULTICAST", topicName,"",true,-1,false,true );
		return "success";
	}
	*/

	public String createQueue(String queueName) throws Exception {
		manageBroker.createQueue(queueName, "ANYCAST", queueName,"",true,-1,false,true );
		return "success";
	}

	public String deleteQueue(String queueName) throws Exception {
		manageBroker.deleteAddress(queueName,true);
		return "success";
	}

	//manageBroker.getAddressSettingsAsJSON(queue);

	public String getQueue(String queueName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		String name = "\"name\": \"" + queueName + "\"";
		String address = "\"address\": \"" + queueControl.getAddress() + "\"";
		String routingType = "\"routingType\": \"" + queueControl.getRoutingType() + "\"";
		String durable = "\"durable\": " + queueControl.isDurable() + "\"";
		String exclusive = "\"exclusive\": " + queueControl.isExclusive() + "\"";
		String temporary = "\"temporary\": \"" + queueControl.isTemporary() + "\"";
		String numberOfMessages = "\"numberOfMessages\": \"" + queueControl.countMessages() + "\"";
		String numberOfConsumers = "\"numberOfConsumers\": \"" + queueControl.getConsumerCount() + "\"";

		String queueInfo = "{\n" + name + ",\n" + address + ",\n" + routingType + ",\n" + durable + ",\n" + exclusive + ",\n" + temporary + ",\n" + numberOfMessages + ",\n" + numberOfConsumers + "\n}";

		return queueInfo;
	}

	public String getQueues() throws Exception {

		String queuesInfo = "";
		String[] queues = manageBroker.getQueueNames();

		for(String queue: queues){
			if(queuesInfo.isEmpty()){
				queuesInfo = getQueue(queue);
			}else{
				queuesInfo = queuesInfo + "," + getQueue(queue);
			}
		}

		queuesInfo = "{\n" + queuesInfo + "\n}";

		return queuesInfo;
	}

	public String clearQueue(String queueName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();
		Queue queue = activeBroker.locateQueue(new SimpleString(queueName));
		if (queue != null) {
			queue.deleteAllReferences();
		}

		return "success";
	}

	public String clearQueues() throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		for (String queueName : manageBroker.getQueueNames()) {
			Queue queue = activeBroker.locateQueue(new SimpleString(queueName));
			if (queue != null) {
				queue.deleteAllReferences();
			}
		}

		return "success";
	}

	public String moveMessage(String sourceQueueName, String targetQueueName, String messageId) throws Exception {
		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + sourceQueueName);

		boolean result = queueControl.moveMessage(Long.parseLong(messageId),targetQueueName);

		return Boolean.toString(result);

	}

	public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + sourceQueueName);

		int result = queueControl.moveMessages("", targetQueueName);

		return Integer.toString(result);

	}

	public String removeMessage(String queueName, int messageId) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		boolean result = queueControl.removeMessage(messageId);

		return Boolean.toString(result);

	}


	public String removeMessages(String queueName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		int result = queueControl.removeAllMessages();

		return Integer.toString(result);

	}

	public String listMessages(String queueName, String filter) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		String messages = queueControl.listMessagesAsJSON(filter);

		return messages;

	}

	public String browseMessage(String queueName, String messageId) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		CompositeData[] messages = queueControl.browse();

		/*
		for(CompositeData message: messages) {
			System.out.println("converting");
			Object messageObject = convertToTrueObject(message);
			if(messageObject instanceof Message){
				System.out.println("ja een message");
				System.out.println("MessageID=" + ((Message) messageObject).getMessageID());
			}
		}*/

		Object[] x = stream(messages).filter(compositeData -> compositeData.get("messageID").equals(messageId)).toArray();

		System.out.println("name=" + x.getClass().getName());
		System.out.println("ttypename=" + x.getClass().getTypeName());
		System.out.println("lenght=" + x.length);

		CompositeData[] messages2 = stream(messages).filter(compositeData -> compositeData.get("messageID").equals(messageId)).toArray(CompositeData[]::new);

		System.out.println("x");
		String result = CompositeDataConverter.convertToJSON(messages2);
		System.out.println("x2");

		//String result = "done";

		return result;

	}


	private static Object convertToTrueObject(CompositeData compositeData) {
		CompositeType type = compositeData.getCompositeType();
		try {
			Class<?> _class = Class.forName(type.getTypeName());
			Method method = _class.getMethod("from", CompositeData.class);
			if (Modifier.isStatic(method.getModifiers())
					&& method.getReturnType() == _class) {
				return method.invoke(null, compositeData);
			}
			return null;
		} catch (ClassNotFoundException | NoSuchMethodException
				| InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public String browseMessages(String endpointName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		CompositeData[] messages = queueControl.browse();

		String result = CompositeDataConverter.convertToJSON(messages);

		return result;

	}


	public String sendMessage(String queueName, Map<String,String> messageHeaders, String messageBody) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + queueName);

		String result = queueControl.sendMessage(messageHeaders, 0, messageBody, true, null, null);

		return result;

	}

	public String getConsumers() throws Exception {
		String consumers = manageBroker.listAllConsumersAsJSON();
		return consumers;
	}

	public String getConnections() throws Exception {
		String connections = manageBroker.listConnectionsAsJSON();
		return connections;
	}

	@Override
	public Object getBroker() throws Exception {
		return broker;
	}

	private void setManageBroker(){
		ActiveMQServer activeBroker = broker.getActiveMQServer();
		ActiveMQServerControlImpl activeBrokerControl = activeBroker.getActiveMQServerControl();

		manageBroker = activeBrokerControl;
	}

}
