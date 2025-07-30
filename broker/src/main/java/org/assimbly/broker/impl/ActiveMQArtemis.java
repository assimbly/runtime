package org.assimbly.broker.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.management.impl.ActiveMQServerControlImpl;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.commons.io.FileUtils;
import org.assimbly.broker.Broker;
import org.assimbly.broker.converter.CompositeDataConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.assimbly.util.OSUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.openmbean.CompositeData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

public class ActiveMQArtemis implements Broker {

	static final Logger log = LoggerFactory.getLogger(ActiveMQArtemis.class);
	private EmbeddedActiveMQ broker;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private final File brokerFile = new File(baseDir + "/broker/broker.xml");
	private final File aioFile = new File(baseDir + "/broker/linux-x86_64/libartemis-native-64.so");
	private ActiveMQServerControlImpl manageBroker;

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}

	//See docs https://activemq.apache.org/components/artemis/documentation/javadocs/javadoc-latest/org/apache/activemq/artemis/api/core/management/QueueControl.html

	public String start() {

		try {

			setAIO();

			broker = new EmbeddedActiveMQ();

			//
			if (brokerFile.exists()) {
				log.info("event=StartBroker status=configuring config=broker.xml path={}", brokerFile.getAbsolutePath());				String fileConfig = "file:///" + brokerFile.getAbsolutePath();
				broker.setConfigResourcePath(fileConfig);
			} else {
				log.warn("No config file 'broker.xml' found.");
				log.info("event=StartBroker status=configuring config=broker.xml url=tcp://127.0.0.1:61616 path= {}", baseDir);
				this.setFileConfiguration("");
				String fileConfig = "file:///" + brokerFile.getAbsolutePath();
				broker.setConfigResourcePath(fileConfig);
			}

			broker.start();

			setManageBroker();

			log.info("event=StartBroker status={}",status());

			return status();

		} catch (Exception e) {
			
            log.error("event=StartBroker status=failed reason={}",e.getMessage(), e);

			return "failed";

		}

	}



	public String startEmbedded() throws Exception {

		log.info("event=StartEmbeddedBroker status=starting url=tcp://127.0.0.1:61616");

		Configuration config = new ConfigurationImpl();
		config.addAcceptorConfiguration("in-vm", "vm://0");
		config.addAcceptorConfiguration("tcp", "tcp://127.0.0.1:61616");
		config.setSecurityEnabled(false);

		broker = new EmbeddedActiveMQ();
		broker.setConfiguration(config);
		broker.start();

		log.info("event=StartEmbeddedBroker status={}",status());

		return status();

	}

	
	public String stop() throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();
		
		if(activeBroker!=null) {
			String nodeID= String.valueOf(activeBroker.getNodeID());
            log.info("event=StopBroker status=stopping id='{}' uptime={} ", nodeID, activeBroker.getUptime());
			broker.stop();
            log.info("event=StopBroker status=stopping id='{}'", nodeID);
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

		if(activeBroker!=null && (activeBroker.isActive() || activeBroker.getState().name().equals("STARTED"))) {
			status = "started";
		}

		return status;
	}

	@Override
	public Map<String, Object> stats() {
		return Map.of();
	}

	public String info() throws Exception {
		
		if(status().equals("started")) {
			ActiveMQServer activeBroker = broker.getActiveMQServer();

			return "uptime="+ activeBroker.getUptime()
					 + ",totalConnections=" + activeBroker.getTotalConnectionCount()
					 + ",totalConsumers=" + activeBroker.getTotalConsumerCount()
					 + ",totalMessages=" + activeBroker.getTotalMessageCount()
					 + ",nodeId=" + activeBroker.getNodeID()
					 + ",state=" + activeBroker.getState()
					 + ",version=" + activeBroker.getVersion().getFullVersion()
					 + ",type=ActiveMQ Artemis";
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
                assert is != null;
                Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        		is.close();
				
			} catch (IOException e) {
				log.error("event=GetFileConfiguration status=failed config=broker.xml reason={}", e.getMessage(), e);
			}
		}
    	
		return FileUtils.readFileToString(brokerFile, StandardCharsets.UTF_8);
					
	}
	
	public String setFileConfiguration(String brokerConfiguration) throws IOException {
		
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		
		if(brokerFile.exists() || !brokerConfiguration.isEmpty()) {

			URL schemaFile = classloader.getResource("broker.xsd");
			String xmlValidation = IntegrationUtil.isValidXML(schemaFile, brokerConfiguration);
			if(!xmlValidation.equals("xml is valid")) {
				return xmlValidation;
			} 
			FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
		}else {
			FileUtils.touch(brokerFile);
			InputStream is = classloader.getResourceAsStream("broker.xml");
            assert is != null;
            Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			is.close();
		}

		return "success";

	}

	public void setAIO() throws IOException {

		if (OSUtil.getOS().equals(OSUtil.OS.LINUX)) {

			checkIfNativeLibraryExists();
			loadNativeLibrary();

		}
	}

	private void checkIfNativeLibraryExists() throws IOException {

		if (!aioFile.exists()) {
			File parentDir = aioFile.getParentFile();
			if (parentDir != null && !parentDir.exists()) {
				boolean dirsCreated = parentDir.mkdirs();
				if (!dirsCreated) {
					log.error("event=SetAIO status=failed reason=Failed to create parent directories for native library path={}", parentDir.getAbsolutePath());
					throw new IOException("Failed to create parent directories for " + parentDir.getAbsolutePath());
				}
			}

			// Copy file from resources into empty file
			ClassLoader classloader = Thread.currentThread().getContextClassLoader();
			InputStream is = classloader.getResourceAsStream("libartemis-native-64.so");

			if (is != null) {
				Files.copy(is, aioFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				is.close();
				log.info("event=SetAIO status=success path={}", aioFile.getParent());
			} else {
				log.warn("event=SetAIO status=failed reason=Native library resource not found");
			}
		}

	}

	private void loadNativeLibrary(){
		try {
			System.load(aioFile.getAbsolutePath());
			log.info("event=AIO_Library_Loaded status=success library={}", aioFile.getAbsolutePath());
		} catch (UnsatisfiedLinkError e) {
			log.error("event=AIO_Library_Loaded status=failed reason=Could not load native library {}", aioFile.getAbsolutePath(), e);
		} catch (SecurityException e) {
			log.error("event=AIO_Library_Loaded status=failed reason=Security exception when loading native library {}", aioFile.getAbsolutePath(), e);
		}
	}

	//Manage queues
	public String createQueue(String queueName) throws Exception {
		manageBroker.createQueue(queueName, "ANYCAST", queueName,"",true,-1,false,true );
		return "success";
	}

	public String deleteQueue(String queueName) throws Exception {
		manageBroker.deleteAddress(queueName,true);
		return "success";
	}

	public String getQueue(String endpointName) throws Exception {

		checkIfEndpointExist(endpointName);

		JSONObject endpointInfo = new JSONObject();
		JSONObject endpoint = getEndpoint(endpointName);
		endpointInfo.put("queue",endpoint);

		return endpointInfo.toString();
	}

	public String getQueues() throws Exception {

		JSONObject endpointsInfo  = new JSONObject();
		JSONObject endpointInfo = new JSONObject();

		if(manageBroker!=null && status().equalsIgnoreCase("started")){
			try {
				String[] endpoints = manageBroker.getQueueNames("ANYCAST");
				endpoints = Arrays.stream(endpoints).distinct().toArray(String[]::new);

				for (String endpoint : endpoints) {
					endpointInfo.append("queue", getEndpoint(endpoint));
				}
			}catch (Exception e){
                log.error("event=GetQueues status={} reason={}", status(), e.getMessage(), e);
			}

			endpointsInfo.put("queues",endpointInfo);
		}

		return endpointsInfo.toString();
	}

	public String clearQueue(String queueName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();
		Queue queue = activeBroker.locateQueue(queueName);
		if (queue != null) {
			queue.deleteAllReferences();
		}

		return "success";
	}

	public String clearQueues() throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		Queue queue;

		for (String queueName : manageBroker.getQueueNames("ANYCAST")) {
			queue = activeBroker.locateQueue(queueName);
			if (queue != null) {
				queue.deleteAllReferences();
			}
		}

		return "success";
	}

	//Manage topics
	public String deleteTopic(String topicName) throws Exception {
		manageBroker.deleteAddress(topicName,true);
		return "success";
	}

	public String createTopic(String topicName) throws Exception {
		manageBroker.createQueue(topicName, "MULTICAST", topicName,"",true,-1,false,true );
		return "success";
	}


	public String clearTopic(String topicName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();
		Queue queue = activeBroker.locateQueue(topicName);
		if (queue != null) {
			queue.deleteAllReferences();
		}

		return "success";
	}

	public String clearTopics() throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		Queue queue;

		for (String queueName : manageBroker.getQueueNames("MULTICAST")) {
			queue = activeBroker.locateQueue(queueName);
			if (queue != null) {
				queue.deleteAllReferences();
			}
		}

		return "success";
	}

	public String getTopic(String endpointName) throws Exception {

		ActiveMQServer activeBroker = broker.getActiveMQServer();
		Queue queue = activeBroker.locateQueue(endpointName);

		JSONObject endpointInfo = new JSONObject();

		if (queue == null) {
			endpointInfo.put("topic","Endpoint " + endpointName + " not found");
		}else{
			JSONObject endpoint = getEndpoint(endpointName);
			endpointInfo.put("topic",endpoint);
		}

		return endpointInfo.toString();	}

	public String getTopics() throws Exception {

		JSONObject endpointsInfo  = new JSONObject();
		JSONObject endpointInfo = new JSONObject();

		String[] endpoints = manageBroker.getQueueNames("MULTICAST");
		endpoints = Arrays.stream(endpoints).distinct().toArray(String[]::new);

		for(String endpoint: endpoints){
			endpointInfo.append("topic", getEndpoint(endpoint));
		}

		endpointsInfo.put("topics",endpointInfo);

		return endpointsInfo.toString();
	}

	private JSONObject getEndpoint(String endpointName) throws Exception {

		JSONObject endpoint = new JSONObject();

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		endpoint.put("name",endpointName);
		endpoint.put("address",queueControl.getAddress());
		endpoint.put("temporary",queueControl.isTemporary());
		endpoint.put("numberOfMessages",queueControl.countMessages());
		endpoint.put("numberOfConsumers",queueControl.getConsumerCount());

		return endpoint;

	}

	private void checkIfEndpointExist(String endpointName) throws EndpointNotFoundException {

		if (!endpointExist(endpointName)) {
			throw new EndpointNotFoundException("Endpoint " + endpointName + " not found");
		}

	}

	private boolean endpointExist(String endpointName) {

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		Queue queue = activeBroker.locateQueue(endpointName);

		return queue != null;

	}

	//Manage Messages
	public String moveMessage(String sourceQueueName, String targetQueueName, String messageId) throws Exception {

		checkIfEndpointExist(sourceQueueName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + sourceQueueName);

		boolean result = queueControl.moveMessage(Long.parseLong(messageId),targetQueueName);

		return Boolean.toString(result);

	}

	public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {

		checkIfEndpointExist(sourceQueueName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + sourceQueueName);

		int result = queueControl.moveMessages("", targetQueueName);

		return Integer.toString(result);

	}

	public String removeMessage(String endpointName, String messageId) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		boolean result = queueControl.removeMessage(Long.parseLong(messageId));

		return Boolean.toString(result);

	}


	public String removeMessages(String endpointName) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		int result = queueControl.removeAllMessages();

		return Integer.toString(result);

	}

	public String listMessages(String endpointName, String filter) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		JSONObject messagesInfo = new JSONObject();
		JSONObject messageInfo = new JSONObject();

		String messages;
        messages = queueControl.listMessagesAsJSON(Objects.requireNonNullElse(filter, ""));

		JSONArray messagesArray = new JSONArray(messages);

		for (int i = 0 ; i < messagesArray.length(); i++) {
			JSONObject message = messagesArray.getJSONObject(i);
			messageInfo.append("message",message);
		}

		messagesInfo.put("messages",messageInfo);

		return messagesInfo.toString();

	}

	public String countMessagesFromList(String endpointList) {

		long numberOfMessages = 0L;
		String[] endpointNames= endpointList.split("\\s*,\\s*");
		ActiveMQServer activeBroker = broker.getActiveMQServer();

		for(String endpointName: endpointNames){

			if(endpointExist(endpointName)){
				QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);
				numberOfMessages = queueControl.getMessageCount();
			}

		}

		return Long.toString(numberOfMessages);

	}


	public String countMessages(String endpointName) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		long numberOfMessages = queueControl.getMessageCount();

		return Long.toString(numberOfMessages);

	}

	public String countDelayedMessages(String endpointName) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		long numberOfMessages = queueControl.getScheduledCount();

		return Long.toString(numberOfMessages);

	}

	public String getFlowMessageCountsList(boolean excludeEmptyQueues) throws Exception {

		Map<String, Long> flowIdsMessageCountMap = getFlowIdsMessageCountMap(excludeEmptyQueues);

		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(flowIdsMessageCountMap);
	}



	public String browseMessage(String endpointName, String messageId, boolean excludeBody) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		queueControl.getFirstMessageAsJSON();

		CompositeData[] messages = queueControl.browse();

		messages = stream(messages).filter(compositeData -> compositeData.get("messageID").equals(messageId)).toArray(CompositeData[]::new);

		return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

	}

	public String browseMessages(String endpointName, Integer page, Integer numberOfMessages, boolean excludeBody) throws Exception {

		checkIfEndpointExist(endpointName);

		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		CompositeData[] messages;

		if(page != null && numberOfMessages != null){
			messages = queueControl.browse(page,numberOfMessages);
		}else{

			long countMessages = queueControl.countMessages();

			if(countMessages > 10000){
				throw new RuntimeException("Maximum returned messages is 10000. Use paging when there are more than 10000 on the queue");
			}else{
				messages = queueControl.browse();
			}

		}

		return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

	}

	private Map<String, Long> getFlowIdsMessageCountMap(boolean excludeEmptyQueues) {
		Map<String, Long> destinationMessageCounts = new HashMap<>();

		try {
			ActiveMQServer activeBroker = broker.getActiveMQServer();
			// Get all queues names
			String[] queueNames = activeBroker.getActiveMQServerControl().getQueueNames();

			for (String queueName : queueNames) {
				if(!queueName.startsWith("ID_")) {
					// discard queues without prefix ID_
					continue;
				}

				// extract flowId
				String flowId = queueName.substring(0, Math.min(queueName.length(), 27));
				QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(ResourceNames.QUEUE + queueName);

				// Get the message count for the current queue
				long messageCount = queueControl.getMessageCount();

				if(destinationMessageCounts.containsKey(flowId)) {
					messageCount += destinationMessageCounts.get(flowId);
				}

				if(messageCount > 0 || !excludeEmptyQueues) {
					// Add queue name and message count to the map
					destinationMessageCounts.put(flowId, messageCount);
				}
			}

		} catch (Exception e) {
			log.error("event=getFlowIdsMessageCountMap reason=Error to get all destinations and messages counts", e);
		}

		return destinationMessageCounts;
	}

	public String sendMessage(String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception {

		checkIfEndpointExist(endpointName);

		String userName = broker.getActiveMQServer().getConfiguration().getClusterUser();
		String password = broker.getActiveMQServer().getConfiguration().getClusterPassword();

		Map<String,String> messageHeadersAsString = messageHeaders.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));


		ActiveMQServer activeBroker = broker.getActiveMQServer();

		QueueControl queueControl = (QueueControl) activeBroker.getManagementService().getResource(org.apache.activemq.artemis.api.core.management.ResourceNames.QUEUE + endpointName);

		return queueControl.sendMessage(messageHeadersAsString, Message.TEXT_TYPE, messageBody, true, userName, password);

	}

	public String getConsumers() throws Exception {
		String consumersList = manageBroker.listAllConsumersAsJSON();

		JSONObject consumers = new JSONObject();
		consumers.put("consumers",new JSONArray(consumersList));

		return consumers.toString();
	}

	public String getConnections() throws Exception {
		String connectionsList = manageBroker.listConnectionsAsJSON();

		JSONObject connections = new JSONObject();
		connections.put("connections",new JSONArray(connectionsList));

		return connections.toString();
	}

	@Override
	public Object getBroker() {
		return broker;
	}

	private void setManageBroker(){
		ActiveMQServer activeBroker = broker.getActiveMQServer();

        manageBroker = activeBroker.getActiveMQServerControl();

	}

}
