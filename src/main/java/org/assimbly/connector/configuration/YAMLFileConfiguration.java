package org.assimbly.connector.configuration;

import java.util.List;
import java.util.TreeMap;

import org.json.JSONObject;
import org.json.XML;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

public class YAMLFileConfiguration {

    public static int PRETTY_PRINT_INDENT_FACTOR = 4;
	private String jsonConfiguration;
	private String xmlConfiguration;
	private String yamlConfiguration;

	private List<TreeMap<String, String>> gatewayProperties;
	private TreeMap<String, String> flowproperties;
    
	public String createConfiguration(String connectorId, List<TreeMap<String, String>> configurations) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createConfiguration(connectorId, configurations);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlConfiguration);

        jsonConfiguration = xmlJSONObj.toString();
        
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonConfiguration);
        yamlConfiguration = new YAMLMapper().writeValueAsString(jsonNodeTree);
        
        return yamlConfiguration;

	}

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		xmlConfiguration = new XMLFileConfiguration().createFlowConfiguration(configuration);

        JSONObject xmlJSONObj = XML.toJSONObject(xmlConfiguration);
        
        jsonConfiguration = xmlJSONObj.toString();
        
        JsonNode jsonNodeTree = new ObjectMapper().readTree(jsonConfiguration);
        yamlConfiguration = new YAMLMapper().writeValueAsString(jsonNodeTree);
        
        return yamlConfiguration;
        
 	}

	public List<TreeMap<String, String>> getConfiguration(String connectorId, String configuration) throws Exception {

	    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
	    Object obj = yamlReader.readValue(configuration, Object.class);

	    ObjectMapper jsonWriter = new ObjectMapper();
	    jsonConfiguration = jsonWriter.writeValueAsString(obj);
		
        JSONObject json = new JSONObject(jsonConfiguration);
        xmlConfiguration = XML.toString(json);
		
		gatewayProperties =  new XMLFileConfiguration().getConfiguration(connectorId, xmlConfiguration);
		
		return gatewayProperties;
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String configuration) throws Exception {

	    ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
	    Object obj = yamlReader.readValue(configuration, Object.class);

	    ObjectMapper jsonWriter = new ObjectMapper();
	    jsonConfiguration = jsonWriter.writeValueAsString(obj);
		
        JSONObject json = new JSONObject(configuration);
        xmlConfiguration = XML.toString(json);
		
		flowproperties =  new XMLFileConfiguration().getFlowConfiguration(flowId, xmlConfiguration);
		
		return flowproperties;
	}
	
}