package org.assimbly.connector;

import java.io.IOException;

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
	* Get the broker (can be used to access extended methods by the implementation (ActiveMQ classic, ActiveMQ artemis)
	* Note: You need to cast the object based on the implementation you are calling. And...calling this you're on your own :)
	*
	* @return returns broker as object
	* @throws Exception if broker can't be found
	*/
	public Object getBroker() throws Exception;
}