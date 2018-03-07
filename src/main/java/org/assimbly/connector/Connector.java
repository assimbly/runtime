package org.assimbly.connector;

import java.net.URI;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.ProducerTemplate;

public interface Connector {

	//configure connector
	//public void setConfiguration(List<TreeMap<String,String>>) throws Exception;
	public List<TreeMap<String,String>> getConfiguration() throws Exception;
	public List<TreeMap<String,String>> getServices() throws Exception;

	//convert configuration
	public String convertConfigurationToXML(String connectorid,List<TreeMap<String,String>> configuration) throws Exception;
	//public List<TreeMap<String,String>> convertXMLToConfiguration(String configuration) throws Exception;
	//public List<TreeMap<String,String>> convertXMLToConfiguration(URI configurationUri) throws Exception;
	
	//configure route
	public void setRouteConfiguration(TreeMap<String,String> configuration) throws Exception;	
	public TreeMap<String,String> getRouteConfiguration(String id) throws Exception;

	//convert route configuration
	public TreeMap<String,String> convertXMLToRouteConfiguration(String id, String configuration) throws Exception;
	public TreeMap<String,String> convertXMLToRouteConfiguration(String id, URI configurationUri) throws Exception;
	public String convertRouteConfigurationToXML(TreeMap<String,String> configuration) throws Exception;
	
	//manage connector
	public void start() throws Exception;
	public void stop() throws Exception;
	public boolean isStarted();
	
	//manage route
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