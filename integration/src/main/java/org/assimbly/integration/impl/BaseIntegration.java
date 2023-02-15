package org.assimbly.integration.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.EventNotifier;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.dil.validation.HttpsCertificateValidator;
import org.assimbly.dil.validation.beans.Expression;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.integration.Integration;
import org.assimbly.dil.transpiler.JSONFileConfiguration;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.dil.transpiler.YAMLFileConfiguration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.assimbly.util.error.ValidationErrorMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.cert.Certificate;
import java.util.*;


public abstract class BaseIntegration implements Integration {

	protected Logger log = LoggerFactory.getLogger(getClass());

	//properties are (list of) key/value maps
	private List<TreeMap<String, String>> properties = new ArrayList<>();
	private TreeMap<String, String> flowProperties;

	private List<TreeMap<String, String>> connections = new ArrayList<>();

	private TreeMap<String, String> configuredFlows = new TreeMap<>();

	//configuration are strings
	private String configuration;
	private String flowConfiguration;

	private Properties encryptionProperties;

	public void setEncryptionProperties(Properties encryptionProperties) {
		this.encryptionProperties = encryptionProperties;
	}

	public Properties getEncryptionProperties() {
		return encryptionProperties;
	}

	public List<TreeMap<String,String>> getFlowConfigurations() throws Exception {
		return this.properties;
	}

	public void setFlowConfiguration(String flowId, TreeMap<String,String> configuration) throws Exception {

		removeFlowConfigurationIfExist(configuration);
		
		this.properties.add(configuration);
		
	}	

	public void removeFlowConfigurationIfExist(TreeMap<String,String> configuration) throws Exception {

		String newId = configuration.get("id");

		Iterator<TreeMap<String, String>> i = this.properties.iterator();
		
		while (i.hasNext()) {
		   TreeMap<String, String> currentConfiguration = i.next(); // must be called before you can call i.remove()
		   String oldId= currentConfiguration.get("id");
		   if(newId.equals(oldId)){
				i.remove();
			    configuredFlows.remove(oldId);
		   }
		}
	}	
	
	
	public void setFlowConfiguration(String flowId, String mediaType, String configuration) throws Exception {

		try {

			if(mediaType.toLowerCase().contains("xml")) {
	        	flowProperties = convertXMLToFlowConfiguration(flowId, configuration);
			}else if(mediaType.toLowerCase().contains("json")) {
	        	flowProperties = convertJSONToFlowConfiguration(flowId, configuration);
			}else {
	        	flowProperties = convertYAMLToFlowConfiguration(flowId, configuration);
			}
		
			setFlowConfiguration(flowId, flowProperties);
			putFlowConfigurationToMap(flowId, mediaType, configuration);
		} catch (Exception e) {
			log.error("Set flow configuration failed",e);
		}
	}

	public TreeMap<String,String> getFlowConfiguration(String flowId) throws Exception {
		TreeMap<String,String> flowConfiguration = null;
		for (TreeMap<String, String> props : getFlowConfigurations()) {
			if (props.get("id").equals(flowId)) {
				flowConfiguration = props;
			}
		}
		
		return flowConfiguration;
	}	

	public String getFlowConfiguration(String flowId, String mediaType) throws Exception {

		flowConfiguration = getFlowConfigurationFromMap(flowId, mediaType);

        return flowConfiguration;
		
	}


	private void putFlowConfigurationToMap(String flowId, String mediaType, String flowConfiguration) throws Exception {

		if(mediaType.toLowerCase().contains("json")) {
			flowConfiguration = DocConverter.convertJsonToXml(flowConfiguration);
		}else if(mediaType.toLowerCase().contains("text")) {
			flowConfiguration = DocConverter.convertYamlToXml(flowConfiguration);
		}

		if (configuredFlows.containsKey(flowId)){
			configuredFlows.replace(flowId,flowConfiguration);
		}else{
			configuredFlows.put(flowId,flowConfiguration);
		}

	}


	private String getFlowConfigurationFromMap(String flowId, String mediaType) throws Exception {

		String flowConfiguration = configuredFlows.get(flowId);

		if(flowConfiguration==null){
			flowConfiguration = "<dil></dil>";
		}

		if(mediaType.toLowerCase().contains("json")) {
			flowConfiguration = DocConverter.convertXmlToJson(flowConfiguration);
		}else if(mediaType.toLowerCase().contains("text")) {
			flowConfiguration = DocConverter.convertXmlToYaml(flowConfiguration);
		}

		return flowConfiguration;

	}



	public String getLastError() {
		
		String error = "0";
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		
		if(stackTrace.length > 0) {
			error = Arrays.toString(stackTrace);
		}
		
		return error;
	}
	
	//--> convert methods (XML)
	/*
	public String convertConfigurationToXML(String integrationid, List<TreeMap<String, String>> configuration) throws Exception {
        return new XMLFileConfiguration().createConfiguration(integrationid, configuration);
	}

	 */

	public List<TreeMap<String, String>> convertXMLToConfiguration(String integrationid, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String integrationid, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getFlowConfigurations(integrationid, configurationUri);
	}
	
	public TreeMap<String, String> convertXMLToFlowConfiguration(String integrationId, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(integrationId, configuration);
	}

	/*
	public TreeMap<String, String> convertXMLToFlowConfiguration(String flowId, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(flowId, configurationUri);
	}	
	
	public String convertFlowConfigurationToXML(TreeMap<String, String> configuration) throws Exception {
		return new XMLFileConfiguration().createFlowConfiguration(configuration);
	}

	 */
	
	//--> convert methods (JSON)

	/*
	public String convertConfigurationToJSON(String integrationid, List<TreeMap<String, String>> configuration) throws Exception {
        return new JSONFileConfiguration().createConfiguration(integrationid, configuration);
	}*/
	
	public List<TreeMap<String, String>> convertJSONToConfiguration(String integrationid, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}	


	public TreeMap<String, String> convertJSONToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

		/*
	public String convertFlowConfigurationToJSON(TreeMap<String, String> configuration) throws Exception {
		return new JSONFileConfiguration().createFlowConfiguration(configuration);
	}*/
	
	//--> convert methods (YAML)	

	/*
	public String convertConfigurationToYAML(String integrationid, List<TreeMap<String, String>> configuration) throws Exception {
        return new YAMLFileConfiguration().createConfiguration(integrationid, configuration);
	}*/
	
	public List<TreeMap<String, String>> convertYAMLToConfiguration(String integrationid, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}	


	public TreeMap<String, String> convertYAMLToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

	/*
	public String convertFlowConfigurationToYAML(TreeMap<String, String> configuration) throws Exception {
		return new YAMLFileConfiguration().createFlowConfiguration(configuration);
	}*/

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}

	public String getBaseDirectory() {
		return BaseDirectory.getInstance().getBaseDirectory();
	}

	public String testConnection(String host, int port, int timeOut) {
		return IntegrationUtil.testConnection(host, port, timeOut);
	}
	
	//--> abstract methods (needs to be implemented in the subclass specific to the integration framework)


	// Integration

	public abstract void start() throws Exception;

	public abstract void stop() throws Exception;

	public abstract String info(String mediaType) throws Exception;

	public abstract boolean isStarted();

	public abstract void setTracing(boolean tracing, String type);

	public abstract void setDebugging(boolean debugging);
	
	public abstract void setDeployDirectory(boolean deployOnstart, boolean deployOnChange) throws Exception;

	public abstract void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout);

	public abstract void setStreamCaching(boolean streamCaching);

	public abstract void setCertificateStore(boolean certificateStore) throws Exception;

	public abstract void setMetrics(boolean metrics);

	public abstract void setHistoryMetrics(boolean historyMetrics);

	public abstract String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception;

	public abstract String removeCollectorConfiguration(String collectorId) throws Exception;

	public abstract void addEventNotifier(EventNotifier eventNotifier) throws Exception;

	public abstract String getStats(String mediaType) throws Exception;

	public abstract String getMessages(String mediaType) throws Exception;

	public abstract String getStatsByFlowIds(String flowIds, String mediaType) throws Exception;

	public abstract String getMetrics(String mediaType) throws Exception;

	public abstract String getHistoryMetrics(String mediaType) throws Exception;

	public abstract String getDocumentationVersion() throws Exception;

	public abstract String getDocumentation(String componentType, String mediaType) throws Exception;

	public abstract String getComponents(String mediaType) throws Exception;

	public abstract String getComponentSchema(String componentType, String mediaType) throws Exception;

	public abstract String getComponentParameters(String componentType, String mediaType) throws Exception;


	//flows

	public abstract boolean removeFlow(String id) throws Exception;

	public abstract boolean hasFlow(String id);

	public abstract String validateFlow(String uri);
	
	public abstract String startAllFlows() throws Exception;

	public abstract String restartAllFlows() throws Exception;

	public abstract String pauseAllFlows() throws Exception;

	public abstract String resumeAllFlows() throws Exception;

	public abstract String stopAllFlows() throws Exception;
	
	public abstract String startFlow(String id) throws Exception;

	public abstract String restartFlow(String id) throws Exception;
	
	public abstract String stopFlow(String id) throws Exception;

	public abstract String pauseFlow(String id) throws Exception;
	
	public abstract String resumeFlow(String id) throws Exception;

	public abstract String routesFlow(String id, String mediaType, String configuration) throws Exception;

	public abstract String installFlow(String id, String mediaType, String configuration) throws Exception;

	public abstract String uninstallFlow(String id, String mediaType) throws Exception;

	public abstract String fileInstallFlow(String id, String mediaType, String configuration) throws Exception;

	public abstract String fileUninstallFlow(String id, String mediaType) throws Exception;
	
	public abstract boolean isFlowStarted(String id) throws Exception;

	public abstract String getFlowInfo(String id, String mediaType) throws Exception;

	public abstract String getFlowStatus(String id) throws Exception;

	public abstract String getFlowUptime(String id) throws Exception;

	public abstract String getFlowLastError(String id);

	public abstract String getFlowMessages(String id, boolean includeSteps, String mediaType) throws Exception;

	public abstract String getFlowTotalMessages(String id) throws Exception;
	
	public abstract String getFlowCompletedMessages(String id) throws Exception;
	
	public abstract String getFlowFailedMessages(String id) throws Exception;

	public abstract String getFlowPendingMessages(String id) throws Exception;

	public abstract String getStepMessages(String id, String stepId, String mediaType) throws Exception;

	public abstract String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception;	

	public abstract String getFlowAlertsCount(String id) throws Exception;	

	public abstract TreeMap<String, String> getIntegrationAlertsCount() throws Exception;	
	
	public abstract String getFlowEventsLog(String id, Integer numberOfEntries) throws Exception;	
	
	public abstract String getFlowStats(String id, boolean fullStats, boolean includeSteps, String mediaType) throws Exception;

	public abstract String getFlowStepStats(String id, String stepid, boolean fullStats, String mediaType) throws Exception;

	public abstract String getListOfFlows(String filter, String mediaType) throws Exception;

	public abstract String getListOfFlowsDetails(String filter, String mediaType) throws Exception;

	public abstract String getListOfSoapActions(String url, String mediaType) throws Exception;

	public abstract String countFlows(String filter, String mediaType) throws Exception;

	public abstract String countSteps(String filter, String mediaType) throws Exception;

	//certificates

	public abstract Certificate[] getCertificates(String url) throws Exception;

	public abstract Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) throws Exception;

	public abstract void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) throws Exception;

	public abstract Map<String,Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception;

	public abstract String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) throws Exception;

	public abstract Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception;

	public abstract void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName)  throws Exception;


	//Misc

	public abstract String getCamelRouteConfiguration(String id, String mediaType) throws Exception;

	public abstract String getAllCamelRoutesConfiguration(String mediaType) throws Exception;

	public abstract TreeMap<String, String> setConnection(TreeMap<String, String> props, String stepType) throws Exception;

	public abstract String resolveDependency(String schema)  throws Exception;

	//public abstract String resolveDependency(String groupId, String artifactId, String version);

	public abstract CamelContext getContext() throws Exception;
	
	public abstract ProducerTemplate getProducerTemplate() throws Exception;
	
	public abstract ConsumerTemplate getConsumerTemplate() throws Exception;
	
	public abstract void send(Object messageBody, ProducerTemplate template);

	public abstract void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);

	public abstract void send(String uri,Object messageBody, Integer numberOfTimes);

	public abstract void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes);

	public abstract String sendRequest(String uri,Object messageBody);

	public abstract String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders);

	// validate

	public abstract ValidationErrorMessage validateCron(String cronExpression);

	public abstract HttpsCertificateValidator.ValidationResult validateCertificate(String httpsUrl);

	public abstract ValidationErrorMessage validateUrl(String url);

	public abstract List<ValidationErrorMessage> validateExpressions(List<Expression> expressions);

	public abstract ValidationErrorMessage validateFtp(FtpSettings ftpSettings);

	public abstract AbstractMap.SimpleEntry validateRegex(Regex regex);

	public abstract EvaluationResponse validateScript(EvaluationRequest scriptRequest);

}
