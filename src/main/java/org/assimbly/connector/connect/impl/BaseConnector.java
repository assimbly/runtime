package org.assimbly.connector.connect.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.ProducerTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assimbly.connector.configuration.XMLFileConfiguration;
import org.assimbly.connector.connect.Connector;
import org.assimbly.connector.connect.util.ConnectorUtil;


public abstract class BaseConnector implements Connector {
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.BaseConnector");

	
	private String configuration;
	private List<TreeMap<String,String>> properties = new ArrayList<>();
	private List<TreeMap<String,String>> connections = new ArrayList<>();
	
	@Override
	public List<TreeMap<String,String>> getConfigurations() throws Exception {
		return this.properties;
	}
		
	@Override
	public void addConfiguration(TreeMap<String,String> properties) throws Exception {
		this.properties.add(properties);
	}	
	
	@Override
	public List<TreeMap<String,String>> getConnections() throws Exception {
		return this.connections;
	}
    	
	
	@Override
	public void addConnection(TreeMap<String,String> properties) throws Exception {
		this.connections.add(properties);
	}
	
	@Override
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
	public void addConfiguration(String connectorID, String configuration) throws Exception {
		
			URI configurationUri;
			
			if(ConnectorUtil.isValidURI(configuration)){
				 	configurationUri = new URI(configuration);
				 	
			}
			else{
				File file = new File(configuration);
				configurationUri = file.toURI();				
			}
			addConfiguration(connectorID, configurationUri);
	}

	public void addConfiguration(String connectorID, URI configurationUri) throws Exception {

		String uriPath = configurationUri.getRawPath();
		
		if (uriPath.contains(":")){
			uriPath = uriPath.substring(uriPath.indexOf("/")+1);
		}
	
		String extension = uriPath.substring(uriPath.lastIndexOf(".") + 1);

		if(extension.equals("xml")){
	        this.properties.add(new XMLFileConfiguration().set(connectorID, configurationUri));
		}else{
    		logger.error("Extension " + extension + "for URI " + configuration + " is not support");
    		throw new Exception("Extension " + extension + "for URI " + configuration + " is not support");        		        	
		}
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
