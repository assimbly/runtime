package org.assimbly.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.EventNotifier;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.util.EncryptionUtil;
import org.assimbly.util.error.ValidationErrorMessage;

import java.security.cert.Certificate;
import java.util.*;

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

	//configure flows
	/**
	* Sets the flow configuration from a Tree (keyvalues). This list
	* is cleared after a integration is reinitialized.
	*
	* @param  configuration of a flow configuration
	* @throws Exception if configuration can't be set
	*/
	public void setFlowConfiguration(String flowId, TreeMap<String,String> configuration) throws Exception;

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
	 * Add configuration for multiple collectors from a JSON Configuration File.
	 *
	 * @param  mediaType (JSON)
	 * @param  configuration (the JSON file with the configuration. See the Assimbly wiki for examples)
	 * @throws Exception if configuration can't be set
	 */
	public String addCollectorsConfiguration(String mediaType, String configuration) throws Exception;


	/**
	 * Add collector configuration from a JSON Configuration File.
	 *
	 * @param  collectorId Id of the collector (String)
	 * @param  mediaType (JSON)
	 * @param  configuration (the JSON file with the configuration. See the Assimbly wiki for examples)
	 * @throws Exception if configuration can't be set
	 */
	public String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception;

	/**
	 * Sets the collector configuration from a string for a specific format (XML,JSON,YAML).
	 *
	 * @param  collectorId Id of the collector (String)
	 * @throws Exception if configuration can't be set
	 */
	public String removeCollectorConfiguration(String collectorId) throws Exception;

	/**
	 * gets the integration configuration currently set (in use).
	 *
	 * @param  props Properties of connection
	 * @param  stepType (from,to, response or error)
	 * @return properties of connection
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
	* @return Message "Connection successfully opened" or "Connection error"
	*/
	public String testConnection(String host, int port, int timeOut);


	//manage integration

	/**
	 * Starts an integration. The integration acts like a container for flows.
	 * After starting it can be configured
	 *
	 * @throws Exception if integration doesn't start
	 */
	public void start() throws Exception;

	/**
	 * Stops an integration
	 *
	 * @throws Exception if integration doesn't start
	 */
	public void stop() throws Exception;

	/**
	 * Info on an integration (state, uptime, numberofroutes etc)
	 *
	 * @throws Exception if integration doesn't start
	 */
	public String info(String mediaType) throws Exception;

	/**
	 * Checks if an integration is started
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
	* Gets the stats of an integration
	*
	* @param  mediaType (xml or json)
	* @throws Exception if stats can't be retrieved
	* @return returns stats of integration (system)
	*
	*/
	public String getStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of all steps
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if stats can't be retrieved
	 * @return returns stats of integration (system)
	 */
	public String getStepsStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of all flows
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if stats can't be retrieved
	 * @return returns stats of integration (system)
	 */
	public String getFlowsStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getMessages(String mediaType) throws Exception;

	/**
	 * Gets the stats of an integration
	 *
	 * @param  flowIds comma separated list of flow ids
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getStatsByFlowIds(String flowIds, String filter, String mediaType) throws Exception;

	/**
	 * Gets the metrics of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns metrics
	 */
	public String getMetrics(String mediaType) throws Exception;

	/**
	 * Gets the historical metrics of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns historical metrics
	 */
	public String getHistoryMetrics(String mediaType) throws Exception;

	/**
	 * Gets the ids of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
	public String getListOfFlows(String filter, String mediaType) throws Exception;

	/**
	 * Gets the details of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id, name, status etc
	 */
	public String getListOfFlowsDetails(String filter, String mediaType) throws Exception;

	/**
	 * Gets the soap actions for a SOAP service (WSDL)
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of SOAP actions
	 */
	public String getListOfSoapActions(String url, String mediaType) throws Exception;


	/**
	 * Gets the template of steps (templates)
	 *
	 * @param  mediaType (xml, json or yaml)
	 * @param  stepName name of the step template
	 * @throws Exception if step template isn't found
	 * @return returns an array of flows with id
	 */
	public abstract String getStepTemplate(String mediaType, String stepName) throws Exception;

	/**
	 * Gets list of step templates
	 *
	 * @throws Exception if list failed to retreive
	 * @return returns an array of flows with id
	 */
	public abstract String getListOfStepTemplates() throws Exception;

	/**
	 * Count the number of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
	public String countFlows(String filter, String mediaType) throws Exception;

	/**
	 * Counts the number of (loaded) steps
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
	public String countSteps(String filter, String mediaType) throws Exception;

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
	public String getComponents(Boolean includeCustomComponents, String mediaType) throws Exception;

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
	public String startFlow(String flowId, long timeout) throws Exception;

	/**
	* Restarts a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String restartFlow(String flowId, long timeout) throws Exception;
	
	/**
	* Stops a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
	public String stopFlow(String flowId, long timeout) throws Exception;
	
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
	 * Configure and Starts a flow (for testing)
	 *
	 * @param  routeId the id of the flow
	 * @param  route (the XML of the route)
	 * @return returns a confirmation message
	 * @throws Exception if flow doesn't start
	 */
	public String installRoute(String routeId, String route) throws Exception;

	/**
	* Configure and Starts a flow (for testing)
	*
	* @param  flowId the id of the flow
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/	
	public String installFlow(String flowId, long timeout, String mediaType, String configuration) throws Exception;

	/**
	 * Configure and Starts a flow (for testing)
	 *
	 * @param  flowId the id of the flow
	 * @return returns a confirmation message
	 * @throws Exception if flow doesn't start
	 */
	public String uninstallFlow(String flowId, long timeout) throws Exception;

	/**
	* Installs a flow by saving the configuration as a file in the deploy directory
	*
	* @param  flowId the id of the flow
	* @param  configuration (the Camel routes XML)
	* @return returns a confirmation message ("saved")
	* @throws Exception if flow doesn't start
	*/	
	public String fileInstallFlow(String flowId, String configuration) throws Exception;


	/**
	* Uninstalls a flow by deleting the configuration as a file in the deploy directory
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message ("deleted")
	* @throws Exception if flow doesn't start
	*/	
	public String fileUninstallFlow(String flowId) throws Exception;

	
	/**
	* Checks if a flow is started
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow status cannot retrieved
	* @return returns true if flow is started.
	*/
	public boolean isFlowStarted(String flowId) throws Exception;

	/**
	 * Gets info of a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  mediaType (XML,JSON,TEXT)
	 * @throws Exception if flow doesn't start
	 * @return returns info (id, name, version, environment, isError, status, uptime, lastError).
	 */
	public String getFlowInfo(String flowId, String mediaType) throws Exception;

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
	 * Gets the processed messages of a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  includeSteps Whether or not to include steps
	 * @param mediaType (XML,JSON or TEXT)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages (total, completed, failed, pending)
	 */
	public String getFlowMessages(String flowId, boolean includeSteps, String mediaType) throws Exception;

	/**
	* Gets the total number of messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
	public String getFlowTotalMessages(String flowId) throws Exception;	

	/**
	* Gets the total number of completed messages a flow has processed
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
	 * Gets the total number of pending messages a flow has processed
	 *
	 * @param  flowId the id of the flow
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getFlowPendingMessages(String flowId) throws Exception;

	/**
	 * Gets the processed messaged of a step in a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  stepId the id of the step
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages (total, completed, failed, pending)
	 */
	public String getStepMessages(String flowId, String stepId, String mediaType) throws Exception;

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
	 * @param flowId    the id of the flow
	 * @param mediaType (xml or json)
	 * @param fullStats (include additional fields)
	 * @param includeMetaData (includes information about the flow)
	 * @param includeSteps (include stats for every step)
	 * @param filter
	 * @return returns number of messages
	 * @throws Exception if flow doesn't start
	 */
	public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, String filter, String mediaType) throws Exception;

	/**
	 * Gets the details stats of a flow step
	 *
	 * @param  flowId the id of the flow
	 * @param  stepId the id of the step
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getFlowStepStats(String flowId, String stepId, boolean fullStats, String mediaType) throws Exception;

	/**
	 * Gets the health of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getHealth(String type, String mediaType) throws Exception;

	/**
	 * Gets the health of an integration
	 *
	 * @param  flowIds comma separated list of flow ids
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getHealthByFlowIds(String flowIds, String type, String mediaType) throws Exception;

	/**
	 * Gets the details health of a flow
	 *
	 * @param flowId    the id of the flow
	 * @param mediaType (xml or json)
	 * @param type The type of healthcheck (route, consumer, producer)
	 * @param includeSteps (includes health information of every step)
	 * @param includeError (includes error information when available)
	 * @param includeDetails (include healthcheck details)
	 * @return returns number of messages
	 * @throws Exception if flow doesn't start
	 */
	public String getFlowHealth(String flowId, String type, boolean includeError, boolean includeSteps, boolean includeDetails, String mediaType) throws Exception;


	/**
	 * Gets the details health of a flow step
	 *
	 * @param  flowId the id of the flow
	 * @param  stepId the id of the step
	 * @param  mediaType (xml or json)
	 * @param type The type of healthcheck (route, consumer, producer)
	 * @param includeError (includes error information when available)
	 * @param includeDetails (include healthcheck details)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
	public String getFlowStepHealth(String flowId, String stepId,  String type, boolean includeError, boolean includeDetails, String mediaType) throws Exception;

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
	* Gets all the running routes as XML/JSON/YAML by id
	*
	* @param  mediaType (xml, json, yaml)
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

	/**
	 * Validates a cron expression
	 *
	 * @param  cronExpression the cron expression
	 * @return result of validation
	 */
	public ValidationErrorMessage validateCron(String cronExpression);

	/**
	 * Validates a certificate
	 *
	 * @param  httpsUrl
	 * @return result of validation	 */
	public HttpsCertificateValidator.ValidationResult validateCertificate(String httpsUrl);

	/**
	 * Validates a url expression
	 *
	 * @param  url
	 * @return result of validation
	 */
	public ValidationErrorMessage validateUrl(String url);

	/**
	 * Validates an expression
	 *
	 * @param  expressions the expression (for example simple, xpath, jsonpath or Groovy)
	 * @return result of validation
	 */
	public List<ValidationErrorMessage> validateExpressions(List<Expression> expressions, boolean isPredicate);

	/**
	 * Validates a ftp expression
	 *
	 * @param  ftpSettings
	 * @return result of validation
	 */
	public ValidationErrorMessage validateFtp(FtpSettings ftpSettings);

	/**
	 * Validates a regex expression
	 *
	 * @param  regex the regex expression
	 * @return result of validation
	 */
	public AbstractMap.SimpleEntry validateRegex(Regex regex);

	/**
	 * Validates a script
	 *
	 * @param  scriptRequest the script (for example Groovy)
	 * @return result of validation
	 */
	public EvaluationResponse validateScript(EvaluationRequest scriptRequest);

	/**
	 * Validates a xslt
	 *
	 * @param  url location of the XSLT file
	 * @param  xsltBody the body of the XSLT file
	 * @return result of validation
	 */
	public List<ValidationErrorMessage> validateXslt(String url, String xsltBody);

}