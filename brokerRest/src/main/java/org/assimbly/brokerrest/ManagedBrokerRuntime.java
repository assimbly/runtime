package org.assimbly.brokerrest;

import org.assimbly.broker.Broker;
import org.assimbly.broker.impl.ActiveMQArtemis;
import org.assimbly.broker.impl.ActiveMQClassic;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ManagedBrokerRuntime {

	protected Logger log = LoggerFactory.getLogger(getClass());
    private Broker broker;
    private final Broker classic = new ActiveMQClassic();
	private final Broker artemis = new ActiveMQArtemis();
	private String status;
    private String result;


    //Broker configuration
    public String getConfiguration(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.getFileConfiguration();
    }

    public String setConfiguration(String brokerType, String brokerConfigurationType, String brokerConfiguration) throws Exception {
        broker = getBroker(brokerType);
        return broker.setFileConfiguration(brokerConfiguration);
    }

    //Broker manage
    public String start(String brokerType, String brokerConfigurationType) throws Exception {

        log.info("Current ActiveMQ broker status: " + status + " (type=" + brokerType + ",configurationtype=" + brokerConfigurationType + ")");

        broker = getBroker(brokerType);
        status = getStatus(brokerType);

        if(status.equals("stopped")) {
            log.info("Starting ActiveMQ " + brokerType + " broker");
            if (brokerConfigurationType.equals("file")) {
                status = broker.start();
            }else if (brokerConfigurationType.equals("embedded")) {
                status = broker.startEmbedded();
            }
            log.info("Started ActiveMQ " + brokerType + " broker");

        }

        return status;

    }


    public String restart(String brokerType, String brokerConfigurationType) throws Exception {

        broker = getBroker(brokerType);
        status = getStatus(brokerType);

        if(status.startsWith("started")) {
            log.info("Restarting ActiveMQ broker");
            if (brokerConfigurationType.equals("file")) {
                status = broker.restart();
            }else if (brokerConfigurationType.equals("embedded")) {
                status = broker.restartEmbedded();
            }
            log.info("Restarted ActiveMQ broker");
        }

        return status;
    }


    public String stop(String brokerType) throws Exception {

        broker = getBroker(brokerType);
        status = getStatus(brokerType);

        if(status.startsWith("started")) {
             log.info("Stopping ActiveMQ broker");
             status = broker.stop();
        }

        return status;

    }

    public String getStatus(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.status();
    }

    public Map<String, Object> getStats(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.stats();
    }

    public String getInfo(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.info();
    }

    public String getConnections(String brokerType, String mediaType) throws Exception {
        broker = getBroker(brokerType);

        result = broker.getConnections();

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String getConsumers(String brokerType, String mediaType) throws Exception {
        broker = getBroker(brokerType);
        result = broker.getConsumers();

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    //Manage queues
    public String createQueue(String brokerType, String queueName) throws Exception {
        broker = getBroker(brokerType);
        return broker.createQueue(queueName);
    }

    public String deleteQueue(String brokerType, String queueName) throws Exception {
        broker = getBroker(brokerType);
        return broker.deleteQueue(queueName);
    }

    public String getQueue(String brokerType, String queueName, String mediaType) throws Exception {
        broker = getBroker(brokerType);
        result = broker.getQueue(queueName);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String getQueues(String brokerType, String mediaType) throws Exception {
        broker = getBroker(brokerType);

        result = broker.getQueues();

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String clearQueue(String brokerType, String queueName) throws Exception {
        broker = getBroker(brokerType);
        return broker.clearQueue(queueName);
    }

    public String clearQueues(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.clearQueues();
    }

    //Manage topics
    public String createTopic(String brokerType, String topicName) throws Exception {
        broker = getBroker(brokerType);
        return broker.createTopic(topicName);
    }

    public String deleteTopic(String brokerType, String topicName) throws Exception {
        broker = getBroker(brokerType);
        return broker.deleteTopic(topicName);
    }

    public String clearTopic(String brokerType, String topicName) throws Exception {
        broker = getBroker(brokerType);
        return broker.clearTopic(topicName);
    }

    public String clearTopics(String brokerType) throws Exception {
        broker = getBroker(brokerType);
        return broker.clearTopics();
    }

    public String getTopic(String brokerType, String topicName, String mediaType) throws Exception{
        broker = getBroker(brokerType);
        result = broker.getTopic(topicName);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String getTopics(String brokerType, String mediaType) throws Exception{
        broker = getBroker(brokerType);
        result = broker.getTopics();

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    //manage messages
    public String listMessages(String brokerType, String endpointName, String filter, String mediaType) throws Exception {

        broker = getBroker(brokerType);
        result = broker.listMessages(endpointName, filter);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String countMessagesFromList(String brokerType, String endpointNames) throws Exception {

        broker = getBroker(brokerType);
        result = broker.countMessagesFromList(endpointNames);

        return result;
    }

    public String getFlowMessageCountsList(String brokerType, boolean excludeEmptyQueues) throws Exception {

        broker = getBroker(brokerType);
        result = broker.getFlowMessageCountsList(excludeEmptyQueues);

        return result;
    }

    public String countMessages(String brokerType, String endpointName) throws Exception {

        broker = getBroker(brokerType);
        result = broker.countMessages(endpointName);

        return result;
    }

    public String countDelayedMessages(String brokerType, String endpointName) throws Exception {

        broker = getBroker(brokerType);
        result = broker.countDelayedMessages(endpointName);

        return result;
    }


    public String sendMessage(String brokerType, String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception{
        broker = getBroker(brokerType);
        return broker.sendMessage(endpointName, messageHeaders, messageBody);
    }

    public String browseMessage(String brokerType, String endpointName, String messageId, String mediaType, boolean excludeBody) throws Exception{
        broker = getBroker(brokerType);
        result = broker.browseMessage(endpointName, messageId, excludeBody);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String browseMessages(String brokerType, String endpointName, Integer page, Integer numberOfMessages, String mediaType, boolean excludeBody) throws Exception{
        broker = getBroker(brokerType);
        result = broker.browseMessages(endpointName, page, numberOfMessages, excludeBody);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String removeMessage(String brokerType, String endpointName, String messageId) throws Exception{
        broker = getBroker(brokerType);
        return broker.removeMessage(endpointName, messageId);
    }

    public String removeMessages(String brokerType, String endpointName) throws Exception{
        broker = getBroker(brokerType);
        return broker.removeMessages(endpointName);
    }

    public String moveMessage(String brokerType, String sourceQueueName, String targetQueueName, String messageId) throws Exception{
        broker = getBroker(brokerType);
        return broker.moveMessage(sourceQueueName, targetQueueName, messageId);
    }

    public String moveMessages(String brokerType, String sourceQueueName, String targetQueueName) throws Exception{
        broker = getBroker(brokerType);
        return broker.moveMessages(sourceQueueName, targetQueueName);
    }

    //private methods
	private Broker getBroker(String brokerType) throws Exception {
        if (brokerType.equalsIgnoreCase("classic")) {
            return classic;
        }else if (brokerType.equalsIgnoreCase("artemis")) {
            return artemis;
        }else{
            throw new IllegalArgumentException("Unknown brokerType: valid values are classic or artemis");
        }
    }
}
