package org.assimbly.connector.connect;

import java.net.URI;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.ProducerTemplate;

public interface Connector {

	//configure
	public void addConfiguration(TreeMap<String,String> properties) throws Exception;	
	public void addConfiguration(String connectorID, String configuration) throws Exception;
	public void addConfiguration(String connectorID, URI configurationUri) throws Exception;
	public List<TreeMap<String,String>> getConfigurations() throws Exception;
	public List<TreeMap<String,String>> getConnections() throws Exception;
	
	//connector
	public void start() throws Exception;
	public void stop() throws Exception;
	public boolean isStarted();

	//route	
	public void addRoute(TreeMap<String,String> properties) throws Exception;
	public void removeRoute(String id) throws Exception;
	public boolean hasRoute(String id);
	public void addConnection(TreeMap<String,String> connections) throws Exception;	
	public boolean removeConnection(String id) throws Exception;	
	public void startRoute(String id) throws Exception;
	public void restartRoute(String id) throws Exception;
	public void stopRoute(String id) throws Exception;
	public void resumeRoute(String id) throws Exception;
	public void pauseRoute(String id) throws Exception;
	public String getRouteStatus(String id) throws Exception;
	public Object getContext() throws Exception;
	
	//send messages 
	public void send(Object messageBody, ProducerTemplate template);
	public void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template);
	
}