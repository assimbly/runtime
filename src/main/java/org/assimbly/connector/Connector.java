package org.assimbly.connector;

import java.net.URI;
import java.util.List;
import java.util.TreeMap;

/**
* <pre>
* This interface is meant to configure and manage a connector.
*
* A <b>Connector</b> is a collection of flows.
* A <b>Flow</b> connects one or more endpoints for example a database and a directory.
* 
* Each flow configuration consists of a Treemap&lt;key,value&gt;. The connector configuration
* consists of a list of flow configurations.
*
* For a valid flow configuration see
* <a href="https://github.com/assimbly/connector">https://github.com/assimbly/connector</a>
* </pre>
*/
public interface Connector {

	//configure connector
	/**
	* Sets the connector configuration from a list of flow configurations (TreeMaps). This list
	* is cleared after a connector is reinitialized.
	*
	* @param  configuration list of flow configurations (Treemaps)
	* @throws Exception if configuration can't be set
	*/
	public void setConfiguration(List<TreeMap<String,String>> configuration) throws Exception;
	/**
	* gets the connector configuration currently set (in use)
	*
	* @return list of flow configurations
	* @throws Exception if configuration can't be retrieved or is not available
	*/
	public List<TreeMap<String,String>> getConfiguration() throws Exception;

	//convert connector configuration from/to XML
	/**
	* Converts the configuration currently in use to XML
	*
	* @param  connectorId the id of a connector
	* @param  configuration list of flow configurations
	* @return returns XML Configuration as String
	* @throws Exception if XML can't be created
	*/
	public String convertConfigurationToXML(String connectorId,List<TreeMap<String,String>> configuration) throws Exception;
	/**
	* Converts a XML configuration to a connector configuration (list of flow configurations)
	*
	* @param  connectorId the id of a connector
	* @param  xmlConfiguration XML configuration in String format
	* @return returns connector configuration (List of flow configurations)
	* @throws Exception if XML can't be converted to a Treemap
	*/
	public List<TreeMap<String,String>> convertXMLToConfiguration(String connectorId, String xmlConfiguration) throws Exception;
	/**
	* Converts a XML configuration to a connector configuration (list of flow configurations)
	*
	* @param  connectorId the id of a connector
	* @param  configurationUri URI to the XML configuration (This can be a file location or an URL)
	* @return returns connector configuration (List of flow configurations)
	* @throws Exception if XML can't be converted to a Treemap
	*/
	public List<TreeMap<String,String>> convertXMLToConfiguration(String connectorId, URI configurationUri) throws Exception;

	//convert connector configuration from/to JSON

	/**
	* Converts a JSON configuration to a connector configuration (list of flow configurations)
	*
	* @param  connectorId the id of a connector
	* @param  jsonConfiguration JSON configuration in String format
	* @return returns connector configuration (List of flow configurations)
	* @throws Exception if XML can't be converted to a Treemap
	*/
	public List<TreeMap<String,String>> convertJSONToConfiguration(String connectorId, String xmlConfiguration) throws Exception;
	
	/**
	* Converts the configuration currently in use to JSON
	*
	* @param  connectorId the id of a connector
	* @param  configuration list of flow configurations
	* @return returns XML Configuration as String
	* @throws Exception if XML can't be created
	*/
	public String convertConfigurationToJSON(String connectorId,List<TreeMap<String,String>> configuration) throws Exception;

	
	//configure flow
	/**
	* Sets the flow configuration from a Tree (keyvalues). This list
	* is cleared after a connector is reinitialized.
	*
	* @param  configuration of a flow configuration
	* @throws Exception if configuration can't be set
	*/
	public void setFlowConfiguration(TreeMap<String,String> configuration) throws Exception;	

	/**
	* gets the flow configuration for a specific if currently set
	*
	* @param  flowId the id of a flow
	* @return flow configuration
	* @throws Exception if configuration can't be retrieved or is not available
	*/	
	public TreeMap<String,String> getFlowConfiguration(String flowId) throws Exception;

	
	//convert flow configuration from/to XML
	/**
	* Converts a XML configuration to a flow configuration
	*
	* @param  flowId the id of a flow
	* @param  configuration XML configuration as string
	* @return returns flow configuration (Treemap)
	* @throws Exception if XML can't be converted to a Treemap
	*/	
	public TreeMap<String,String> convertXMLToFlowConfiguration(String flowId, String configuration) throws Exception;
	
	/**
	* Converts a XML configuration to a flow configuration
	*
	* @param  flowId the id of a flow
	* @param  configurationUri URI to the XML configuration (This can be a file location or an URL)
	* @return returns flow configuration (Treemap)
	* @throws Exception if XML can't be converted to a Treemap
	*/	
	public TreeMap<String,String> convertXMLToFlowConfiguration(String flowId, URI configurationUri) throws Exception;
	
	/**
	* Converts a flow configuration to a XML configuration
	*
	* @param  configuration treemap
	* @return returns XML as String
	* @throws Exception if XML can't be converted to a Treemap
	*/
	public String convertFlowConfigurationToXML(TreeMap<String,String> configuration) throws Exception;

	
	//convert flow configuration from/to JSON
	/**
	* Converts a JSON configuration to a flow configuration
	*
	* @param  flowId the id of a flow
	* @param  configuration JSON configuration as string
	* @return returns flow configuration (Treemap)
	* @throws Exception if JSON can't be converted to a Treemap
	*/	
	public TreeMap<String,String> convertJSONToFlowConfiguration(String flowId, String configuration) throws Exception;
	
	/**
	* Converts a JSON configuration to a flow configuration
	*
	* @param  flowId the id of a flow
	* @param  configurationUri URI to the XML configuration (This can be a file location or an URL)
	* @return returns flow configuration (Treemap)
	* @throws Exception if JSON can't be converted to a Treemap
	*/	
	//todo
	//public TreeMap<String,String> convertJSONToFlowConfiguration(String flowId, URI configurationUri) throws Exception;
	
	/**
	* Converts a flow configuration to a JSON configuration
	*
	* @param  configuration treemap
	* @return returns JSON as String
	* @throws Exception if JSON can't be converted to a Treemap
	*/
	public String convertFlowConfigurationToJSON(TreeMap<String,String> configuration) throws Exception;

	
	//manage connector
	/**
	* Starts a connector. The connector acts like a container for flows.  
	* After starting it can be configured
	*
	* @throws Exception if connector doesn't start
	*/
	public void start() throws Exception;

	/**
	* Stops a connector
	*
	* @throws Exception if connector doesn't start
	*/
	public void stop() throws Exception;

	/**
	* Checks if a connector is started  
	*
	* @return returns true if connector is started
	*/
	public boolean isStarted();
	
	//manage flow
	/**
	* Checks if a flow is a part of connector
	*
	* @param  flowId the id of a flow
	* @return returns true if started.
	*/
	public boolean hasFlow(String flowId);

	/**
	* removes flow from connector
	*
	* @param  flowId the id of a flow
	* @return returns true if removed.
	 * @throws Exception 
	*/
	public boolean removeFlow(String flowId) throws Exception;
	
	/**
	* Starts a flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	*/	
	public String startFlow(String flowId) throws Exception;

	/**
	* Restarts a flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	*/
	public String restartFlow(String flowId) throws Exception;
	
	/**
	* Stops a flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	*/
	public String stopFlow(String flowId) throws Exception;
	
	/**
	* Resumes a flow if paused
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	*/
	public String resumeFlow(String flowId) throws Exception;

	/**
	* Pauses a flow if started
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	*/
	public String pauseFlow(String flowId) throws Exception;
	
	/**
	* Gets the status of a flow 
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns true (stopped, started, paused).
	*/
	public String getFlowStatus(String flowId) throws Exception;

	/**
	* Gets the status of a flow 
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns true (stopped, started, paused).
	*/
	public String getFlowUptime(String flowId) throws Exception;	
	
	
	/**
	* Get the context of connector (can be used to access extended methods by the implementation (Camel, Spring)
	* Note: Calling this you're on your own...
	*
	* @return returns context as object
	* @throws Exception if context can't be found
	*/
	public Object getContext() throws Exception;
	
}