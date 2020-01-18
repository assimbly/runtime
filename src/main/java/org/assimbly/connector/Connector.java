package org.assimbly.connector;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.spi.EventNotifier;

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
	* Sets the connector configuration from a list of flow configurations (TreeMaps<Key,Value>). 
	* The configuration cleared after a connector is reinitialized.
	*
	* @param  configuration: list of flow configurations (Treemaps)
	* @throws Exception if configuration can't be set
	*/
	public void setConfiguration(List<TreeMap<String,String>> configuration) throws Exception;

	/**
	* Sets the connector configuration from a string of a specific format (XML,JSON,YAML). 
	* The configuration cleared after a connector is reinitialized.
	*
	* @param  connctorId
	* @param  mediatype (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)	
	 * @return 
	* @throws Exception if configuration can't be set
	*/
	public void setConfiguration(String connectorId, String mediaType, String configuration) throws Exception;
	
	/**
	* gets the connector configuration currently set (in use). 
	*
	* @return list of flow configurations (Treemap<key,value>)
	* @throws Exception if configuration can't be retrieved or isn't available
	*/
	public List<TreeMap<String,String>> getConfiguration() throws Exception;

	/**
	* gets the connector configuration currently set (in use). 
	*
	* @param  connctorId
	* @param  mediatype (XML,JSON,YAML)
	* @return list of flow configurations (String of mediatype)
	* @throws Exception if configuration can't be retrieved or isn't available
	*/
	public String getConfiguration(String connectorId, String mediaType) throws Exception;
	
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
	* Sets the flow configuration from a string for a specific format (XML,JSON,YAML). This list
	* is cleared after a connector is reinitialized.
	*
	* @param  flowID
	* @param  mediatype (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
	 * @return 
	* @throws Exception if configuration can't be set
	*/
	public void setFlowConfiguration(String flowId, String mediaType, String configuration) throws Exception;
	
	/**
	* gets the flow configuration for a specific if currently set
	*
	* @param  flowId the id of a flow
	* @return flow configuration
	* @throws Exception if configuration can't be retrieved or is not available
	*/	
	public TreeMap<String,String> getFlowConfiguration(String flowId) throws Exception;
	
	/**
	* gets the connector configuration currently set (in use). 
	*
	* @param  flowID
	* @param  mediatype (XML,JSON,YAML)
	* @return list of flow configurations (String of mediatype)
	* @throws Exception if configuration can't be retrieved or is not available
	*/
	public String getFlowConfiguration(String flowId, String mediaType) throws Exception;

	
	/**
	* sets the connector base directory. In this directory everything is stored (alert, events) 
	*
	* @param  baseDirectory (path) 
	* @return list of flow configurations (String of mediatype)
	* @throws Exception if base directory can't be set is not available
	*/
	public void setBaseDirectory(String baseDirectory) throws Exception;
	
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

	/**
	* Adds event notifier to notified about events  
	* 
	*/
	public void addEventNotifier(EventNotifier eventNotifier) throws Exception;

	
	
	//manage flow
	/**
	* Checks if a flow is a part of connector
	*
	* @param  flowId the id of a flow
	* @return returns true if started.
	*/
	public boolean hasFlow(String flowId);

	/**
	* Validates the uri + options
	*
	* @param  flowId the id of a flow
	* @return returns true if valid.
	*/
	public String validateFlow(String flowId);
	
	/**
	* Gets the stats of a connector
	*
	* @param  type of stats ("default" or "history")
	* @param  mediatype (xml or json)
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getStats(String statsType, String mediaType) throws Exception;	

	/**
	* Gets the version of documentation/integration framework 
	*
	* @throws Exception if version couldn't retrieved
	* @return returns documentation version
	*/
	public String getDocumentationVersion() throws Exception;	

	
	/**
	* Gets the documentation of a component
	*
	* @param  type of component (for example 'file')
 	* @param  type of dataform (xml or json)
	* @throws Exception if documenation couldn't get found
	* @return returns documentation
	*/
	public String getDocumentation(String componentType, String mediaType) throws Exception;	

	/**
	* Gets the documentation/schema of a component
	*
	* @param  type of component (for example 'file')
 	* @param  type of dataform (xml or json)
	* @throws Exception if documentation couldn't get found
	* @return returns documenation
	*/
	public String getComponentSchema(String componentType, String mediaType) throws Exception;	

	/**
	* Gets the parameters of a component
	*
	* @param  type of component (for example 'file')
 	* @param  type of dataform (xml or json)
	* @throws Exception if documentation couldn't get found
	* @return returns list of options
	*/
	public String getComponentParameters(String componentType, String mediaType) throws Exception;		
	
	/**
	* Gets the last error of a connector
	*
	* @throws Exception if error couldn't be retrieved
	* @return the last error or 0 if no error
	*/
	public String getLastError() throws Exception;	

	/**
	* Gets TLS certificates for a url.
	*  
	* Download the chain of certificates for the specified url
	*
	* @throws Exception if certificates cannot be downloaded
	*/
	public Certificate[] getCertificates(String url) throws Exception;	

	/**
	* Gets TLS certificates for a url.
	*  
	* Download the chain of certificates for the specified url
	*
	* @throws Exception if certificates cannot be downloaded
	*/
	public Certificate getCertificateFromTruststore(String certificateName) throws Exception;	

	
	/**
	* Sets TLS certificates.
	*  
	* Download and import certificates to truststore (jks) used by the connector
	*
	* @throws Exception if certificates cannot be imported
	*/
	public void setCertificatesInTruststore(String url) throws Exception;	

	
	/**
	* Import TLS certificate.
	*  
	* Import certificate into truststore (jks) used by the connector
	*
	* @throws Exception if certificates cannot be imported
	*/
	public String importCertificateInTruststore(String certificateName, Certificate certificate) throws Exception;
	
	
	/**
	* Import TLS certificates.
	*  
	* Import certificates to truststore (jks) used by the connector
	*
	* @throws Exception if certificates cannot be imported
	*/
	public Map<String,Certificate> importCertificatesInTruststore(Certificate[] certificates) throws Exception;
	
	/**
	* Delete certificate from key/truststore
	*  
	* @throws Exception if certificates cannot be deleted
	*/
	public void deleteCertificatesInTruststore(String certificateName) throws Exception;	

	
	
	/**
	* removes flow from connector
	*
	* @param  flowId the id of a flow
	* @return returns true if removed.
	 * @throws Exception 
	*/
	public boolean removeFlow(String flowId) throws Exception;
	
	/**
	* Starts all configured flows
	*
	* @throws Exception if one of the flows doesn't start
	*/	
	public String startAllFlows() throws Exception;

	/**
	* Restarts all configured flows
	*
	* @throws Exception if one of the flows doesn't stop
	*/	
	public String restartAllFlows() throws Exception;

	/**
	* Starts all configured flows
	*
	* @throws Exception if one of the flows doesn't start
	*/	
	public String pauseAllFlows() throws Exception;

	/**
	* Resume all configured flows
	*
	* @throws Exception if one of the flows doesn't resume
	*/	
	public String resumeAllFlows() throws Exception;

	/**
	* Stops all configured flows
	*
	* @throws Exception if one of the flows doesn't stop
	*/	
	public String stopAllFlows() throws Exception;
	
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
	* @return returns data/time in human readable format
	*/
	public String getFlowUptime(String flowId) throws Exception;	

	/**
	* Gets the number of messages a flow has prcessed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowLastError(String flowId) throws Exception;	

	/**
	* Gets the last error of a flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowTotalMessages(String flowId) throws Exception;	

	/**
	* Gets the total number of messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowCompletedMessages(String flowId) throws Exception;	

	/**
	* Gets the total number of failed messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowFailedMessages(String flowId) throws Exception;	

	/**
	* Gets the failure log for the specified flow
	*
	* @param  flowId the id of the flow
	* @param  numberOfEntries (maximum number of entries to return)
	* @throws Exception if log cannot be retrieved
	* @return failure log events (comma separated)
	*/	
	public String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception;	

	/**
	* Gets number of entries in (todays) failed log of flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if log cannot be retrieved
	* @return number of flow failures
	*/	
	public String getFlowAlertsCount(String id) throws Exception;	

	/**
	* Gets number of entries in (todays) failed log of all configured/running flows
	*
	* @throws Exception if log cannot be retrieved
	* @return failure log events (comma separated)
	*/	
	public TreeMap<String, String> getConnectorAlertsCount() throws Exception;
	
	/**
	* Gets the event log for the specified flow (start events,stop events and message failures)
	*
	* @param  flowId the id of the flow
	* @param  numberOfEntries (maximum number of entries to return)
	* @throws Exception if log cannot be retrieved
	* @return flow log events (comma separated)
	*/		
	public String getFlowEventsLog(String id, Integer numberOfEntries) throws Exception;	
	
	/**
	* Gets the details stats of a flow
	*
	* @param  flowId the id of the flow
	* @param  mediatype (xml or json)
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowStats(String flowId, String mediaType) throws Exception;	
	
	/**
	* Get the context of connector (can be used to access extended methods by the implementation (Camel, Spring)
	* Note: You need to cast the object based on the implementation you are calling. And...calling this you're on your own :)
	*
	* @return returns context as object
	* @throws Exception if context can't be found
	*/
	public Object getContext() throws Exception;
	
}