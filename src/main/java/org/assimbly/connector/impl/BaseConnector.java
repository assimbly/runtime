package org.assimbly.connector.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assimbly.connector.configuration.JSONFileConfiguration;
import org.assimbly.connector.configuration.XMLFileConfiguration;
import org.assimbly.connector.configuration.YAMLFileConfiguration;
import org.assimbly.connector.Connector;


public abstract class BaseConnector implements Connector {
	
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.impl.BaseConnector");

	//properties are (list of) key/value maps
	private List<TreeMap<String,String>> properties = new ArrayList<>();
	private TreeMap<String, String> flowProperties;
	private List<TreeMap<String,String>> connections = new ArrayList<>();

	//configuration are strings
	private String configuration;
	private String flowConfiguration;
	
	public void setConfiguration(List<TreeMap<String, String>> configuration) throws Exception {
		for (TreeMap<String, String> flowConfiguration : configuration){
			setFlowConfiguration(flowConfiguration);
		}		
	}

	public void setConfiguration(String connectorId, String mediaType, String configuration) throws Exception {
		
		mediaType = mediaType.toLowerCase();
		
		if(mediaType.contains("xml")) {
			this.properties = convertXMLToConfiguration(connectorId, configuration);
		}else if(mediaType.contains("json")) {
			this.properties = convertJSONToConfiguration(connectorId, configuration);
		}else {
			this.properties = convertYAMLToConfiguration(connectorId, configuration);
		}
        
        setConfiguration(this.properties);
		
	}	
	
	public List<TreeMap<String,String>> getConfiguration() throws Exception {
		return this.properties;
	}

	public String getConfiguration(String connectorId, String mediaType) throws Exception {
		
		this.properties = getConfiguration();
		mediaType = mediaType.toLowerCase();

		if(mediaType.contains("xml")) {
        	configuration = convertConfigurationToXML(connectorId,this.properties);
		}else if(mediaType.contains("json")) {
        	configuration = convertConfigurationToJSON(connectorId,this.properties);
		}else {
        	configuration = convertConfigurationToYAML(connectorId,this.properties);
		}
        
        return configuration;
		
	}
	
	public void setFlowConfiguration(TreeMap<String,String> configuration) throws Exception {
		this.properties.add(configuration);
	}	

	public void setFlowConfiguration(String flowId, String mediaType, String configuration) throws Exception {
		
		mediaType = mediaType.toLowerCase();

		if(mediaType.contains("xml")) {
        	flowProperties = convertXMLToFlowConfiguration(flowId, configuration);
		}else if(mediaType.contains("json")) {
        	flowProperties = convertJSONToFlowConfiguration(flowId, configuration);
		}else {
        	flowProperties = convertYAMLToFlowConfiguration(flowId, configuration);
		}
        
        setFlowConfiguration(flowProperties);
		
	}
	
	public TreeMap<String,String> getFlowConfiguration(String flowId) throws Exception {
		TreeMap<String,String> flowConfiguration = null;
		for (TreeMap<String, String> props : getConfiguration()) {
			if (props.get("id").equals(flowId)) {
				flowConfiguration = props;
			}
		}
		
		return flowConfiguration;
	}	

	public String getFlowConfiguration(String flowId, String mediaType) throws Exception {
		
		this.flowProperties = getFlowConfiguration(flowId);
		mediaType = mediaType.toLowerCase();

		if(mediaType.contains("xml")) {
        	flowConfiguration = convertFlowConfigurationToXML(this.flowProperties);
		}else if(mediaType.contains("json")) {
        	flowConfiguration = convertFlowConfigurationToJSON(this.flowProperties);
		}else {
        	flowConfiguration = convertFlowConfigurationToYAML(this.flowProperties);
		}
        
        return flowConfiguration;
		
	}
	
	
	@SuppressWarnings("unused")
	private List<TreeMap<String,String>> getServices() throws Exception {
		return this.connections;
	}
    	
	@SuppressWarnings("unused")
	private void addService(TreeMap<String,String> properties) throws Exception {
		this.connections.add(properties);
	}
	
	@SuppressWarnings("unused")
	private boolean removeService(String id) throws Exception {
		TreeMap<String, String> con = null;
		for (TreeMap<String, String> connection : connections){
			if (connection.get("connection_id").equals(id)){
				con = connection;
			}
		}
		if (con != null){
			this.connections.remove(con);
			return true;
		}
		else{
			return false;
		}
	}
	
	//--> convert methods (XML)
	public String convertConfigurationToXML(String connectorid, List<TreeMap<String, String>> configuration) throws Exception {
        return new XMLFileConfiguration().createConfiguration(connectorid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String connectorid, String configuration) throws Exception {
		return new XMLFileConfiguration().getConfiguration(connectorid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String connectorid, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getConfiguration(connectorid, configurationUri);
	}
	
	public TreeMap<String, String> convertXMLToFlowConfiguration(String connectorId, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(connectorId, configuration);
	}

	public TreeMap<String, String> convertXMLToFlowConfiguration(String flowId, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(flowId, configurationUri);
	}	
	
	public String convertFlowConfigurationToXML(TreeMap<String, String> configuration) throws Exception {
		return new XMLFileConfiguration().createFlowConfiguration(configuration);
	}
	
	//--> convert methods (JSON)	
	public String convertConfigurationToJSON(String connectorid, List<TreeMap<String, String>> configuration) throws Exception {
        return new JSONFileConfiguration().createConfiguration(connectorid, configuration);
	}
	
	public List<TreeMap<String, String>> convertJSONToConfiguration(String connectorid, String configuration) throws Exception {
		return new JSONFileConfiguration().getConfiguration(connectorid, configuration);
	}	
	
	public TreeMap<String, String> convertJSONToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new JSONFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

	public String convertFlowConfigurationToJSON(TreeMap<String, String> configuration) throws Exception {
		return new JSONFileConfiguration().createFlowConfiguration(configuration);
	}
	
	//--> convert methods (YAML)	
	
	public String convertConfigurationToYAML(String connectorid, List<TreeMap<String, String>> configuration) throws Exception {
        return new YAMLFileConfiguration().createConfiguration(connectorid, configuration);
	}
	
	public List<TreeMap<String, String>> convertYAMLToConfiguration(String connectorid, String configuration) throws Exception {
		return new YAMLFileConfiguration().getConfiguration(connectorid, configuration);
	}	
	
	public TreeMap<String, String> convertYAMLToFlowConfiguration(String flowId, String configuration) throws Exception {
		return new YAMLFileConfiguration().getFlowConfiguration(flowId, configuration);
	}	

	public String convertFlowConfigurationToYAML(TreeMap<String, String> configuration) throws Exception {
		return new YAMLFileConfiguration().createFlowConfiguration(configuration);
	}
	
	//--> abstract methods (needs to be implemented in the subclass specific to the integration framework)
	
	public abstract void start() throws Exception;

	public abstract boolean isStarted();

	public abstract void stop() throws Exception;

	public abstract String getStats(String statsType, String mediaType) throws Exception;	

	public abstract Object getContext() throws Exception;
	
	public abstract boolean removeFlow(String id) throws Exception;

	public abstract boolean hasFlow(String id);
	
	public abstract String startFlow(String id) throws Exception;

	public abstract String restartFlow(String id) throws Exception;
	
	public abstract String stopFlow(String id) throws Exception;

	public abstract String pauseFlow(String id) throws Exception;
	
	public abstract String resumeFlow(String id) throws Exception;

	public abstract String getFlowStatus(String id) throws Exception;

	public abstract String getFlowUptime(String id) throws Exception;

	public abstract String getFlowLastError(String id);	
	
	public abstract String getFlowTotalMessages(String id) throws Exception;	
	
	public abstract String getFlowCompletedMessages(String id) throws Exception;
	
	public abstract String getFlowFailedMessages(String id) throws Exception;	
	
	public abstract String getFlowStats(String id, String mediaType) throws Exception;
	
	public abstract void send(Object messageBody, ProducerTemplate template);

	public abstract void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);
	
}
