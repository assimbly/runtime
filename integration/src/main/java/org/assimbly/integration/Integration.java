package org.assimbly.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.EventNotifier;
import org.assimbly.util.EncryptionUtil;

import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * <pre>
 * This interface is meant to configure and manage a integration.
 *
 * A <b>Integration</b> is a collection of flows.
 * A <b>Flow</b> connects one or more steps for example a database and a directory.
 *
 * Each flow configuration consists of a Treemap&lt;key,value&gt;. The integration configuration
 * consists of a list of flow configurations.
 *
 * For a valid flow configuration see
 * <a href="https://github.com/assimbly/runtime">https://github.com/assimbly/runtime</a>
 * </pre>
 */
public interface Integration {

	/**
	 * Gets the integration properties
	 */
	public Properties getEncryptionProperties();

	/**
	 * Sets the integration properties to pass application property variables from the application to the integration
	 *
	 * @param properties: set application properties
	 */
	public void setEncryptionProperties(Properties properties);

	/**
	 * @return encryption Utility
	 */
	public EncryptionUtil getEncryptionUtil();

	//configure integration

	/**
	 * Sets the integration configuration from a list of flow configurations (TreeMap&lt; Key,Value&gt;).
	 * The configuration cleared after a integration is reinitialized.
	 *
	 * @param configuration: list of flow configurations (Treemaps)
	 * @throws Exception if configuration can't be set
	 */
	public void setFlowConfigurations(List<TreeMap<String, String>> configuration) throws Exception;

	/**
	 * Sets the integration configuration from a string of a specific format (XML,JSON,YAML).
	 * The configuration cleared after a integration is reinitialized.
	 *
	 * @param integrationId   ID of the integration
	 * @param mediaType     (XML,JSON,YAML)
	 * @param configuration (the XML, JSON or YAML file)
	 * @throws Exception if configuration can't be set
	 */
	public void setFlowConfigurations(String integrationId, String mediaType, String configuration) throws Exception;
	
	/**
	* gets the integration configuration currently set (in use). 
	*
	* @return list of flow configurations (Treemap&lt;key,value&gt;)
	* @throws Exception if configuration can't be retrieved or isn't available
	*/
	public List<TreeMap<String,String>> getFlowConfigurations() throws Exception;

	/**
	* gets the integration configuration currently set (in use). 
	*
	* @param  integrationId ID of the integration
	* @param  mediaType (XML,JSON,YAML)
	* @return list of flow configurations (String of mediaType)
	* @throws Exception if configuration can't be retrieved or isn't available
	*/
	public String getFlowConfigurations(String integrationId, String mediaType) throws Exception;
	
	//configure flow
	/**
	* Sets the flow configuration from a Tree (keyvalues). This list
	* is cleared after a integration is reinitialized.
	*
	* @param  configuration of a flow configuration
	* @throws Exception if configuration can't be set
	*/
	public void setFlowConfiguration(TreeMap<String,String> configuration) throws Exception;	

	/**
	* Sets the flow configuration from a string for a specific format (XML,JSON,YAML). This list
	* is cleared after a integration is reinitialized.
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
	* gets the integration configuration currently set (in use). 
	*
	* @param  flowId ID of the flow (String)
	* @param  mediaType (XML,JSON,YAML)
	* @return list of flow configurations (String of mediaType)
	* @throws Exception if configuration can't be retrieved or is not available
	*/
	public String getFlowConfiguration(String flowId, String mediaType) throws Exception;

	/**
	 * gets the integration configuration currently set (in use).
	 *
	 * @param  props Properties of service
	 * @param  stepType (from,to, response or error)
	 * @return properties of service
	 * @throws Exception if connection can't be set
	 */
	public TreeMap<String, String> setConnection(TreeMap<String, String> props, String stepType) throws Exception;

	/**
	* sets the integration base directory. In this directory event are stored (like logs, alerts, events) 
	*
	* @param  baseDirectory (path) 
	* @throws Exception if base directory can't be set is not available
	*/
	public void setBaseDirectory(String baseDirectory) throws Exception;

	/**
	* gets the integration base directory. In this directory event are stored (like logs, alerts, events) 
	*
	* @throws Exception if base directory can't be retrieved
	*/
	public String getBaseDirectory() throws Exception;
	
	/**
	* sets the integration base directory. In this directory everything is stored (alert, events) 
	*
	* @param  host (dnsname or ip of server)
	* @param  port number (1 through 65535)
	* @param  timeOut in seconds
	* @return Message "Connection succesfully opened" or "Connection error"
	*/
	public String testConnection(String host, int port, int timeOut);


	//manage integration

	/**
	 * Starts a integration. The integration acts like a container for flows.
	 * After starting it can be configured
	 *
	 * @throws Exception if integration doesn't start
	 */
	public void start() throws Exception;

	/**
	 * Stops a integration
	 *
	 * @throws Exception if integration doesn't start
	 */
	public void stop() throws Exception;

	/**
	 * Checks if a integration is started
	 *
	 * @return returns true if integration is started
	 */
	public boolean isStarted();

	/**
	 * Turn on/off tracing
	 * @param tracing to turn on tracing, false to turn it off
	 * @param type of tracing which can be default (log to default logging) or backlog (log to a backlog queue)
	 */
	public void setTracing(boolean tracing, String type);

	/**
	 * Turn on/off debugging
	 * @param debugging to turn on debugging, false to turn it off
	 */
	public void setDebugging(boolean debugging);

	/**
	 * Turn on/off loading/starting flows (as file) from the deploy directory
	 * @param deployOnStart if true then flows in the deploy directory are started on startup of Assimbly
	 * @param deployOnChange if true then flows in the deploy directory are started on file change
	 */
	public void setDeployDirectory(boolean deployOnStart, boolean deployOnChange) throws Exception;

	/**
	 * Turn on/off suppressLoggingOnTimeout
	 * @param suppressLoggingOnTimeout to turn on debugging, false to turn it off
	 */
	public void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout);

	/**
	 * Turn on/off streamCaching
	 * @param streamCaching to turn on streamCaching, false to turn it off
	 */
	public void setStreamCaching(boolean streamCaching);

	/**
	 * Turn on/off debugging
	 * @param certificateStore to turn on certificateStore, false to turn it off
	 */
	public void setCertificateStore(boolean certificateStore) throws Exception;

	/**
	 * Turn on/off metrics
	 * @param metrics to turn on metrics, false to turn it off
	 */
	public void setMetrics(boolean metrics);

	/**
	 * Turn on/off historyMetrics
	 * @param historyMetrics to turn on historyMetrics, false to turn it off
	 */
	public void setHistoryMetrics(boolean historyMetrics);

	/**
	* Adds event notifier to notified about events
	* @param  eventNotifier eventNotifier object
	* @throws Exception if eventNotifier
	*/
	public void addEventNotifier(EventNotifier eventNotifier) throws Exception;
	
	//manage flow
	/**
	* Checks if a flow is a part of integration
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
	* Gets the stats of a integration
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
	 * Gets the all information of the components
	 *
	 * @param  mediaType type of dataform (xml or json)
	 * @throws Exception if components couldn't get found
	 * @return returns list of components
	 */
	public String getComponents(String mediaType) throws Exception;

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
	* Gets the last error of a integration
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
	* @param keystoreName the name of the keystore (jks file)
	* @return returns the Certificate object
	* @throws Exception if certificates cannot be downloaded
	*/
	public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) throws Exception;

	
	/**
	* Sets TLS certificates.
	*  
	* Download and import certificates to truststore (jks) used by the integration
	*
	* @param url an https url
	* @throws Exception if certificates cannot be imported
	*/
	public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) throws Exception;

	
	/**
	* Import TLS certificate.
	*  
	* Import certificate into truststore (jks) used by the integration
	*
	* @param certificateName name of the certificate
	* @param certificate Java certificate object
	* @param keystoreName the name of the keystore (jks file)
 	* @return returns a confirmation message
	* @throws Exception if certificates cannot be imported
	*/
	public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) throws Exception;
	
	
	/**
	* Import TLS certificates.
	*  
	* Import certificates to truststore (jks) used by the integration
	*
	* @param certificates map with one or more Java certificate object
	* @param keystoreName the name of the keystore (jks file)
	* @return returns a map with certificate name and Java certificate object
	* @throws Exception if certificates cannot be imported
	*/
	public Map<String,Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception;


	/**
	 * Import TLS certificate.
	 *
	 * Import certificate into truststore (jks) used by the integration
	 *
	 * @return returns a confirmation message
	 * @throws Exception if certificates cannot be imported
	 */
	public Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception;

		/**
	* Delete certificate from key/truststore
	* 
	* @param certificateName name of the certificate
	* @throws Exception if certificates cannot be deleted
	*/
	public void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName)  throws Exception;


	/**
	* removes flow from integration
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
	* Configure and Starts a flow (for testing)
	*
	* @param  flowId the id of the flow
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/	
	public String testFlow(String flowId, String mediaType, String configuration) throws Exception;

	/**
	* Configure and Starts a flow from a routes xml
	*
	* @param  flowId the id of the flow
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the Camel routes XML)
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/	
	public String routesFlow(String flowId, String mediaType, String configuration) throws Exception;


	/**
	* Installs a flow by saving the configuration as a file in the deploy directory
	*
	* @param  flowId the id of the flow
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the Camel routes XML)
	* @return returns a confirmation message ("saved")
	* @throws Exception if flow doesn't start
	*/	
	public String fileInstallFlow(String flowId, String mediaType, String configuration) throws Exception;


	/**
	* Uninstalls a flow by deleting the configuration as a file in the deploy directory
	*
	* @param  flowId the id of the flow
	* @param  mediaType (XML,JSON,YAML)
	* @return returns a confirmation message ("deleted")
	* @throws Exception if flow doesn't start
	*/	
	public String fileUninstallFlow(String flowId, String mediaType) throws Exception;

	
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
	public TreeMap<String, String> getIntegrationAlertsCount() throws Exception;
	
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
	public String getFlowStats(String flowId, String stepid, String mediaType) throws Exception;
	
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
	public String resolveDependency(String scheme) throws Exception;
	
	/**
	* Resolve the Camel component dependency by scheme name (this is download and dynamically loaded in runtime)
	*
	* @param groupId  name of the (Maven) GroupID
	* @param artifactId name of the (Maven) ArtifactID
	* @param version (Maven) version number
	* @return Message on succes or failure
	*/
	//public String resolveDependency(String groupId, String artifactId, String version);
	
	
	
	/**
	* Get the context of integration (can be used to access extended methods by the implementation (Camel)
	* Note: Calling this you're on your own :)
	*
	* @return returns context as object
	* @throws Exception if context can't be found
	*/
	public CamelContext getContext() throws Exception;

	/**
	* Get a producertemplate for CamelIntegration
	*
	* @return returns ProducerTemplate
	* @throws Exception if context can't be found
	*/
	public ProducerTemplate getProducerTemplate() throws Exception;

	/**
	* Get a consumer template for CamelIntegration
	*
	* @return returns ConsumerTemplate
	* @throws Exception if context can't be found
	*/
	public ConsumerTemplate getConsumerTemplate() throws Exception;

	/**
	 * Send a message to (default producer)
	 *
	 * @param messageBody Content of the body
	 * @param template for the producer
	 */
	public void send(Object messageBody, ProducerTemplate template);

	/**
	 * Send a message with headers to (default producer)
	 *
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 * @param template for the producer
	 */
	public void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param numberOfTimes Number of times the message is sent
	 */
	public void send(String uri,Object messageBody, Integer numberOfTimes);

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 * @param numberOfTimes Number of times the message is sent
	 */
	public void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes);

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 */
	public String sendRequest(String uri,Object messageBody);

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 */
	public String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders);

}