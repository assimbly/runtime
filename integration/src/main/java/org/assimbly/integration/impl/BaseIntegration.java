package org.assimbly.integration.impl;

import org.assimbly.dil.transpiler.JSONFileConfiguration;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.dil.transpiler.YAMLFileConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.Integration;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;


public abstract class BaseIntegration implements Integration {

	protected Logger log = LoggerFactory.getLogger(getClass());

	//properties are (list of) key/value maps
	private final List<TreeMap<String, String>> properties = new ArrayList<>();

	private final TreeMap<String, String> configuredFlows = new TreeMap<>();

	private String flowConfiguration;

	private Properties encryptionProperties;

	public void setEncryptionProperties(Properties encryptionProperties) {
		this.encryptionProperties = encryptionProperties;
	}

	public Properties getEncryptionProperties() {
		return encryptionProperties;
	}

	public List<TreeMap<String,String>> getFlowConfigurations() {
		return this.properties;
	}

	public void setFlowConfiguration(String flowId, TreeMap<String,String> configuration) throws Exception {

		removeFlowConfigurationIfExist(configuration);
		
		this.properties.add(configuration);
		
	}	

	public void removeFlowConfigurationIfExist(TreeMap<String,String> configuration) {

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

			TreeMap<String, String> flowProperties;
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
		TreeMap<String,String> flowConf = null;
		for (TreeMap<String, String> props : getFlowConfigurations()) {
			if (props.get("id").equals(flowId)) {
				flowConf = props;
			}
		}
		
		return flowConf;
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

		flowConfiguration = configuredFlows.get(flowId);

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
	
	public List<TreeMap<String, String>> convertXMLToConfiguration(String integrationid, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String integrationid, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getFlowConfigurations(integrationid, configurationUri);
	}
	
	public TreeMap<String, String> convertXMLToFlowConfiguration(String integrationId, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(integrationId, configuration);
	}

	//--> convert methods (JSON)

	public List<TreeMap<String, String>> convertJSONToConfiguration(String integrationid, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}
	public TreeMap<String, String> convertJSONToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

	public List<TreeMap<String, String>> convertYAMLToConfiguration(String integrationid, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfigurations(integrationid, configuration);
	}	


	public TreeMap<String, String> convertYAMLToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

	public void setBaseDirectory(String baseDirectory) {
		BaseDirectory.getInstance().setBaseDirectory(baseDirectory);
	}

	public String getBaseDirectory() {
		return BaseDirectory.getInstance().getBaseDirectory();
	}

	public String testConnection(String host, int port, int timeOut) {
		return IntegrationUtil.testConnection(host, port, timeOut);
	}

}
