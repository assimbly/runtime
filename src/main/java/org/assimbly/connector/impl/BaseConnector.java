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
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.BaseConnector");

	private List<TreeMap<String,String>> properties = new ArrayList<>();
	private List<TreeMap<String,String>> connections = new ArrayList<>();

	public void setRouteConfiguration(TreeMap<String,String> configuration) throws Exception {
		this.properties.add(configuration);
	}	

	public TreeMap<String,String> getRouteConfiguration(String id) throws Exception {
		TreeMap<String,String> configuration = null;
		for (TreeMap<String, String> props : getConfiguration()) {
			if (props.get("id").equals(id)) {
				configuration = props;
			}
		}
		
		return configuration;
	}	

	public List<TreeMap<String,String>> getConfiguration() throws Exception {
		return this.properties;
	}

	public List<TreeMap<String,String>> getServices() throws Exception {
		return this.connections;
	}
    	
	public void addConnection(TreeMap<String,String> properties) throws Exception {
		this.connections.add(properties);
	}
	
	public boolean removeConnection(String id) throws Exception {
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
	
	//initial setup of configuration from external source (for example XML file)
	public TreeMap<String, String> convertXMLToRouteConfiguration(String connectorID, String configuration) throws Exception {
		return new XMLFileConfiguration().get(connectorID, configuration);
	}

	public TreeMap<String, String> convertXMLToRouteConfiguration(String connectorID, URI configurationUri) throws Exception {
		return new XMLFileConfiguration().get(connectorID, configurationUri);
	}	
	
	@Override
	public String convertRouteConfigurationToXML(TreeMap<String, String> configuration) throws Exception {
		return new XMLFileConfiguration().create(configuration);
	}
	
	@Override
	public String convertConfigurationToXML(String gatewayid, List<TreeMap<String, String>> configuration) throws Exception {
        return new XMLFileConfiguration().create(gatewayid, configuration);
	}
	
	
	//--> abstract methods (needs to be implemented in the subclass
	
	public abstract void start() throws Exception;

	public abstract boolean isStarted();

	public abstract void stop() throws Exception;

	public abstract void addRoute(TreeMap<String, String> props) throws Exception;
	
	public abstract void removeRoute(String id) throws Exception;

	public abstract boolean hasRoute(String id);
	
	public abstract void startRoute(String id) throws Exception;

	public abstract void restartRoute(String id) throws Exception;
	
	public abstract void stopRoute(String id) throws Exception;

	public abstract void pauseRoute(String id) throws Exception;
	
	public abstract void resumeRoute(String id) throws Exception;

	public abstract String getRouteStatus(String id) throws Exception;
	
	public abstract Object getContext() throws Exception;
	
	public abstract void send(Object messageBody, ProducerTemplate template);

	public abstract void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);
	
}
