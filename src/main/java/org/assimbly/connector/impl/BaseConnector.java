package org.assimbly.connector.impl;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assimbly.connector.configuration.XMLFileConfiguration;
import org.assimbly.connector.Connector;


public abstract class BaseConnector implements Connector {
	
	@SuppressWarnings("unused")
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.impl.BaseConnector");

	private List<TreeMap<String,String>> properties = new ArrayList<>();
	private List<TreeMap<String,String>> connections = new ArrayList<>();
	
	public void setConfiguration(List<TreeMap<String, String>> configuration) throws Exception {
		for (TreeMap<String, String> flowConfiguration : configuration){
			setFlowConfiguration(flowConfiguration);
		}		
	}
	
	public List<TreeMap<String,String>> getConfiguration() throws Exception {
		return this.properties;
	}

	public void setFlowConfiguration(TreeMap<String,String> configuration) throws Exception {
		this.properties.add(configuration);
	}	

	public TreeMap<String,String> getFlowConfiguration(String id) throws Exception {
		TreeMap<String,String> configuration = null;
		for (TreeMap<String, String> props : getConfiguration()) {
			if (props.get("id").equals(id)) {
				configuration = props;
			}
		}
		
		return configuration;
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
	
	//convert methods
	public String convertConfigurationToXML(String connectorid, List<TreeMap<String, String>> configuration) throws Exception {
        return new XMLFileConfiguration().createConfiguration(connectorid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String connectorid, String configuration) throws Exception {
		return new XMLFileConfiguration().getConfiguration(connectorid, configuration);
	}

	public List<TreeMap<String, String>> convertXMLToConfiguration(String connectorid, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getConfiguration(connectorid, configurationUri);
	}
	
	public TreeMap<String, String> convertXMLToFlowConfiguration(String connectorID, String configuration) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(connectorID, configuration);
	}

	public TreeMap<String, String> convertXMLToFlowConfiguration(String connectorID, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().getFlowConfiguration(connectorID, configurationUri);
	}	
	
	public String convertFlowConfigurationToXML(TreeMap<String, String> configuration) throws Exception {
		return new XMLFileConfiguration().createFlowConfiguration(configuration);
	}
	
	//--> abstract methods (needs to be implemented in the subclass
	
	public abstract void start() throws Exception;

	public abstract boolean isStarted();

	public abstract void stop() throws Exception;

	public abstract void removeFlow(String id) throws Exception;

	public abstract boolean hasFlow(String id);
	
	public abstract void startFlow(String id) throws Exception;

	public abstract void restartFlow(String id) throws Exception;
	
	public abstract void stopFlow(String id) throws Exception;

	public abstract void pauseFlow(String id) throws Exception;
	
	public abstract void resumeFlow(String id) throws Exception;

	public abstract String getFlowStatus(String id) throws Exception;
	
	public abstract Object getContext() throws Exception;
	
	public abstract void send(Object messageBody, ProducerTemplate template);

	public abstract void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);
	
}
