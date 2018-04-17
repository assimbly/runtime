package org.assimbly.connector.configuration;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONObject;
import org.json.XML;

public class JSONFileConfiguration {

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;
	private String jsonConfiguration;
	private String xmlconfiguration;
	private List<TreeMap<String, String>> gatewayProperties;
	private TreeMap<String, String> flowproperties;
    
	public String createConfiguration(String connectorId, List<TreeMap<String, String>> configurations) throws Exception {

		xmlconfiguration = new XMLFileConfiguration().createConfiguration(connectorId, configurations);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlconfiguration);
        jsonConfiguration = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
		
		return jsonConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		String xmlconfiguration = new XMLFileConfiguration().createFlowConfiguration(configuration);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlconfiguration);
        jsonConfiguration = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
		
		return jsonConfiguration;
	}

	public List<TreeMap<String, String>> getConfiguration(String connectorId, String configuration) throws Exception {
		
        JSONObject json = new JSONObject(configuration);
        xmlconfiguration = XML.toString(json);
		
		gatewayProperties =  new XMLFileConfiguration().getConfiguration(connectorId, xmlconfiguration);
		
		return gatewayProperties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String configuration) throws Exception {
		
        JSONObject json = new JSONObject(configuration);
        xmlconfiguration = XML.toString(json);
		
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlconfiguration);
		
		return flowproperties;
	}
	
}