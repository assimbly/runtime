package org.assimbly.dil.transpiler;

import org.assimbly.docconverter.DocConverter;

import java.util.List;
import java.util.TreeMap;

public class YAMLFileConfiguration {

	private String xmlConfiguration;
	private String yamlConfiguration;

	public String createConfiguration(String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(integrationId, configurations);

		yamlConfiguration = DocConverter.xmlToYaml(xmlConfiguration);
		        
        return yamlConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(configuration);

		yamlConfiguration = DocConverter.xmlToYaml(xmlConfiguration);
        
        return yamlConfiguration;
        
 	}

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, String configuration) throws Exception {

		xmlConfiguration = DocConverter.yamlToXml(configuration);

        return new XMLFileConfiguration().getFlowConfigurations(integrationId, xmlConfiguration);
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String configuration) throws Exception {

		xmlConfiguration = DocConverter.yamlToXml(configuration);

        return new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
	}
	
}