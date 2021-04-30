package org.assimbly.broker;

import java.io.IOException;
import java.util.Map;

/**
* <pre>
* This interface is meant to configure and manage a broker.
*
* A <b>broker</b> is a message broker.
* A <b>broker</b> implements queueing and/or topics.

* For a valid configuration check the website for the used implementation
* </pre>
*/
public interface Broker {

	//configure broker
	/**
	* Get the broker configuration as XML from the broker path. 
	* A default configuration is return when no configuration is found.
	*
    * @return broker configuration as XML 
	* @throws IOException if configuration can't be retrieved
	*/
	public String getFileConfiguration() throws IOException;;
	
	/**
	* Sets the broker configuration from a XML 
	* A default configuration is return when brokerConfiguration param is an empty string.
	*
	* @param  brokerConfiguration (XML)	
	* @return String (confirm when configuration is set) 
	* @throws IOException if configuration can't be set
	*/
	public String setFileConfiguration(String brokerConfiguration) throws IOException;
	
	
	/**
	* sets the connector base directory. In this directory everything is stored (alert, events) 
	*
	* @param  baseDirectory (path) 
	* @throws Exception if base directory can't be set is not available
	*/
	public void setBaseDirectory(String baseDirectory) throws Exception;
	
	//manage broker
	
	/**
	* Starts the broker from a file configuration. 
	*
    * @return status of broker 
	* @throws Exception if broker can't be started
	*/
	public String start() throws Exception;

	/**
	* Starts an embedded broker on a localhost (use this for testing). 
	*
    * @return status of broker 
	* @throws Exception if broker can't be started	
	*/
	public String startEmbedded() throws Exception;
	
	/**
	* Stops the broker. 
	*
    * @return status of broker 
	* @throws Exception if broker can't be stopped
	*/
	public String stop() throws Exception;
	
	/**
	* Restarts the broker from a file configuration. 
	*
    * @return status of broker 
	* @throws Exception if broker can't be restarted
	*/
	public String restart() throws Exception; 
	
	/**
	* Restarts the embedded broker 
	* 
    * @return status of broker 
	* @throws Exception if broker can't be started
	*/
	public String restartEmbedded() throws Exception;
	
	/**
	* Status of the broker: "started" or "stopped" 
	*
    * @return status of broker 
	* @throws Exception if broker can't be started
	*/
	public String status() throws Exception;
	
	/**
	* Info of the running broker. This is comma separated string 
	*
    * @return info broker 
	* @throws Exception if info can't get retrieved
	*/
	public String info() throws Exception;

	public String getConsumers() throws Exception;

	public String getConnections() throws Exception;

	public String createQueue(String queueName) throws Exception;

	public String deleteQueue(String queueName) throws Exception;

	public String getQueue(String queueName) throws Exception;

	public String getQueues() throws Exception;

	public String clearQueue(String queueName) throws Exception;

	public String clearQueues() throws Exception;


	public String listMessages(String endpointName, String filter) throws Exception;

	public String removeMessage(String endpointName, int messageId) throws Exception;

	public String removeMessages(String endpointName) throws Exception;

	public String moveMessage(String sourceQueueName, String targetQueueName, String message) throws Exception;

	public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception;

	public String browseMessage(String endpointName, String messageId) throws Exception;

	public String browseMessages(String endpointName) throws Exception;

	public String sendMessage(String endpointName, Map<String,String> messageHeaders, String messageBody) throws Exception;


	/**
	* Get the broker (can be used to access extended methods by the implementation (ActiveMQ classic, ActiveMQ artemis)
	* Note: You need to cast the object based on the implementation you are calling. And...calling this you're on your own :)
	*
	* @return returns broker as object
	* @throws Exception if broker can't be found
	*/
	public Object getBroker() throws Exception;
}