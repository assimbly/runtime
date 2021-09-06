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

	/**
	 * Get all consumers of the broker
	 *
	 * @return list of broker consumers
	 * @throws Exception if consumers can't be retrieved
	 */
	public String getConsumers() throws Exception;

	/**
	 * Get all connections of the broker
	 *
	 * @return list of broker connections
	 * @throws Exception if connections can't be retrieved
	 */
	public String getConnections() throws Exception;

	/**
	 * Creates a new queue
	 *
	 * @param  queueName Name of the queue
	 * @return String (confirmation when queue is created)
	 * @throws Exception if queue can't be created
	 */
	public String createQueue(String queueName) throws Exception;

	/**
	 * Deletes a queue
	 *
	 * @param  queueName Name of the queue
	 * @return String (confirmation when queue is deleted)
	 * @throws Exception if queue can't be deleted
	 */
	public String deleteQueue(String queueName) throws Exception;


	/**
	 * gets information about the specified queues. Information like:
	 * - name of the queue
	 * - address of the queue
	 * - number of messages
	 *
	 * @return String (queue information)
	 * @param  queueName Name of the queue
	 * @throws Exception if queue information can't be retrieved
	 */
	public String getQueue(String queueName) throws Exception;

	/**
	 * gets information about the one or more queues. Information like:
	 * - name of the queue
	 * - address of the queue
	 * - number of messages
	 *
	 * @return String (list of queues with information on every queue)
	 * @throws Exception if queue information can't be retrieved
	 */
	public String getQueues() throws Exception;

	/**
	 * Clears the specified queue. The deletes/purge all messages on the queue
	 *
	 * @param  queueName Name of the queue
	 * @return String (confirmation when queue is cleared)
	 * @throws Exception if queue can't be cleared
	 */
	public String clearQueue(String queueName) throws Exception;

	/**
	 * Clears all the queues. The deletes/purge all messages on the broker (use with care)
	 *
	 * @return String (confirmation when broker (all queues) is cleared)
	 * @throws Exception if queues can't be cleared
	 */
	public String clearQueues() throws Exception;

	/**
	 * Creates a new topic
	 *
	 * @param  topicName Name of the topic
	 * @return String (confirmation when topic is created)
	 * @throws Exception if topic can't be created
	 */
	public String createTopic(String topicName) throws Exception;

	/**
	 * Deletes a topic
	 *
	 * @param  topicName Name of the queue
	 * @return String (confirmation when topic is deleted)
	 * @throws Exception if topic can't be deleted
	 */
	public String deleteTopic(String topicName) throws Exception;


	/**
	 * Clears the specified queue. The deletes/purge all messages on the queue
	 *
	 * @param  topicName Name of the topic
	 * @return String (confirmation when topic is cleared)
	 * @throws Exception if topic can't be cleared
	 */
	public String clearTopic(String topicName) throws Exception;

	/**
	 * Clears all the topics. The deletes/purge all messages on the broker (use with care)
	 *
	 * @return String (confirmation when broker (all topics) is cleared)
	 * @throws Exception if topics can't be cleared
	 */
	public String clearTopics() throws Exception;

	/**
	 * gets information about the specified queues. Information like:
	 * - name of the topic
	 * - address of the topic
	 * - number of messages
	 *
	 * @return String (topic information)
	 * @param  topicName Name of the topic
	 * @throws Exception if topic information can't be retrieved
	 */
	public String getTopic(String topicName) throws Exception;

	/**
	 * gets information about the one or more topics. Information like:
	 * - name of the topic
	 * - address of the topic
	 * - number of messages
	 *
	 * @return String (list of topics with information on every queue)
	 * @throws Exception if topic information can't be retrieved
	 */
	public String getTopics() throws Exception;

	/**
	 * list all messages on the broker
	 *
	 * @param  endpointName (name of queue or topic)
	 * @param  filter (JMS Selector)
	 * @return List of all messages
	 * @throws Exception if list can't be retrieved
	 */
	public String listMessages(String endpointName, String filter) throws Exception;

	/**
	 * list all messages on the broker
	 *
	 * @param  endpointName (name of queue or topic)
	 * @return List of all messages
	 * @throws Exception if list can't be retrieved
	 */
	public String countMessages(String endpointName) throws Exception;


	/**
	 * removes a message on specified endpoint by messageId
	 *
	 * @param  endpointName (name of queue or topic)
	 * @return confirmation if message is removed
	 * @throws Exception if message can't be removed
	 */
	public String removeMessage(String endpointName, String messageId) throws Exception;

	/**
	 * removes all message on the specified endpoint
	 *
	 * @param  endpointName (name of queue or topic)
	 * @return confirmation if message is removed
	 * @throws Exception if messages can't be removed
	 */
	public String removeMessages(String endpointName) throws Exception;

	/**
	 * moves a message on specified queueu by messageId to another queue
	 *
	 * @param  sourceQueueName (name of queue)
	 * @param  targetQueueName (name of queue)
	 * @return confirmation if message is moved
	 * @throws Exception if message can't be moved
	 */
	public String moveMessage(String sourceQueueName, String targetQueueName, String message) throws Exception;

	/**
	 * moves all messages on specified queueu to another queue
	 *
	 * @param  sourceQueueName (name of queue)
	 * @param  targetQueueName (name of queue)
	 * @return confirmation if message is moved
	 * @throws Exception if message can't be moved
	 */
	public String moveMessages(String sourceQueueName, String targetQueueName) throws Exception;

	/**
	 * browse a message on specified endpoint by messageId
	 *
	 * @param  endpointName (name of queue or topic)
	 * @return message with headers and content
	 * @throws Exception if message can't be retrieved
	 */
	public String browseMessage(String endpointName, String messageId, boolean excludeBody) throws Exception;

	/**
	 * browse all message on specified endpoint
	 *
	 * @param  endpointName (name of queue or topic)
	 * @param  page The number of the page to return
	 * @param  numberOfMessages The number of messages on the specified page
	 * @return messages with headers and content
	 * @throws Exception if message can't be retrieved
	 */
	public String browseMessages(String endpointName, Integer page, Integer numberOfMessages, boolean excludeBody) throws Exception;

	/**
	 * send a message to the specified endpoint
	 *
	 * @param  endpointName (name of queue or topic)
	 * @param  messageHeaders Map with key/values
	 * @param  messageBody The content/playload of a messsage
	 * @return messages with headers and content
	 * @throws Exception if message can't be retrieved
	 */
	public String sendMessage(String endpointName, Map<String,Object> messageHeaders, String messageBody) throws Exception;


	/**
	* Get the broker (can be used to access extended methods by the implementation (ActiveMQ classic, ActiveMQ artemis)
	* Note: You need to cast the object based on the implementation you are calling. And...calling this you're on your own :)
	*
	* @return returns broker as object
	* @throws Exception if broker can't be found
	*/
	public Object getBroker() throws Exception;
}