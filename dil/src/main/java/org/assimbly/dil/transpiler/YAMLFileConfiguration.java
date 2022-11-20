package org.assimbly.dil.transpiler;

import java.util.List;
import java.util.TreeMap;

import org.assimbly.docconverter.DocConverter;

public class YAMLFileConfiguration {

	private String xmlConfiguration;
	private String yamlConfiguration;

	private List<TreeMap<String, String>> gatewayProperties;
	private TreeMap<String, String> flowproperties;
    
	public String createConfiguration(String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(integrationId, configurations);

		yamlConfiguration = DocConverter.convertXmlToYaml(xmlConfiguration);
		        
        return yamlConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(configuration);

		yamlConfiguration = DocConverter.convertXmlToYaml(xmlConfiguration);
        
        return yamlConfiguration;
        
 	}

	public List<TreeMap<String, String>> getFlowConfigurations(String integrationId, String configuration) throws Exception {

		xmlConfiguration = DocConverter.convertYamlToXml(configuration);
		
		gatewayProperties =  new XMLFileConfiguration().getFlowConfigurations(integrationId, xmlConfiguration);
		
		return gatewayProperties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String configuration) throws Exception {

		xmlConfiguration = DocConverter.convertYamlToXml(configuration);
		
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
		
		return flowproperties;
	}
	
}