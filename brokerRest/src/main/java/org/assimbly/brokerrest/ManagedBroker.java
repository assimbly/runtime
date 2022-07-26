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
public class ManagedBroker {

	protected Logger log = LoggerFactory.getLogger(getClass());

    private Broker broker;

    private Broker classic = new ActiveMQClassic();

	private Broker artemis = new ActiveMQArtemis();

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
            log.info("Starting ActiveMQ broker");
            if (brokerConfigurationType.equals("file")) {
                status = broker.start();
            }else if (brokerConfigurationType.equals("embedded")) {
                status = broker.startEmbedded();
            }
            log.info("Started ActiveMQ Artemis broker");

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
    public String listMessages(String brokerType, String stepName, String filter, String mediaType) throws Exception {
        broker = getBroker(brokerType);
        result = broker.listMessages(stepName, filter);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String countMessages(String brokerType, String stepName) throws Exception {
        broker = getBroker(brokerType);
        result = broker.countMessages(stepName);

        return result;
    }

    public String sendMessage(String brokerType, String stepName, Map<String,Object> messageHeaders, String messageBody) throws Exception{
        broker = getBroker(brokerType);
        return broker.sendMessage(stepName, messageHeaders, messageBody);
    }

    public String browseMessage(String brokerType, String stepName, String messageId, String mediaType, boolean excludeBody) throws Exception{
        broker = getBroker(brokerType);
        result = broker.browseMessage(stepName, messageId, excludeBody);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String browseMessages(String brokerType, String stepName, Integer page, Integer numberOfMessages, String mediaType, boolean excludeBody) throws Exception{
        broker = getBroker(brokerType);
        result = broker.browseMessages(stepName, page, numberOfMessages, excludeBody);

        if(mediaType.equalsIgnoreCase("application/xml")){
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String removeMessage(String brokerType, String stepName, String messageId) throws Exception{
        broker = getBroker(brokerType);
        return broker.removeMessage(stepName, messageId);
    }

    public String removeMessages(String brokerType, String stepName) throws Exception{
        broker = getBroker(brokerType);
        return broker.removeMessages(stepName);
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
            throw new Exception("Unknown brokerType: valid values are classic or artemis");
        }
    }
}
