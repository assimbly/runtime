package org.assimbly.dil.transpiler;

import java.util.List;
import java.util.TreeMap;

import org.assimbly.docconverter.DocConverter;

public class JSONFileConfiguration {

	private List<TreeMap<String, String>> properties;
	private TreeMap<String, String> flowproperties;

	private String jsonConfiguration;
	private String xmlConfiguration;

	public String createConfiguration(String integrationId, List<TreeMap<String, String>> properties) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(integrationId, properties);
		jsonConfiguration = DocConverter.convertXmlToJson(xmlConfiguration);
		
		return jsonConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> flowProperties) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(flowProperties);
		jsonConfiguration = DocConverter.convertXmlToJson(xmlConfiguration);
		
		return jsonConfiguration;
	}

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, String jsonConfiguration) throws Exception {
		
		xmlConfiguration = DocConverter.convertJsonToXml(jsonConfiguration);
		properties =  new XMLFileConfiguration().getFlowConfigurations(integrationId, xmlConfiguration);
		
		return properties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String jsonConfiguration) throws Exception {

		xmlConfiguration = DocConverter.convertJsonToXml(jsonConfiguration);
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
		
		return flowproperties;
	}
	
}