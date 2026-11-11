package org.assimbly.broker.impl;

import org.apache.activemq.broker.jmx.*;

import java.util.*;

import tools.jackson.databind.ObjectMapper;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.Connection;
import org.apache.activemq.broker.TransportConnector;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern MAXIMUM_CONNECTIONS = Pattern.compile("maximumConnections=(\\d+)");

    public void setBaseDirectory(String baseDirectory) {
        BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
    }

    public String start() {

        try {
            if (brokerFile.exists()) {
                log.info("event=StartBroker status=configuring config=broker.xml path={}", brokerFile.getAbsolutePath());
            } else {
                log.info("event=StartBroker status=configuring config=broker.xml url=tcp://127.0.0.1:61616 path={}", baseDir);
                this.setFileConfiguration("");
                log.warn("No config file 'activemq.xml' found.");
            }

            // File.toURI() gives the file:///C:/... form the xbean scheme needs on Windows
            URI fileUri = brokerFile.getCanonicalFile().toURI();
            broker = BrokerFactory.createBroker(new URI("xbean:" + fileUri));

            if (!broker.isStarted()) {
                broker.start();
            }

            if (broker.isStarted()) {
                setBrokerViewMBean();
                log.info("Broker Runtime started");
            }

            return status();
        } catch (Exception e) {
            log.error("event=StartBroker status=failed reason={}", e.getMessage(), e);
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
        if(broker != null) {
            broker.stop();
        }
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
        return broker != null && broker.isStarted() ? "started" : "stopped";
    }

    public String getFileConfiguration() throws IOException {

        if(!brokerFile.exists()) {
            Files.createDirectories(brokerFile.toPath().getParent());
            try(InputStream is = defaultConfiguration()) {
                Files.copy(is, brokerFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        return Files.readString(brokerFile.toPath());

    }

    private InputStream defaultConfiguration() throws IOException {

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("activemq.xml");
        if (is == null) {
            throw new IOException("Example activemq.xml not found on the classpath");
        }

        return is;

    }

    public String setFileConfiguration(String brokerConfiguration) throws IOException {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        String resolvedConfiguration;

        if (brokerConfiguration == null) {
            brokerConfiguration = "";
        }

        if (brokerFile.exists() || !brokerConfiguration.isEmpty()) {

            URL schemaFile = classloader.getResource("spring-beans.xsd");
            String xmlValidation = IntegrationUtil.isValidXML(schemaFile, brokerConfiguration);
            if (!xmlValidation.equals("xml is valid")) {
                return xmlValidation;
            }
            resolvedConfiguration = brokerConfiguration;

        } else {
            try(InputStream inputStream = defaultConfiguration()) {
                resolvedConfiguration = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        resolvedConfiguration = resolvedConfiguration.replace("${activemq.data}", baseDir + "/broker");
        Files.createDirectories(brokerFile.toPath().getParent());
        Files.writeString(brokerFile.toPath(), resolvedConfiguration);

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

            // Stays 0 when the configuration has no openwire connector or no connection limit
            int maxConnections = 0;
            TransportConnector openwire = broker.getTransportConnectorByName("openwire");
            String query = openwire == null ? null : openwire.getConnectUri().getQuery();

            if(query != null) {
                Matcher matcher = MAXIMUM_CONNECTIONS.matcher(query);
                if(matcher.find())
                    maxConnections = Integer.parseInt(matcher.group(1));
            }

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
        return "success";
    }

    public String getQueue(String queueName) throws Exception {

        JSONObject endpointInfo = new JSONObject();

        JSONObject endpoint = getEndpoint("Queue",queueName);
        endpointInfo.put("queue",endpoint);

        return endpointInfo.toString();
    }

    public String getQueues() throws Exception {

        JSONObject endpointsInfo  = new JSONObject();
        JSONObject endpointInfo = new JSONObject();

        ObjectName[] queues = brokerViewMBean.getQueues();

        for(ObjectName queue: queues){
            endpointInfo.append("queue", getEndpoint("Queue",destinationName(queue)));
        }

        endpointsInfo.put("queues",endpointInfo);

        return endpointsInfo.toString();

    }

    public String clearQueue(String queueName) throws Exception {

        getQueueViewMBean(queueName).purge();

        return "success";
    }

    public String clearQueues() throws Exception {

        ObjectName[] queues = brokerViewMBean.getQueues();

        for(ObjectName queue: queues){
            getQueueViewMBean(destinationName(queue)).purge();
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

        TopicViewMBean topicViewMbean = getTopicViewMBean(topicName);
        topicViewMbean.resetStatistics();

        return "success";
    }

    public String clearTopics() throws Exception {

        ObjectName[] topics = brokerViewMBean.getTopics();

        for(ObjectName topic: topics){
            getTopicViewMBean(destinationName(topic)).resetStatistics();
        }

        return "success";

    }

    public String getTopic(String topicName) throws Exception {
        JSONObject endpointInfo = new JSONObject();

        JSONObject endpoint = getEndpoint("Topic",topicName);
        endpointInfo.put("topic",endpoint);

        return endpointInfo.toString();
    }

    public String getTopics() throws Exception {

        JSONObject endpointsInfo  = new JSONObject();
        JSONObject endpointInfo = new JSONObject();

        ObjectName[] topics = brokerViewMBean.getTopics();

        for(ObjectName topic: topics){
            String topicAsString = destinationName(topic);
            if(!topicAsString.startsWith("ActiveMQ")){
                endpointInfo.append("topic", getEndpoint("Topic",topicAsString));
            }
        }

        endpointsInfo.put("topics",endpointInfo);

        return endpointsInfo.toString();
    }

    private String getEndpointType(String endpointName) {

        if (hasDestination(brokerViewMBean.getQueues(), endpointName)) {
            return "Queue";
        }

        if (hasDestination(brokerViewMBean.getTopics(), endpointName)) {
            return "Topic";
        }

        return "Unknown";

    }

    private static boolean hasDestination(ObjectName[] destinations, String endpointName) {
        return Arrays.stream(destinations).anyMatch(destination -> endpointName.equals(destinationName(destination)));
    }

    private static String destinationName(ObjectName destination) {
        return destination.getKeyProperty("destinationName");
    }

    private String checkIfEndpointExist(String endpointName) throws EndpointNotFoundException {

        String endpointType = getEndpointType(endpointName);

        if (endpointType.equals("Unknown")) {
            throw new EndpointNotFoundException("Endpoint " + endpointName + " not found");
        }

        return endpointType;
    }


    // Moving and purging messages only exists for queues
    private void checkIfQueueExist(String endpointName) throws EndpointNotFoundException {

        if (!getEndpointType(endpointName).equals("Queue")) {
            throw new EndpointNotFoundException("Endpoint " + endpointName + " not found");
        }

    }


    //Manage Messages

    public String moveMessage(String sourceQueueName, String targetQueueName, String messageId) throws Exception {

        checkIfQueueExist(sourceQueueName);

        QueueViewMBean queueViewMbean = getQueueViewMBean(sourceQueueName);

        boolean result = queueViewMbean.moveMessageTo(messageId,targetQueueName);

        return Boolean.toString(result);
    }

    public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception {

        checkIfQueueExist(sourceQueueName);

        QueueViewMBean queueViewMbean = getQueueViewMBean(sourceQueueName);

        int result = queueViewMbean.moveMatchingMessagesTo("",targetQueueName);

        return Integer.toString(result);
    }

    public String removeMessage(String endpointName, String messageId) throws Exception {

        checkIfQueueExist(endpointName);

        QueueViewMBean queueViewMbean = getQueueViewMBean(endpointName);

        boolean result = queueViewMbean.removeMessage(messageId);
        return Boolean.toString(result);
    }

    public String removeMessages(String endpointName) throws Exception {

        checkIfQueueExist(endpointName);

        QueueViewMBean queueViewMbean = getQueueViewMBean(endpointName);
        long queueSize = queueViewMbean.getQueueSize();
        queueViewMbean.purge();

        return Long.toString(queueSize);

    }

    public String browseMessage(String endpointName, String messageId, boolean excludeBody) throws Exception {

        String endpointType = checkIfEndpointExist(endpointName);

        // A quote in the id would otherwise end the string and change the selector
        String messageIdKey = "JMSMessageID='" + messageId.replace("'", "''") + "'";

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse(messageIdKey);

        return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

    }

    public String browseMessages(String endpointName, Integer page, Integer numberOfMessages, boolean excludeBody) throws Exception {

        String endpointType = checkIfEndpointExist(endpointName);

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        CompositeData[] messages = destinationViewMBean.browse();

        if(page != null && numberOfMessages != null) {
            messages =  getMessagesByPage(messages, page, numberOfMessages);
        }

        return CompositeDataConverter.convertToJSON(messages, null,false, excludeBody);

    }

    public String listMessages(String endpointName, String filter) throws Exception {

        String endpointType = checkIfEndpointExist(endpointName);

        CompositeData[] messages = getDestinationViewMBean(endpointType,endpointName).browse(filter);

        return CompositeDataConverter.convertToJSON(messages, 1000, true, true);

    }


    public String countMessages(String endpointName) throws Exception {

        String endpointType = checkIfEndpointExist(endpointName);

        long queueSize = getDestinationViewMBean(endpointType,endpointName).getQueueSize();

        return Long.toString(queueSize);

    }

    public String countMessagesFromList(String endpointList) throws Exception {

        long numberOfMessages = 0L;
        String[] endpointNames = endpointList.split(",\\s*");

        for(String endpointName: endpointNames){

            String endpointType = getEndpointType(endpointName);

            if(!endpointType.equals("Unknown")){
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

    public String getFlowMessageCountsList(boolean excludeEmptyQueues) {

        Map<String, Long> flowIdsMessageCountMap = getFlowIdsMessageCountMap(excludeEmptyQueues);

        return OBJECT_MAPPER.writeValueAsString(flowIdsMessageCountMap);
    }

    public String sendMessage(String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception {

        String endpointType = checkIfEndpointExist(endpointName);

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        Map<String, Object> headers = messageHeaders == null ? new HashMap<>() : new HashMap<>(messageHeaders);

        // A message without any header is sent persistent; with other headers the broker default applies
        if(headers.isEmpty() || headers.containsKey("JMSDeliveryMode")){
            headers.put("JMSDeliveryMode", isPersistent(headers.get("JMSDeliveryMode")) ? PERSISTENT : NON_PERSISTENT);
        }

        headers.remove("JMSTimestamp");

        destinationViewMBean.sendTextMessage(headers,messageBody);

        return "success";
    }

    private static boolean isPersistent(Object deliveryMode) {
        return deliveryMode == null || Set.of("PERSISTENT", "0", "2").contains(deliveryMode.toString().toUpperCase());
    }


    public String getConsumers() {
        return "n/a";
    }

    public String getConnections() throws Exception {

        JSONObject connectionsInfo  = new JSONObject();
        JSONObject connectionInfo = new JSONObject();

        Connection[] connections = broker.getRegionBroker().getClients();

        for(Connection connection: connections){
            JSONObject connectionDetails = new JSONObject();
            connectionDetails.put("connectionID", connection.getConnectionId());
            connectionDetails.put("clientAddress", connection.getRemoteAddress());
            connectionDetails.put("creationTime", Long.toString(connection.getStatistics().getStartTime()));
            connectionInfo.append("connection", connectionDetails);
        }

        connectionsInfo.put("connections",connectionInfo);

        return connectionsInfo.toString();


    }

    private CompositeData[] getMessagesByPage(CompositeData[] messages, Integer page, Integer numberOfMessages){

        if(page < 1 || numberOfMessages < 1){
            throw new IllegalArgumentException("page and numberOfMessages must be 1 or higher");
        }

        int startIndex = (int) Math.min((long) (page - 1) * numberOfMessages, messages.length);
        int endIndex = (int) Math.min((long) startIndex + numberOfMessages, messages.length);

        return Arrays.copyOfRange(messages, startIndex, endIndex);

    }


    private JSONObject getEndpoint(String endpointType, String endpointName) throws Exception {

        DestinationViewMBean destinationViewMBean = getDestinationViewMBean(endpointType, endpointName);

        JSONObject endpoint = new JSONObject();

        endpoint.put("name",endpointName);
        endpoint.put("address",destinationViewMBean.getName());
        endpoint.put("temporary", "false");
        if(endpointType.equalsIgnoreCase("Topic")){
            endpoint.put("numberOfMessages",destinationViewMBean.getEnqueueCount());
        }else{
            endpoint.put("numberOfMessages",destinationViewMBean.getQueueSize());
        }
        endpoint.put("numberOfConsumers",destinationViewMBean.getConsumerCount());

        return endpoint;

    }


    @Override
    public Object getBroker() {
        return broker;
    }

    public void setBrokerViewMBean() throws MalformedObjectNameException {
        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName());
        brokerViewMBean = (BrokerViewMBean) broker.getManagementContext().newProxyInstance(activeMQ, BrokerViewMBean.class, true);
    }

    public DestinationViewMBean getDestinationViewMBean(String destinationType, String destinationName) throws MalformedObjectNameException {
        return destinationView(destinationType, destinationName, DestinationViewMBean.class);
    }

    private <T> T destinationView(String destinationType, String destinationName, Class<T> view) throws MalformedObjectNameException {

        ObjectName activeMQ = new ObjectName("org.apache.activemq:type=Broker,brokerName=" + broker.getBrokerName() + ",destinationType=" + destinationType + ",destinationName=" + destinationName);
        return view.cast(broker.getManagementContext().newProxyInstance(activeMQ, view, true));

    }

    private static final List<String> INTERNAL_PREFIXES = List.of(
            "ActiveMQ.Advisory.",
            "ActiveMQ.Scheduler",
            "ActiveMQ.Plugin",
            "ActiveMQ.DLQ",
            "ID:"                 // temporary destinations, just in case
    );

    private boolean isInternalDestination(String destinationName) {
        return destinationName == null
                || INTERNAL_PREFIXES.stream().anyMatch(destinationName::startsWith);
    }

    private Map<String, Long> getFlowIdsMessageCountMap(boolean excludeEmptyQueues) {

        Map<String, Long> destinationMessageCounts = new HashMap<>();

        try {
            // No trailing ",*": matches only the destination MBeans, not consumer/producer subscriptions
            ObjectName pattern = new ObjectName("org.apache.activemq:type=Broker,brokerName="
                    + broker.getBrokerName()
                    + ",destinationType=Queue"
                    + ",destinationName=*");

            Set<ObjectName> destinations = broker.getManagementContext().queryNames(pattern, null);

            for (ObjectName destination : destinations) {
                String destinationName = destination.getKeyProperty("destinationName");

                if (isInternalDestination(destinationName)) {
                    continue;
                }

                DestinationViewMBean view = getDestinationViewMBean("Queue", destinationName);
                long messageCount = view.getQueueSize();

                if (messageCount > 0 || !excludeEmptyQueues) {
                    destinationMessageCounts.put(destinationName, messageCount);
                }
            }

        } catch (Exception e) {
            log.error("event=getFlowIdsMessageCountMap status=failed message=Failed to get all destinations and messages counts reason={}", e.getMessage(), e);
        }

        return destinationMessageCounts;
    }

    private QueueViewMBean getQueueViewMBean(String destinationName) throws MalformedObjectNameException {
        return destinationView("Queue", destinationName, QueueViewMBean.class);
    }

    private TopicViewMBean getTopicViewMBean(String destinationName) throws MalformedObjectNameException {
        return destinationView("Topic", destinationName, TopicViewMBean.class);
    }

    private JobSchedulerViewMBean getJobSchedulerViewMBean() throws Exception {

        ObjectName objectName = broker.getAdminView().getJMSJobScheduler();
        return (JobSchedulerViewMBean) broker.getManagementContext().newProxyInstance(objectName, JobSchedulerViewMBean.class, true);

    }

}