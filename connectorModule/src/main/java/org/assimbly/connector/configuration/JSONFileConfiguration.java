package org.assimbly.connector.configuration;

import java.util.List;
import java.util.TreeMap;

import org.assimbly.docconverter.DocConverter;

public class JSONFileConfiguration {

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;

	private List<TreeMap<String, String>> properties;
	private TreeMap<String, String> flowproperties;

	private String jsonConfiguration;
	private String xmlConfiguration;

	public String createConfiguration(String connectorId, List<TreeMap<String, String>> properties) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(connectorId, properties);
		jsonConfiguration = DocConverter.convertXmlToJson(xmlConfiguration);
		
		return jsonConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> flowProperties) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(flowProperties);
		jsonConfiguration = DocConverter.convertXmlToJson(xmlConfiguration);
		
		return jsonConfiguration;
	}

	public List<TreeMap<String, String>> getConfiguration(String connectorId, String jsonConfiguration) throws Exception {
		
		xmlConfiguration = DocConverter.convertJsonToXml(jsonConfiguration);
		properties =  new XMLFileConfiguration().getConfiguration(connectorId, xmlConfiguration);
		
		return properties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String jsonConfiguration) throws Exception {

		xmlConfiguration = DocConverter.convertJsonToXml(jsonConfiguration);
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
		
		return flowproperties;
	}
	
}