package org.assimbly.broker.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.activemq.broker.*;
import org.apache.activemq.broker.jmx.*;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.broker.Broker;
import org.assimbly.broker.converter.CompositeDataConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

public class ActiveMQClassic implements Broker {

	protected Logger log = LoggerFactory.getLogger(getClass());
	
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    File brokerFile = new File(baseDir + "/broker/activemq.xml");

    BrokerService broker;

    BrokerViewMBean brokerViewMBean;
    QueueViewMBean queueViewMbean;
    TopicViewMBean topicViewMbean;

    private String endpointExist;
    private String endpointType;

    public void setBaseDirectory(String baseDirectory) {
        BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
    }

    public String start() {

        try{
            broker = new BrokerService();
        

            if(brokerFile.exists()) {
                log.info("Using config file 'activemq.xml'. Loaded from " + brokerFile.getCanonicalPath());
                URI urlConfig = new URI("xbean:" + URLEncoder.encode(brokerFile.getCanonicalPath(), "UTF-8"));
                broker = BrokerFactory.createBroker(urlConfig);
            }else {
                this.setFileConfiguration("");
                log.warn("No config file 'activemq.xml' found.");
                log.info("Created default 'activemq.xml' stored in following directory: " + brokerFile.getAbsolutePath());
                log.info("broker.xml documentation reference: https://activemq.apache.org/components/artemis/documentation/latest/configuration-index.html");
                log.info("");
                log.info("Start broker in local mode on url: tcp://127.0.0.1:61616");

                URI urlConfig = new URI("xbean:" + URLEncoder.encode(brokerFile.getCanonicalPath(), "UTF-8"));
                broker = BrokerFactory.createBroker(urlConfig);
            }

            if(!broker.isStarted()) {
                broker.start();
            }

            if(broker.isStarted()) {
                setBrokerViewMBean();
            }

            return status();
        }catch (Exception e) {
            e.printStackTrace();
            return "Failed to start broker. Reason: " + e.getMessage();
        }

    }


    public String startEmbedded() throws Exception {

        broker = new BrokerService();

        log.warn("Start broker in local mode on url: tcp://127.0.0.1:61616");

        TransportConnector connector = new TransportConnector();
        connector.setUri(new URI("tcp://127.0.0.1:61616"));

        broker.addConnector(connector);

        if(!broker.isStarted()) {
            broker.start();
        }

        if(broker.isStarted()) {
            broker.setUseJmx(true);
            setBrokerViewMBean();
        }

        return status();

    }

    public String stop() throws Exception {
        broker.stop();
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

        if(broker==null) {
            broker = new BrokerService();
        }

        if(broker.isStarted()){
            return "started";
        }
        else {
            return "stopped";
        }
    }

    public String getFileConfiguration() throws IOException {

        if(!brokerFile.exists()) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();

            try {
                FileUtils.touch(brokerFile);
                InputStream is = classloader.getResourceAsStream("activemq.xml");
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

            URL schemaFile = classloader.getResource("spring-beans.xsd");
            String xmlValidation = IntegrationUtil.isValidXML(schemaFile, brokerConfiguration);
            if(!xmlValidation.equals("xml is valid")) {
                return xmlValidation;
            }

            FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
        }else {
            FileUtils.touch(brokerFile);
            InputStream is = classloader.getResourceAsStream("activemq.xml");
            Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            is.close();
        }

        return "configuration set";
    }


    @Override
    public String info() throws Exception {
        if(status().equals("started")) {

            BrokerView adminView = broker.getAdminView();

            String info = "uptime="+ broker.getUptime()
                    + 	",totalConnections=" + broker.getTotalConnections()
                    + ",totalConsumers=" + adminView.getTotalConsumerCount()
                    + ",totalMessages=" + adminView.getTotalMessageCount()
                    + ",nodeId=" + adminView.getBrokerId()
                    + ",state=" + broker.isStarted()
                    + ",version=" + adminView.getBrokerVersion()
                    + ",type=ActiveMQ Classic";
            return info;
        }else {
            return "no info. broker not running";
        }

    }

    public String createQueue(String queueName) throws Exception {
        brokerViewMBean.addQueue(queueName);
        return "success";
    }

    public String deleteQueue(String queueName) throws Exception {
        brokerViewMBean.removeQueue(queueName);
        return null;
    }

    public String getQueue(String queueName) throws Exception {

        JSONObject endpointInfo = new JSONObject();

        JSONObject endpoint = getEndpoint("false","Queue",queueName);
        endpointInfo.put("queue",endpoint);

        return endpointInfo.toString();
    }

    public String getQueues() throws Exception {

        JSONObject endpointsInfo  = new JSONObject();
        JSONObject endpointInfo = new JSONObject();

        ObjectName[] queues = brokerViewMBean.getQueues();

        for(Object queue: queues){

            String queueAsString = StringUtils.substringAfter(queue.toString(), "destinationName=");

            endpointInfo.append("queue", getEndpoint("false","Queue",queueAsString));

        }

        endpointsInfo.put("queues",endpointInfo);

        return endpointsInfo.toString();

    }

    public String clearQueue(String queueName) throws Exception {

        queueViewMbean = getQueueViewMBean("Queue", queueName);
        queueViewMbean.purge();

        return "success";
    }

    public String clearQueues() throws Exception {
        ObjectName[] queues = brokerViewMBean.getQueues();

        for(Object queue: queues){
            String queueAsString = StringUtils.substringAfter(queue.toString(), "destinationName=");
            queueViewMbean = getQueueViewMBean("Queue", queueAsString);
            queueViewMbean.purge();
        }

        return "success";

    }

    public String createTopic(String topicName) throws Exception {
        brokerViewMBean.addTopic(topicName);
        return "success";
    }

    public String deleteTopic(String topicName) throws Exception {
        brokerViewMBean.removeTopic(topicName);
        return "success";
    }

    public String clearTopic(String topicName) throws Exception {

        topicViewMbean = getTopicViewMBean("Topic", topicName);
        topicViewMbean.resetStatistics();

        return "success";
    }

    public String clearTopics() throws Exception {

        ObjectName[] topics = brokerViewMBean.getTopics();

        for(Object topic: topics){
            String topicAsString = StringUtils.substringAfter(topic.toString(), "destinationName=");
            topicViewMbean = getTopicViewMBean("Topic", topicAsString);
            topicViewMbean.resetStatistics();

        }

        return "success";

    }

    public String getTopic(String topicName) throws Exception {
        JSONObject endpointInfo = new JSONObject();

        JSONObject endpoint = getEndpoint("false","Topic",topicName);
        endpointInfo.put("topic",endpoint);

        return endpointInfo.toString();
    }

    public String getTopics() throws Exception {

        JSONObject endpointsInfo  = new JSONObject();
        JSONObject endpointInfo = new JSONObject();

        ObjectName[] topics = brokerViewMBean.getTopics();

        for(Object topic: topics){
            String topicAsString = StringUtils.substringAfter(topic.toString(), "destinationName=");
            if(!topicAsString.startsWith("ActiveMQ")){
                endpointInfo.append("topic", getEndpoint("false","Topic",topicAsString));
            }
        }

        endpointsInfo.put("topics",endpointInfo);

        return endpointsInfo.toString();
    }

    private String checkIfEndpointExist(String endpointName) throws Exception {

        ObjectName[] queues = brokerViewMBean.getQueues();

        for (Object queue : queues) {
            String endpointAsString = StringUtils.substringAfter(queue.toString(), "destinationName=");
            if(endpointName.equals(endpointAsString)){
                return "Queue";
            }
        }

        ObjectName[] topics = brokerViewMBean.getTopics();

        for (Object topic : topics) {
            String endpointAsString = StringUtils.substringAfter(topic.toString(), "destinationName=");
            if(endpointName.equals(endpointAsString)){
                return "Topic";
            }
        }

        return "Unknown";

    }

    //Manage Messages

    public String moveMessage(String sourceQueueName, String targetQueueName, String messageId) throws Exception {

        endpointType = checkIfEndpointExist(sourceQueueName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + sourceQueueName + " not found");
        }

        queueViewMbean = getQueueViewMBean(endpointType, sourceQueueName);

        boolean result = queueViewMbean.moveMessageTo(messageId,targetQueueName);

        return Boolean.toString(result);
    }

    public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {

        endpointType = checkIfEndpointExist(sourceQueueName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + sourceQueueName + " not found");
        }

        queueViewMbean = getQueueViewMBean(endpointType, sourceQueueName);

        int result = queueViewMbean.moveMatchingMessagesTo("",targetQueueName);

        return Integer.toString(result);
    }

    public String removeMessage(String endpointName, String messageId) throws Exception {

        endpointExist = checkIfEndpointExist(endpointName);

        if (!endpointExist.equalsIgnoreCase("queue")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        queueViewMbean = getQueueViewMBean("Queue", endpointName);

        boolean result = queueViewMbean.removeMessage(messageId);
        return Boolean.toString(result);
    }

    public String removeMessages(String endpointName) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        queueViewMbean = getQueueViewMBean(endpointType, endpointName);
        Long queueSize = queueViewMbean.getQueueSize();
        queueViewMbean.purge();

        return Long.toString(queueSize);

    }

    public String browseMessage(String endpointName, String messageId, boolean excludeBody) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        messageId = "JMSMessageID='" + messageId + "'";

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse(messageId);

        String result = CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

        return result;

    }

    public String browseMessages(String endpointName, Integer page, Integer numberOfMessages, boolean excludeBody) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse();

        if(page != null && numberOfMessages != null) {
            messages =  getMessagesByPage(messages, page, numberOfMessages);
        }

        String result = CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

        return result;
    }

    public String listMessages(String endpointName, String filter) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        CompositeData[] messages = getDestinationViewMBean(endpointType,endpointName).browse(filter);

        String result = CompositeDataConverter.convertToJSON(messages, 1000, true, true);

        return result;

    }

    public String countMessages(String endpointName) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        Long queueSize = getDestinationViewMBean(endpointType,endpointName).getQueueSize();

        return Long.toString(queueSize);

    }

    public String sendMessage(String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception {

        endpointType = checkIfEndpointExist(endpointName);

        if (endpointType.equalsIgnoreCase("unknown")) {
            throw new Exception("Endpoint " + endpointName + " not found");
        }

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        if(!MapUtils.isEmpty(messageHeaders)){

            if(messageHeaders.containsKey("JMSDeliveryMode")) {
                if (messageHeaders.get("JMSDeliveryMode").toString().equalsIgnoreCase("PERSISTENT") || messageHeaders.get("JMSDeliveryMode").toString().equalsIgnoreCase("0")) {
                    messageHeaders.put("JMSDeliveryMode", 0);
                } else {
                    messageHeaders.put("JMSDeliveryMode", 1);
                }
            }

            if(messageHeaders.containsKey("JMSTimestamp")) {
                messageHeaders.remove("JMSTimestamp");
            }

            destinationViewMBean.sendTextMessage(messageHeaders,messageBody);
        }else{
            destinationViewMBean.sendTextMessage(messageBody);
        }
        return "success";
    }


    public String getConsumers() throws Exception {
        return "n/a";
    }

    public String getConnections() throws Exception {

        JSONObject connectionsInfo  = new JSONObject();
        JSONObject connectionInfo = new JSONObject();

        Connection[] connections = broker.getRegionBroker().getClients();

        JSONObject connectionDetails = new JSONObject();

        for(Connection connection: connections){
            connectionDetails.put("connectionID", connection.getConnectionId());
            connectionDetails.put("clientAddress", connection.getRemoteAddress());
            connectionDetails.put("creationTime", Long.toString(connection.getStatistics().getStartTime()));
            //connectionDetails.put("sessionCount", connection.getActiveTransactionCount());
            connectionInfo.append("connection", connectionDetails);
        }

        connectionsInfo.put("connections",connectionInfo);

        return connectionsInfo.toString();


    }

    private CompositeData[] getMessagesByPage(CompositeData[] messages, Integer page, Integer numberOfMessages){

        List<CompositeData> list = Arrays.asList(messages);

        if(page == 1){

            Integer startIndex = 0;
            Integer endIndex = numberOfMessages;

            if(list.size() < endIndex){
                endIndex = list.size();
            }

            list = list.subList(startIndex, endIndex);


        }else{

            Integer startIndex = (page -1) * numberOfMessages;
            Integer endIndex = (page) * numberOfMessages;

            if(list.size() < startIndex){
                startIndex = list.size();
            }

            if(list.size() < endIndex){
                endIndex = list.size();
            }

            list = list.subList(startIndex, endIndex);
        }

        messages = list.toArray(new CompositeData[list.size()]);

        return messages;

    }


    private JSONObject getEndpoint(String isTemporary, String endpointType, String endpointName) throws Exception {

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        JSONObject endpoint = new JSONObject();

        endpoint.put("name",endpointName);
        endpoint.put("address",destinationViewMBean.getName());
        endpoint.put("temporary",isTemporary);
        if(endpointType.equalsIgnoreCase("Topic")){
            endpoint.put("numberOfMessages",destinationViewMBean.getEnqueueCount());
        }else{
            endpoint.put("numberOfMessages",destinationViewMBean.getQueueSize());
        }
        endpoint.put("numberOfConsumers",destinationViewMBean.getConsumerCount());

        return endpoint;

    }


    @Override
    public Object getBroker() throws Exception {
        return broker;
    }

    public void setBrokerViewMBean() throws MalformedObjectNameException {
        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName());
        brokerViewMBean = (BrokerViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, BrokerViewMBean.class, true);
    }

    public DestinationViewMBean getDestinationViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        //type=Broker,brokerName=localbroker,
		/*
		Hashtable<String, String> params = new Hashtable<>();
		params.put("brokerName", broker.getBrokerName());
		params.put("type", "Broker");
		params.put("destinationType", "Queue");
		params.put("destinationName", queueName);
		ObjectName queueObjectName = ObjectName.getInstance(broker., params);

				queueViewMbean  = (QueueViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, QueueViewMBean.class, true);

		*/

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        DestinationViewMBean destinationViewMbean = (DestinationViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, DestinationViewMBean.class, true);

        return destinationViewMbean;
    }

    public QueueViewMBean getQueueViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        QueueViewMBean queueViewMbean  = (QueueViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, QueueViewMBean.class, true);

        return queueViewMbean;
    }

    public TopicViewMBean getTopicViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        TopicViewMBean topicViewMbean  = (TopicViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, TopicViewMBean.class, true);

        return topicViewMbean;
    }

    public ConnectorViewMBean getConnectorViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        ConnectorViewMBean connectorViewMBean  = (ConnectorViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, ConnectorViewMBean.class, true);

        return connectorViewMBean;
    }

}