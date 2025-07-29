package org.assimbly.broker.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.net.UrlEscapers;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.Connection;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.broker.jmx.*;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.broker.Broker;
import org.assimbly.broker.converter.CompositeDataConverter;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jakarta.jms.DeliveryMode.NON_PERSISTENT;
import static jakarta.jms.DeliveryMode.PERSISTENT;

public class ActiveMQClassic implements Broker {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private final File brokerFile = new File(baseDir + "/broker/activemq.xml");
    private BrokerService broker;
    private BrokerViewMBean brokerViewMBean;
    private QueueViewMBean queueViewMbean;
    private TopicViewMBean topicViewMbean;
    private String endpointType;

    public void setBaseDirectory(String baseDirectory) {
        BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
    }

    public String start() {

        try{
            broker = new BrokerService();

            String brokerPath = brokerFile.getCanonicalPath();

            String brokerUrl = "xbean:file:" + UrlEscapers.urlFragmentEscaper().escape(brokerPath);

            if(brokerFile.exists()) {
                log.debug("event=StartBroker status=configuring config=broker.xml path={}", brokerFile.getAbsolutePath());
                URI configurationUri = new URI(brokerUrl);
                broker = BrokerFactory.createBroker(configurationUri);
            }else {
                this.setFileConfiguration("");
                log.warn("No config file 'activemq.xml' found.");
                log.debug("event=StartBroker status=configuring config=broker.xml url=tcp://127.0.0.1:61616 path= {}", baseDir);

                brokerUrl = "xbean:" + UrlEscapers.urlFragmentEscaper().escape(brokerFile.getCanonicalPath());
                URI urlConfig = new URI(brokerUrl);
                broker = BrokerFactory.createBroker(urlConfig);
            }

            if(!broker.isStarted()) {
                broker.start();
            }

            if(broker.isStarted()) {
                setBrokerViewMBean();
                log.info("Broker Runtime started");
            }

            return status();
        }catch (Exception e) {
            log.error("event=StartBroker status=failed reason={}",e.getMessage(), e);

            return "failed";
        }

    }

    public String startEmbedded() throws Exception {

        log.info("event=StartEmbeddedBroker status=starting url=tcp://127.0.0.1:61616");

        broker = new BrokerService();

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

        log.info("event=StartEmbeddedBroker status={}",status());

        return status();

    }

    public String stop() throws Exception {
        log.info("event=StopBroker status=stopping");
        broker.stop();
        log.info("event=StopBroker status={}", status());
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
                assert is != null;
                Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                is.close();

            } catch (IOException e) {
                log.error("event=GetFileConfiguration status=failed config=activemq.xml reason={}", e.getMessage(), e);
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

            brokerConfiguration = StringUtils.replace(brokerConfiguration, "${activemq.data}", baseDir + "/broker");
            FileUtils.writeStringToFile(brokerFile, brokerConfiguration,StandardCharsets.UTF_8);
        }else {
            FileUtils.touch(brokerFile);
            InputStream inputStream = classloader.getResourceAsStream("activemq.xml");

            assert inputStream != null;
            brokerConfiguration = DocConverter.convertStreamToString(inputStream);

            inputStream.close();

            brokerConfiguration = StringUtils.replace(brokerConfiguration, "${activemq.data}", baseDir + "/broker");

            FileUtils.writeStringToFile(brokerFile,brokerConfiguration, StandardCharsets.UTF_8);

        }

        return "success";
    }


    @Override
    public String info() throws Exception {
        if(status().equals("started")) {

            BrokerView adminView = broker.getAdminView();

            return "uptime="+ broker.getUptime()
                    + ",totalConnections=" + broker.getTotalConnections()
                    + ",currentConnections=" + broker.getCurrentConnections()
                    + ",totalConsumers=" + adminView.getTotalConsumerCount()
                    + ",totalMessages=" + adminView.getTotalMessageCount()
                    + ",nodeId=" + adminView.getBrokerId()
                    + ",state=" + broker.isStarted()
                    + ",version=" + adminView.getBrokerVersion()
                    + ",type=ActiveMQ Classic";
        }else {
            return "no info. broker not running";
        }

    }

    @Override
    public Map<String, Object> stats() throws Exception {

        if(status().equals("started")) {

            BrokerView adminView = broker.getAdminView();

            String query = broker.getBroker().getBrokerService().getTransportConnectorByName("openwire").getConnectUri().getQuery();
            Pattern pattern = Pattern.compile("maximumConnections=(\\d+)");
            Matcher matcher = pattern.matcher(query);
            int maxConnections = 0;

            if(matcher.find())
                maxConnections = Integer.parseInt(matcher.group(1));

            return Map.of(
                    "openConnections", adminView.getCurrentConnectionsCount(),
                    "maxConnections", maxConnections,
                    "totalNumberOfQueues", adminView.getQueues().length,
                    "totalNumberOfTemporaryQueues", adminView.getTemporaryQueues().length,
                    "tmpPercentUsage", adminView.getTempPercentUsage(),
                    "storePercentUsage", adminView.getStorePercentUsage(),
                    "memoryPercentUsage", adminView.getMemoryPercentUsage(),
                    "averageMessageSize", adminView.getAverageMessageSize()
            );

        }else {
            return Collections.emptyMap();
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

    private String getEndpointType(String endpointName) {

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

    private void checkIfEndpointExist(String endpointName) throws Exception {

        if (!endpointExist(endpointName)) {
            throw new EndpointNotFoundException("Endpoint " + endpointName + " not found");
        }

        endpointType = getEndpointType(endpointName);
    }


    private boolean endpointExist(String endpointName) {

        ObjectName[] queues = brokerViewMBean.getQueues();

        for (Object queue : queues) {
            String endpointAsString = StringUtils.substringAfter(queue.toString(), "destinationName=");
            if(endpointName.equals(endpointAsString)){
                return true;
            }
        }

        ObjectName[] topics = brokerViewMBean.getTopics();

        for (Object topic : topics) {
            String endpointAsString = StringUtils.substringAfter(topic.toString(), "destinationName=");
            if(endpointName.equals(endpointAsString)){
                return false;
            }
        }

        return false;

    }


    //Manage Messages

    public String moveMessage(String sourceQueueName, String targetQueueName, String messageId) throws Exception {

        checkIfEndpointExist(sourceQueueName);

        queueViewMbean = getQueueViewMBean(endpointType, sourceQueueName);

        boolean result = queueViewMbean.moveMessageTo(messageId,targetQueueName);

        return Boolean.toString(result);
    }

    public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {

        checkIfEndpointExist(sourceQueueName);

        queueViewMbean = getQueueViewMBean(endpointType, sourceQueueName);

        int result = queueViewMbean.moveMatchingMessagesTo("",targetQueueName);

        return Integer.toString(result);
    }

    public String removeMessage(String endpointName, String messageId) throws Exception {

        endpointType = getEndpointType(endpointName);

        if (!endpointType.equalsIgnoreCase("queue")) {
            throw new EndpointNotFoundException("Endpoint " + endpointName + " not found");
        }

        queueViewMbean = getQueueViewMBean("Queue", endpointName);

        boolean result = queueViewMbean.removeMessage(messageId);
        return Boolean.toString(result);
    }

    public String removeMessages(String endpointName) throws Exception {

        checkIfEndpointExist(endpointName);

        queueViewMbean = getQueueViewMBean(endpointType, endpointName);
        long queueSize = queueViewMbean.getQueueSize();
        queueViewMbean.purge();

        return Long.toString(queueSize);

    }

    public String browseMessage(String endpointName, String messageId, boolean excludeBody) throws Exception {

        checkIfEndpointExist(endpointName);

        String messageIdKey = "JMSMessageID='" + messageId + "'";

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse(messageIdKey);

        return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

    }

    public String browseMessages(String endpointName, Integer page, Integer numberOfMessages, boolean excludeBody) throws Exception {

        checkIfEndpointExist(endpointName);

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse();

        if(page != null && numberOfMessages != null) {
            messages =  getMessagesByPage(messages, page, numberOfMessages);
        }

        return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

    }

    public String listMessages(String endpointName, String filter) throws Exception {

        checkIfEndpointExist(endpointName);

        CompositeData[] messages = getDestinationViewMBean(endpointType,endpointName).browse(filter);

        return CompositeDataConverter.convertToJSON(messages, 1000, true, true);

    }


    public String countMessages(String endpointName) throws Exception {

        checkIfEndpointExist(endpointName);

        long queueSize = getDestinationViewMBean(endpointType,endpointName).getQueueSize();

        return Long.toString(queueSize);

    }

    public String countMessagesFromList(String endpointList) throws Exception {

        long numberOfMessages = 0L;
        String[] endpointNames= endpointList.split("\\s*,\\s*");

        for(String endpointName: endpointNames){

            if(endpointExist(endpointName)){
                endpointType = getEndpointType(endpointName);
                numberOfMessages += getDestinationViewMBean(endpointType,endpointName).getQueueSize();
            }

        }

        return Long.toString(numberOfMessages);

    }

    public String countDelayedMessages(String endpointName) throws Exception {

        checkIfEndpointExist(endpointName);

        Collection<?> collection = getJobSchedulerViewMBean().getAllJobs(true).values();

        long numberOfDelayedMessages = collection.stream()
                .map(o -> (CompositeData) o)
                .filter(row -> endpointName.equals(row.get("destinationName")))
                .count();

        return Long.toString(numberOfDelayedMessages);

    }

    public String getFlowMessageCountsList(boolean excludeEmptyQueues) throws Exception {

        Map<String, Long> flowIdsMessageCountMap = getFlowIdsMessageCountMap(endpointType, excludeEmptyQueues);

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(flowIdsMessageCountMap);
    }

    public String sendMessage(String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception {

        checkIfEndpointExist(endpointName);

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        if(MapUtils.isEmpty(messageHeaders)){
            messageHeaders.put("JMSDeliveryMode", PERSISTENT);
            destinationViewMBean.sendTextMessage(messageHeaders,messageBody);
        }else{
            if(messageHeaders.containsKey("JMSDeliveryMode")) {
                if (messageHeaders.get("JMSDeliveryMode").toString().equalsIgnoreCase("PERSISTENT") || messageHeaders.get("JMSDeliveryMode").toString().equalsIgnoreCase("0") || messageHeaders.get("JMSDeliveryMode").toString().equalsIgnoreCase("2")) {
                    messageHeaders.put("JMSDeliveryMode", PERSISTENT);
                } else {
                    messageHeaders.put("JMSDeliveryMode", NON_PERSISTENT);
                }
            }

            messageHeaders.remove("JMSTimestamp");

            destinationViewMBean.sendTextMessage(messageHeaders,messageBody);

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
            connectionInfo.append("connection", connectionDetails);
        }

        connectionsInfo.put("connections",connectionInfo);

        return connectionsInfo.toString();


    }

    private CompositeData[] getMessagesByPage(CompositeData[] messages, Integer page, Integer numberOfMessages){

        List<CompositeData> list = Arrays.asList(messages);

        int startIndex;
        int endIndex;
        if(page == 1){

            startIndex = 0;
            endIndex = numberOfMessages;


        }else{

            startIndex = (page - 1) * numberOfMessages;
            endIndex = (page) * numberOfMessages;

            if(list.size() < startIndex){
                startIndex = list.size();
            }

        }
        if(list.size() < endIndex){
            endIndex = list.size();
        }
        list = list.subList(startIndex, endIndex);

        return list.toArray(new CompositeData[0]);

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

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        return (DestinationViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, DestinationViewMBean.class, true);

    }

    private Map<String, Long> getFlowIdsMessageCountMap(String destinationType, boolean excludeEmptyQueues) throws MalformedObjectNameException {
        Map<String, Long> destinationMessageCounts = new HashMap<>();

        try {
            // Get all destinations
            Set<ObjectName> destinations = broker.getManagementContext().queryNames(new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",*"), null);

            // Iterate over each destination
            for (ObjectName destination : destinations) {
                String destinationName = destination.getKeyProperty("destinationName");

                if(!destinationName.startsWith("ID_")) {
                    // discard destination without prefix ID_
                    continue;
                }

                // extract flowId
                String flowId = destinationName.substring(0, Math.min(destinationName.length(), 27));

                // Get the DestinationViewMBean for the current destination
                DestinationViewMBean destinationViewMBean = getQueueViewMBean(destinationType, destinationName);

                // Get the message count for the current destination
                long messageCount = destinationViewMBean.getQueueSize();

                if(destinationMessageCounts.containsKey(flowId)) {
                    messageCount += destinationMessageCounts.get(flowId);
                }

                if(messageCount > 0 || !excludeEmptyQueues) {
                    // Add the destination name and message count to the map
                    destinationMessageCounts.put(flowId, messageCount);
                }
            }
        } catch (Exception e) {
            log.error("event=getFlowIdsMessageCountMap status=failed message=Failed to get all destinations and messages counts reason={}", e.getMessage(), e);
        }

        return destinationMessageCounts;
    }

    private QueueViewMBean getQueueViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        return (QueueViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, QueueViewMBean.class, true);

    }

    private TopicViewMBean getTopicViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        return (TopicViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, TopicViewMBean.class, true);

    }

    private JobSchedulerViewMBean getJobSchedulerViewMBean() throws Exception {

        ObjectName objectName = broker.getAdminView().getJMSJobScheduler();
        return (JobSchedulerViewMBean) broker.getManagementContext().newProxyInstance(objectName, JobSchedulerViewMBean.class, true);

    }

}