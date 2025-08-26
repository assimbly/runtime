package org.assimbly.integration;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.ValidationExpression;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.util.EncryptionUtil;
import org.assimbly.util.error.ValidationErrorMessage;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
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
    Properties getEncryptionProperties();

	/**
	 * Sets the integration properties to pass application property variables from the application to the integration
	 *
	 * @param properties: set application properties
	 */
    void setEncryptionProperties(Properties properties);

	/**
	 * @return encryption Utility
	 */
    EncryptionUtil getEncryptionUtil();

	//configure flows
	/**
	* Sets the flow configuration from a Tree (keyvalues). This list
	* is cleared after a integration is reinitialized.
	*
	* @param  configuration of a flow configuration
	* @throws Exception if configuration can't be set
	*/
    void setFlowConfiguration(String flowId, TreeMap<String, String> configuration) throws Exception;

	/**
	* Sets the flow configuration from a string for a specific format (XML,JSON,YAML). This list
	* is cleared after a integration is reinitialized.
	*
	* @param  flowId Id of the flow (String)
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
	* @throws Exception if configuration can't be set
	*/
    void setFlowConfiguration(String flowId, String mediaType, String configuration) throws Exception;
	
	/**
	* gets the flow configuration for a specific if currently set
	*
	* @param  flowId the id of a flow
	* @return flow configuration
	* @throws Exception if configuration can't be retrieved or is not available
	*/
    TreeMap<String,String> getFlowConfiguration(String flowId) throws Exception;
	
	/**
	* gets the integration configuration currently set (in use). 
	*
	* @param  flowId ID of the flow (String)
	* @param  mediaType (XML,JSON,YAML)
	* @return list of flow configurations (String of mediaType)
	* @throws Exception if configuration can't be retrieved or is not available
	*/
    String getFlowConfiguration(String flowId, String mediaType) throws Exception;

	/**
	 * Add configuration for multiple collectors from a JSON Configuration File.
	 *
	 * @param  mediaType (JSON)
	 * @param  configuration (the JSON file with the configuration. See the Assimbly wiki for examples)
	 * @throws Exception if configuration can't be set
	 */
    String addCollectorsConfiguration(String mediaType, String configuration) throws Exception;


	/**
	 * Add collector configuration from a JSON Configuration File.
	 *
	 * @param  collectorId Id of the collector (String)
	 * @param  mediaType (JSON)
	 * @param  configuration (the JSON file with the configuration. See the Assimbly wiki for examples)
	 * @throws Exception if configuration can't be set
	 */
    String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception;

	/**
	 * Sets the collector configuration from a string for a specific format (XML,JSON,YAML).
	 *
	 * @param  collectorId Id of the collector (String)
	 * @throws Exception if configuration can't be set
	 */
    String removeCollectorConfiguration(String collectorId) throws Exception;

	/**
	 * gets the integration configuration currently set (in use).
	 *
	 * @param  props Properties of connection
	 * @param  stepType (from,to, response or error)
	 * @throws Exception if connection can't be set
	 */
    void setConnection(TreeMap<String, String> props, String stepType) throws Exception;

	/**
	* sets the integration base directory. In this directory event are stored (like logs, alerts, events) 
	*
	* @param  baseDirectory (path) 
	* @throws Exception if base directory can't be set is not available
	*/
    void setBaseDirectory(String baseDirectory) throws Exception;

	/**
	* gets the integration base directory. In this directory event are stored (like logs, alerts, events) 
	*
	* @throws Exception if base directory can't be retrieved
	*/
    String getBaseDirectory() throws Exception;
	
	/**
	* sets the integration base directory. In this directory everything is stored (alert, events) 
	*
	* @param  host (dnsname or ip of server)
	* @param  port number (1 through 65535)
	* @param  timeOut in seconds
	* @return Message "Connection successfully opened" or "Connection error"
	*/
    String testConnection(String host, int port, int timeOut);


	//manage integration

	/**
	 * Starts an integration. The integration acts like a container for flows.
	 * After starting it can be configured
	 *
	 * @throws Exception if integration doesn't start
	 */
    void start() throws Exception;

	/**
	 * Stops an integration
	 *
	 * @throws Exception if integration doesn't start
	 */
    void stop() throws Exception;

	/**
	 * Info on an integration (state, uptime, numberofroutes etc)
	 *
	 * @throws Exception if integration doesn't start
	 */
    String info(String mediaType) throws Exception;

	/**
	 * Checks if an integration is started
	 *
	 * @return returns true if integration is started
	 */
    boolean isStarted();

	//manage flow
	/**
	* Checks if a flow is a part of integration
	*
	* @param  flowId the id of a flow
	* @return returns true if started.
	*/
    boolean hasFlow(String flowId);

	/**
	* Validates the uri + options
	*
	* @param  flowId the id of a flow
	* @return returns true if valid.
	*/
    String validateFlow(String flowId);
	
	/**
	* Gets the stats of an integration
	*
	* @param  mediaType (xml or json)
	* @throws Exception if stats can't be retrieved
	* @return returns stats of integration (system)
	*
	*/
    String getStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of all steps
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if stats can't be retrieved
	 * @return returns stats of integration (system)
	 */
    String getStepsStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of all flows
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if stats can't be retrieved
	 * @return returns stats of integration (system)
	 */
    String getFlowsStats(String mediaType) throws Exception;

	/**
	 * Gets the stats of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getMessages(String mediaType) throws Exception;

	/**
	 * Gets the stats of an integration
	 *
	 * @param  flowIds comma separated list of flow ids
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getStatsByFlowIds(String flowIds, String filter, String mediaType) throws Exception;

	/**
	 * Gets the metrics of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns metrics
	 */
    String getMetrics(String mediaType) throws Exception;

	/**
	 * Gets the historical metrics of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns historical metrics
	 */
    String getHistoryMetrics(String mediaType) throws Exception;

	/**
	 * Gets the ids of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
    String getListOfFlows(String filter, String mediaType) throws Exception;

	/**
	 * Gets the details of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id, name, status etc
	 */
    String getListOfFlowsDetails(String filter, String mediaType) throws Exception;

	/**
	 * Gets the soap actions for a SOAP service (WSDL)
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of SOAP actions
	 */
    String getListOfSoapActions(String url, String mediaType) throws Exception;


	/**
	 * Gets the template of steps (templates)
	 *
	 * @param  mediaType (xml, json or yaml)
	 * @param  stepName name of the step template
	 * @throws Exception if step template isn't found
	 * @return returns an array of flows with id
	 */
    String getStepTemplate(String mediaType, String stepName) throws Exception;

	/**
	 * Gets list of step templates
	 *
	 * @throws Exception if list failed to retreive
	 * @return returns an array of flows with id
	 */
    String getListOfStepTemplates() throws Exception;

	/**
	 * Count the number of (loaded) flows
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
    String countFlows(String filter, String mediaType) throws Exception;

	/**
	 * Counts the number of (loaded) steps
	 *
	 * @param  filter by status (started, stopped, suspended)
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns an array of flows with id
	 */
    String countSteps(String filter, String mediaType) throws Exception;

	/**
	* Gets the version of documentation/integration framework 
	*
	* @throws Exception if version couldn't retrieved
	* @return returns documentation version
	*/
    String getDocumentationVersion() throws Exception;

	/**
	* Gets the documentation of a component
	*
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
	* @throws Exception if documenation couldn't get found
	* @return returns documentation
	*/
    String getDocumentation(String componentType, String mediaType) throws Exception;

	/**
	 * Gets the all information of the components
	 *
	 * @param  mediaType type of dataform (xml or json)
	 * @throws Exception if components couldn't get found
	 * @return returns list of components
	 */
    String getComponents(boolean includeCustomComponents, String mediaType) throws Exception;

	/**
	* Gets the documentation/schema of a component
	*
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
	* @throws Exception if documentation couldn't get found
	* @return returns documenation
	*/
    String getComponentSchema(String componentType, String mediaType) throws Exception;

	/**
	* Gets the parameters of a component
	*
	* @param  componentType type of component (for example 'file')
 	* @param  mediaType type of dataform (xml or json)
	* @throws Exception if documentation couldn't get found
	* @return returns list of options
	*/
    String getComponentParameters(String componentType, String mediaType) throws Exception;
	
	/**
	* Gets the last error of a integration
	*
	* @throws Exception if error couldn't be retrieved
	* @return the last error or 0 if no error
	*/
    String getLastError() throws Exception;

	/**
	* Gets TLS certificates for a url.
	* Download the chain of certificates for the specified url
	*
	* @param url an https url
 	* @return returns a map with certificates for this url
	* @throws Exception if certificates cannot be downloaded
	*/
    Certificate[] getCertificates(String url) throws Exception;

	/**
	* Gets TLS certificates for a url.
	* Download the chain of certificates for the specified url
	*
	* @param certificateName name of the certificate
	* @param keystoreName the name of the keystore (jks file)
	* @return returns the Certificate object
	* @throws Exception if certificates cannot be downloaded
	*/
    Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) throws Exception;

	
	/**
	* Sets TLS certificates.
	* Download and import certificates to truststore (jks) used by the integration
	*
	* @param url an https url
	* @throws Exception if certificates cannot be imported
	*/
    void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) throws Exception;

	
	/**
	* Import TLS certificate.
	* Import certificate into truststore (jks) used by the integration
	*
	* @param certificateName name of the certificate
	* @param certificate Java certificate object
	* @param keystoreName the name of the keystore (jks file)
 	* @return returns a confirmation message
	* @throws Exception if certificates cannot be imported
	*/
    String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) throws Exception;
	
	
	/**
	* Import TLS certificates.
	* Import certificates to truststore (jks) used by the integration
	*
	* @param certificates map with one or more Java certificate object
	* @param keystoreName the name of the keystore (jks file)
	* @return returns a map with certificate name and Java certificate object
	* @throws Exception if certificates cannot be imported
	*/
    Map<String,Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception;


	/**
	 * Import TLS certificate.
	 * Import certificate into truststore (jks) used by the integration
	 *
	 * @return returns a confirmation message
	 * @throws Exception if certificates cannot be imported
	 */
    Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception;

		/**
	* Delete certificate from key/truststore
	* 
	* @param certificateName name of the certificate
	* @throws Exception if certificates cannot be deleted
	*/
        void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName)  throws Exception;


	/**
	* removes flow from integration
	*
	* @param  flowId the id of a flow
	* @return returns true if removed.
	 * @throws Exception if flow cannot be removed
	*/
    boolean removeFlow(String flowId) throws Exception;
	
	/**
	 * Starts all configured flows
	 *
	 * @throws Exception if one of the flows doesn't start
	 */
    void startAllFlows() throws Exception;

	/**
	* Restarts all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't stop
	*/
    String restartAllFlows() throws Exception;

	/**
	* Starts all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't start
	*/
    String pauseAllFlows() throws Exception;

	/**
	* Resume all configured flows
	*
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't resume
	*/
    String resumeAllFlows() throws Exception;

	/**
	* Stops all configured flows
	* 
	* @return returns a confirmation message
	* @throws Exception if one of the flows doesn't stop
	*/
    String stopAllFlows() throws Exception;
	
	/**
	* Starts a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String startFlow(String flowId, long timeout) throws Exception;

	/**
	* Restarts a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String restartFlow(String flowId, long timeout) throws Exception;
	
	/**
	* Stops a flow
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String stopFlow(String flowId, long timeout) throws Exception;
	
	/**
	* Resumes a flow if paused
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String resumeFlow(String flowId) throws Exception;

	/**
	* Pauses a flow if started
	*
	* @param  flowId the id of the flow
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String pauseFlow(String flowId) throws Exception;

	/**
	 * Configure and Starts a flow (for testing)
	 *
	 * @param  routeId the id of the flow
	 * @param  route (the XML of the route)
	 * @return returns a confirmation message
	 * @throws Exception if flow doesn't start
	 */
    String installRoute(String routeId, String route) throws Exception;

	/**
	* Configure and Starts a flow (for testing)
	*
	* @param  flowId the id of the flow
	* @param  timeout the timeout in milliseconds
	* @param  mediaType (XML,JSON,YAML)
	* @param  configuration (the XML, JSON or YAML file)
	* @return returns a confirmation message
	* @throws Exception if flow doesn't start
	*/
    String installFlow(String flowId, long timeout, String mediaType, String configuration) throws Exception;

	/**
	 * Configure and Starts a flow (for testing)
	 *
	 * @param  flowId the id of the flow
	 * @return returns a confirmation message
	 * @throws Exception if flow doesn't start
	 */
    String uninstallFlow(String flowId, long timeout) throws Exception;

	/**
	* Checks if a flow is started
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow status cannot retrieved
	* @return returns true if flow is started.
	*/
    boolean isFlowStarted(String flowId) throws Exception;

	/**
	 * Gets info of a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  mediaType (XML,JSON,TEXT)
	 * @throws Exception if flow doesn't start
	 * @return returns info (id, name, version, environment, isError, status, uptime, lastError).
	 */
    String getFlowInfo(String flowId, String mediaType) throws Exception;

	/**
	* Gets the status of a flow 
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns true (stopped, started, paused).
	*/
    String getFlowStatus(String flowId) throws Exception;

	/*
	* Gets the status of a flow 
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns data/time in human readable format
	*/
    String getFlowUptime(String flowId) throws Exception;

	/**
	* Gets the number of messages a flow has prcessed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
    String getFlowLastError(String flowId) throws Exception;

	/**
	 * Gets the processed messages of a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  includeSteps Whether or not to include steps
	 * @param mediaType (XML,JSON or TEXT)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages (total, completed, failed, pending)
	 */
    String getFlowMessages(String flowId, boolean includeSteps, String mediaType) throws Exception;

	/**
	* Gets the total number of messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
    String getFlowTotalMessages(String flowId) throws Exception;

	/**
	* Gets the total number of completed messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
    String getFlowCompletedMessages(String flowId) throws Exception;

	/**
	* Gets the total number of failed messages a flow has processed
	*
	* @param  flowId the id of the flow
	* @throws Exception if flow doesn't start
	* @return returns number of messages
	*/
    String getFlowFailedMessages(String flowId) throws Exception;

	/**
	 * Gets the total number of pending messages a flow has processed
	 *
	 * @param  flowId the id of the flow
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getFlowPendingMessages(String flowId) throws Exception;

	/**
	 * Gets the processed messaged of a step in a flow
	 *
	 * @param  flowId the id of the flow
	 * @param  stepId the id of the step
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages (total, completed, failed, pending)
	 */
    String getStepMessages(String flowId, String stepId, String mediaType) throws Exception;

	/**
	* Gets the failure log for the specified flow
	*
	* @param  flowId the id of the flow
	* @param  numberOfEntries (maximum number of entries to return)
	* @throws Exception if log cannot be retrieved
	* @return failure log events (comma separated)
	*/
    String getFlowAlertsLog(String flowId, Integer numberOfEntries) throws Exception;

	/**
	* Gets number of entries in (todays) failed log of flow
	*
	* @param  flowId the id of the flow
	* @throws Exception if log cannot be retrieved
	* @return number of flow failures
	*/
    String getFlowAlertsCount(String flowId) throws Exception;

	/**
	* Gets number of entries in (todays) failed log of all configured/running flows
	*
	* @throws Exception if log cannot be retrieved
	* @return failure log events (comma separated)
	*/
    TreeMap<String, String> getIntegrationAlertsCount() throws Exception;
	
	/**
	* Gets the event log for the specified flow (start events,stop events and message failures)
	*
	* @param  flowId the id of the flow
	* @param  numberOfEntries (maximum number of entries to return)
	* @throws Exception if log cannot be retrieved
	* @return flow log events (comma separated)
	*/
    String getFlowEventsLog(String flowId, Integer numberOfEntries) throws Exception;
	
	/**
	 * Gets the details stats of a flow
	 *
	 * @param flowId    the id of the flow
	 * @param fullStats (include additional fields)
	 * @param includeMetaData (includes information about the flow)
	 * @param includeSteps (include stats for every step)
	 * @return returns number of messages
	 * @throws Exception if flow doesn't start
	 */
    String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, String filter) throws Exception;

	/**
	 * Gets the details stats of a flow step
	 *
	 * @param  flowId the id of the flow
	 * @param  stepId the id of the step
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getFlowStepStats(String flowId, String stepId, boolean fullStats) throws Exception;

	/**
	 * Gets the health of an integration
	 *
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getHealth(String type, String mediaType) throws Exception;

	/**
	 * Gets the health of an integration
	 *
	 * @param  flowIds comma separated list of flow ids
	 * @param  mediaType (xml or json)
	 * @throws Exception if flow doesn't start
	 * @return returns number of messages
	 */
    String getHealthByFlowIds(String flowIds, String type, String mediaType) throws Exception;

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
    String getFlowHealth(String flowId, String type, boolean includeError, boolean includeSteps, boolean includeDetails, String mediaType) throws Exception;


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
    String getFlowStepHealth(String flowId, String stepId, String type, boolean includeError, boolean includeDetails, String mediaType) throws Exception;

	/**
	 * Gets the details stats of a flow
	 *
	 * @param  mediaType (xml or json)
	 * @param filter (filters list by thread name)
	 * @param topEntries (returns top entries by CPU Time)
	 * @return returns a list of threads
	 * @throws Exception if threads cannot be retrieved
	 */
    String getThreads(String mediaType, String filter, int topEntries) throws Exception;

	/**
	* Gets a running route as XML/JSON by id
	*
	* @param  flowId the id of the flow
	* @param  mediaType (xml or json)
	* @throws Exception if configuration can't be retrieved
	* @return returns the Camel Route Configuration. XML is the default Apache Camel format.
	*/
    String getCamelRouteConfiguration(String flowId, String mediaType) throws Exception;

	/**
	* Resolve the Camel component dependency by scheme name (this is download and dynamically loaded in runtime)
	*
	* @param  scheme name of the scheme
	* @return Message on succes or failure
	*/
    String resolveDependency(String scheme) throws Exception;

	/**
	* Get the context of integration (can be used to access extended methods by the implementation (Camel)
	* Note: Calling this you're on your own :)
	*
	* @return returns context as object
	* @throws Exception if context can't be found
	*/
    CamelContext getContext() throws Exception;

	/**
	* Get a producertemplate for CamelIntegration
	*
	* @return returns ProducerTemplate
	* @throws Exception if context can't be found
	*/
    ProducerTemplate getProducerTemplate() throws Exception;

	/**
	* Get a consumer template for CamelIntegration
	*
	* @return returns ConsumerTemplate
	* @throws Exception if context can't be found
	*/
    ConsumerTemplate getConsumerTemplate() throws Exception;

	/**
	 * Send a message to (default producer)
	 *
	 * @param messageBody Content of the body
	 * @param template for the producer
	 */
    void send(Object messageBody, ProducerTemplate template);

	/**
	 * Send a message with headers to (default producer)
	 *
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 * @param template for the producer
	 */
    void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param numberOfTimes Number of times the message is sent
	 */
    void send(String uri, Object messageBody, Integer numberOfTimes) throws IOException;

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 * @param numberOfTimes Number of times the message is sent
	 */
    void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes) throws IOException;

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 */
    String sendRequest(String uri, Object messageBody) throws IOException;

	/**
	 * Send a message with headers to an uri
	 *
	 * @param uri Step uri
	 * @param messageBody Content of the body
	 * @param messageHeaders Treemap<String, Object> with one or more headers
	 */
    String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders);

	/**
	 * Validates a cron expression
	 *
	 * @param  cronExpression the cron expression
	 * @return result of validation
	 */
    ValidationErrorMessage validateCron(String cronExpression);

	/**
	 * Validates a certificate
	 *
	 * @return result of validation	 */
    HttpsCertificateValidator.ValidationResult validateCertificate(String httpsUrl) throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, CertificateException, IOException;

	/**
	 * Validates a url expression
	 *
	 * @return result of validation
	 */
    ValidationErrorMessage validateUrl(String url);

	/**
	 * Validates an expression
	 *
	 * @param  expressions the expression (for example simple, xpath, jsonpath or Groovy)
	 * @return result of validation
	 */
    List<ValidationExpression> validateExpressions(List<ValidationExpression> expressions, boolean isPredicate);

	/**
	 * Validates a ftp expression
	 * @return result of validation
	 */
    ValidationErrorMessage validateFtp(FtpSettings ftpSettings) throws IOException;

	/**
	 * Validates a regex expression
	 *
	 * @param  regex the regex expression
	 * @return result of validation
	 */
    AbstractMap.SimpleEntry<Integer, String> validateRegex(Regex regex);

	/**
	 * Validates a script
	 *
	 * @param  scriptRequest the script (for example Groovy)
	 * @return result of validation
	 */
    EvaluationResponse validateScript(EvaluationRequest scriptRequest);

	/**
	 * Validates a xslt
	 *
	 * @param  url location of the XSLT file
	 * @param  xsltBody the body of the XSLT file
	 * @return result of validation
	 */
    List<ValidationErrorMessage> validateXslt(String url, String xsltBody);

}