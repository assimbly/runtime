package org.assimbly.connector.configuration;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONObject;
import org.json.XML;

public class JSONFileConfiguration {

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;
	private String jsonConfiguration;
	private String xmlConfiguration;
	private List<TreeMap<String, String>> gatewayProperties;
	private TreeMap<String, String> flowproperties;
    
	public String createConfiguration(String connectorId, List<TreeMap<String, String>> configurations) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(connectorId, configurations);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlConfiguration);
        jsonConfiguration = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
		
		return jsonConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(configuration);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlConfiguration);
        
        jsonConfiguration = xmlJSONObj.toString(PRETTY_PRINT_INDENT_FACTOR);
		
		return jsonConfiguration;
	}

	public List<TreeMap<String, String>> getConfiguration(String connectorId, String configuration) throws Exception {
		
        JSONObject json = new JSONObject(configuration);
        xmlConfiguration = XML.toString(json);
		
		gatewayProperties =  new XMLFileConfiguration().getConfiguration(connectorId, xmlConfiguration);
		
		return gatewayProperties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String configuration) throws Exception {
		
        JSONObject json = new JSONObject(configuration);
        xmlConfiguration = XML.toString(json);
		
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
		
		return flowproperties;
	}
	
}