package org.assimbly.connector;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
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
	* Sets the connector configuration from a list of flow configurations (TreeMap&lt; Key,Value&gt;). 
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
	* @param  connectorId ID of the connector 
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)	
	* @throws Exception if configuration can't be set
	*/
	public void setConfiguration(String connectorId, String mediaType, String configuration) throws Exception;
	
	/**
	* gets the connector configuration currently set (in use). 
	*
	* @return list of flow configurations (Treemap&lt;key,value&gt;)
	* @throws Exception if configuration can't be retrieved or isn't available
	*/
	public List<TreeMap<String,String>> getConfiguration() throws Exception;

	/**
	* gets the connector configuration currently set (in use). 
	*
	* @param  connectorId ID of the connector
	* @param  mediaType (XML,JSON,YAML)
	* @return list of flow configurations (String of mediaType)
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
	* @param  flowId Id of the flow (String)
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
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
	* @param  flowId ID of the flow (String)
	* @param  mediaType (XML,JSON,YAML)
	* @return list of flow configurations (String of mediaType)
	* @throws Exception if configuration can't be retrieved or is not available
	*/
	public String getFlowConfiguration(String flowId, String mediaType) throws Exception;
	
	/**
	* sets the connector base directory. In this directory everything is stored (alert, events) 
	*
	* @param  baseDirectory (path) 
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
	* @param  eventNotifier eventNotifier object
	* @throws Exception if eventNotifier
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
	* @param  statsType type of stats ("default" or "history")
	* @param  mediaType (xml or json)
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
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
	* @throws Exception if documenation couldn't get found
	* @return returns documentation
	*/
	public String getDocumentation(String componentType, String mediaType) throws Exception;	

	/**
	* Gets the documentation/schema of a component
	*
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
	* @throws Exception if documentation couldn't get found
	* @return returns documenation
	*/
	public String getComponentSchema(String componentType, String mediaType) throws Exception;	

	/**
	* Gets the parameters of a component
	*
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
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
	* @param url an https url
 	* @return returns a map with certificates for this url
	* @throws Exception if certificates cannot be downloaded
	*/
	public Certificate[] getCertificates(String url) throws Exception;	

	/**
	* Gets TLS certificates for a url.
	*  
	* Download the chain of certificates for the specified url
	*
	* @param certificateName name of the certificate
	* @return returns the Certificate object
	* @throws Exception if certificates cannot be downloaded
	*/
	public Certificate getCertificateFromTruststore(String certificateName) throws Exception;	

	
	/**
	* Sets TLS certificates.
	*  
	* Download and import certificates to truststore (jks) used by the connector
	*
	* @param url an https url
	* @throws Exception if certificates cannot be imported
	*/
	public void setCertificatesInTruststore(String url) throws Exception;	

	
	/**
	* Import TLS certificate.
	*  
	* Import certificate into truststore (jks) used by the connector
	*
	* @param certificateName name of the certificate
	* @param certificate Java certificate object
 	* @return returns a confirmation message
	* @throws Exception if certificates cannot be imported
	*/
	public String importCertificateInTruststore(String certificateName, Certificate certificate) throws Exception;
	
	
	/**
	* Import TLS certificates.
	*  
	* Import certificates to truststore (jks) used by the connector
	*
	* @param certificates map with one or more Java certificate object
	* @return returns a map with certificate name and Java certificate object
	* @throws Exception if certificates cannot be imported
	*/
	public Map<String,Certificate> importCertificatesInTruststore(Certificate[] certificates) throws Exception;
	
	/**
	* Delete certificate from key/truststore
	* 
	* @param certificateName name of the certificate
	* @throws Exception if certificates cannot be deleted
	*/
	public void deleteCertificatesInTruststore(String certificateName) throws Exception;	
	
	/**
	* removes flow from connector
	*
	* @param  flowId the id of a flow
	* @return returns true if removed.
	 * @throws Exception if flow cannot be removed
	*/
	public boolean removeFlow(String flowId) throws Exception;
	
	/**
	* Starts all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't start
	*/	
	public String startAllFlows() throws Exception;

	/**
	* Restarts all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't stop
	*/	
	public String restartAllFlows() throws Exception;

	/**
	* Starts all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't start
	*/	
	public String pauseAllFlows() throws Exception;

	/**
	* Resume all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't resume
	*/	
	public String resumeAllFlows() throws Exception;

	/**
	* Stops all configured flows
	* 
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't stop
	*/	
	public String stopAllFlows() throws Exception;
	
	/**
	* Starts a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/	
	public String startFlow(String flowId) throws Exception;

	/**
	* Restarts a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String restartFlow(String flowId) throws Exception;
	
	/**
	* Stops a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String stopFlow(String flowId) throws Exception;
	
	/**
	* Resumes a flow if paused
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String resumeFlow(String flowId) throws Exception;

	/**
	* Pauses a flow if started
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String pauseFlow(String flowId) throws Exception;

	/**
	* Checks if a flow is started
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow status cannot retrieved
	* @return returns true if flow is started.
	*/
	public boolean isFlowStarted(String flowId) throws Exception;
		
	/**
	* Gets the status of a flow 
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns true (stopped, started, paused).
	*/
	public String getFlowStatus(String flowId) throws Exception;

	/*
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
	public String getFlowAlertsLog(String flowId, Integer numberOfEntries) throws Exception;	

	/**
	* Gets number of entries in (todays) failed log of flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if log cannot be retrieved
	* @return number of flow failures
	*/	
	public String getFlowAlertsCount(String flowId) throws Exception;	

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
	public String getFlowEventsLog(String flowId, Integer numberOfEntries) throws Exception;	
	
	/**
	* Gets the details stats of a flow
	*
	* @param  flowId the id of the flow
	* @param  mediaType (xml or json)
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowStats(String flowId, String mediaType) throws Exception;	
	
	/**
	* Gets a running route as XML/JSON by id
	*
	* @param  flowId the id of the flow
	* @param  mediaType (xml or json)
	* @throws Exception if configuration can't be retrieved
	* @return returns the Camel Route Configuration. XML is the default Apache Camel format.
	*/
	public String getCamelRouteConfiguration(String flowId, String mediaType) throws Exception;	

	/**
	* Gets all the running routes as XML/JSON by id
	*
	* @param  mediaType (xml or json)
	* @throws Exception if configuration can't be retrieved
	* @return returns the Camel Route Configuration. XML is the default Apache Camel format.
	*/	
	public String getAllCamelRoutesConfiguration(String mediaType) throws Exception;	
	
	/**
	* Resolve the Camel component dependency by scheme name (this is download and dynamically loaded in runtime)
	*
	* @param  scheme name of the scheme
	* @return Message on succes or failure
	*/
	public String resolveDependency(String scheme);
	
	/**
	* Resolve the Camel component dependency by scheme name (this is download and dynamically loaded in runtime)
	*
	* @param groupId  name of the (Maven) GroupID
	* @param artifactId name of the (Maven) ArtifactID
	* @param version (Maven) version number
	* @return Message on succes or failure
	*/	public String resolveDependency(String groupId, String artifactId, String version);
	
	
	
	/**
	* Get the context of connector (can be used to access extended methods by the implementation (Camel)
	* Note: Calling this you're on your own :)
	*
	* @return returns context as object
	* @throws Exception if context can't be found
	*/
	public CamelContext getContext() throws Exception;

	/**
	* Get a producertemplate for CamelConnector
	*
	* @return returns ProducerTemplate
	* @throws Exception if context can't be found
	*/
	public ProducerTemplate getProducerTemplate() throws Exception;

	/**
	* Get a consumer template for CamelConnector
	*
	* @return returns ConsumerTemplate
	* @throws Exception if context can't be found
	*/
	public ConsumerTemplate getConsumerTemplate() throws Exception;

}