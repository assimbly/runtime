package org.assimbly.dil.transpiler;

import org.assimbly.docconverter.DocConverter;

import java.util.List;
import java.util.TreeMap;

public class JSONFileConfiguration {

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

        return new XMLFileConfiguration().getFlowConfigurations(integrationId, xmlConfiguration);
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String jsonConfiguration) throws Exception {

		xmlConfiguration = DocConverter.convertJsonToXml(jsonConfiguration);

        return new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
	}
	
}