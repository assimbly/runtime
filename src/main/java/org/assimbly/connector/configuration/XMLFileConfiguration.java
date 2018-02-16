package org.assimbly.connector.configuration;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.TreeMap;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.assimbly.connector.connect.util.ConnectorUtil;
import org.w3c.dom.Document;

public class XMLFileConfiguration {
	
	private TreeMap<String, String> properties;
	private String connectorID;
	
	private XMLConfiguration conf;

	private String connectionID;
	private String connectionXPath;
	private String connectorXPath;
	
	private String uri;
	private String component;
	private String options;
	private String headerID;

	public TreeMap<String, String> set(String connectorID, URI uri) throws Exception {

	   this.connectorID = connectorID;

	   String scheme = uri.getScheme();
	   //load uri to configuration 
	   Parameters params = new Parameters();
	   
		if(scheme.startsWith("sonicfs")) {

			URL Url = uri.toURL();

	        InputStream is = Url.openStream();        	        

	        conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(params.xml()).getConfiguration();
	        FileHandler fh = new FileHandler(conf);
	        fh.load(is);
			
		}else if (scheme.startsWith("file")) {
			
			   File xml = new File(uri.getRawPath());

			   FileBasedConfigurationBuilder<XMLConfiguration> builder =
			       new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
			       .configure(params.xml()
			           .setFileName(xml.getName())
			           .setFile(xml)
			           .setSchemaValidation(false));

			   // This will throw a ConfigurationException if the XML document does not
			   // conform to its Schema.
			   conf = builder.getConfiguration();				
		}else if (scheme.startsWith("http")) {
			
			URL Url = uri.toURL();
			
			FileBasedConfigurationBuilder<XMLConfiguration> builder =
			       new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
			       .configure(params.xml()
			    	   .setURL(Url)
			           .setFileName("conf.xml")
			           .setSchemaValidation(false));

			   // This will throw a ConfigurationException if the XML document does not
			   // conform to its Schema.
			   conf = builder.getConfiguration();				
		}else {
    		throw new Exception("URI scheme for " + uri.getRawPath() + " is not supported");        		        	
		}

	   //create Treemap from configuration
	   properties = new TreeMap<String, String>();

	   //set general properties
	   setGeneralPropertiesFromXMLFile();
	   
	   //set from properties
	   setURIfromXMLFile("from");
	   setServiceFromXMLFile("from");
	   setHeaderFromXMLFile("from");
	   
	   //set to properties
	   setURIfromXMLFile("to");		   
	   setServiceFromXMLFile("to");
	   setHeaderFromXMLFile("to");
	   
	   //set error properties
	   setURIfromXMLFile("error");
	   setServiceFromXMLFile("error");
	   setHeaderFromXMLFile("error");
	   
	   //set up defaults settings if null -->
		if(connectorID == null){    			
			connectorID = "route" + System.currentTimeMillis();					
		}
		properties.put("id",connectorID);	
	   
	   	if(properties.get("from.uri") != null){
			properties.put("route","default");
		}else{
			properties.put("route", "none");
		}

		if(properties.get("to.uri") == null){
			properties.put("to.uri","mock:wastebin");		
		}
   	   	   
		properties.put("header.contenttype", "text/xml;charset=UTF-8");
	   
	   return properties;
	}

	private void setURIfromXMLFile(String type) throws ConfigurationException {

		   options = "";	
		   
		   Document doc = conf.getDocument();
		   
		   XPath xPath = XPathFactory.newInstance().newXPath();
		   try {
			component = xPath.evaluate("//connectors/connector[id='" + connectorID + "']/" + type + "/uri",doc);
		   } catch (XPathExpressionException e) {
			   e.printStackTrace();
		   }
		   
		   if(component == null || component.isEmpty()){return;};
		   
		   List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, "connectors/connector[id='" + connectorID + "']/" + type + "/options");
		   
	  	   for(String optionProperty : optionProperties){
			   options += optionProperty.split("options.")[1] + "=" + conf.getProperty(optionProperty) + "&";
	  	   }
	  	   
	  	   if(options.isEmpty()){
	  		 uri = component;
	  	   }else{
	  		 options = options.substring(0,options.length() -1);
	  		 uri = component + "?" + options;  
	  	   }
	  	   
	  	   properties.put(type + ".uri", uri);
	  	   
	}

	private void setServiceFromXMLFile(String type) throws ConfigurationException {
		   	
	    connectionID = conf.getString("connectors/connector[id='" + connectorID + "']/" + type + "/connection_id");
	    if(connectionID == null){return;};
	    
	    properties.put(type + ".connection_id", connectionID);
	    
	    connectionXPath = "connections/connection[connection_id='" + connectionID + "']";
		List<String> connectionProporties = ConnectorUtil.getXMLParameters(conf, connectionXPath);
		
		if(!connectionProporties.isEmpty()){
	  	   for(String connectionProperty : connectionProporties){
	  		   properties.put(type + "." + connectionProperty.substring(connectionXPath.length() + 1), conf.getString(connectionProperty));
    	   }
		}
	}
	
	private void setGeneralPropertiesFromXMLFile() throws ConfigurationException{
		connectorXPath = "connectors/connector[id='" + connectorID + "']";
		List<String> connectorProporties = ConnectorUtil.getXMLParameters(conf, connectorXPath);
		if(!connectorProporties.isEmpty()){
	  	   for(String connectorProperty : connectorProporties){
	  		   properties.put(connectorProperty.substring(connectorXPath.length() + 1), conf.getString(connectorProperty));
    	   }
		}
	}
	
	private void setHeaderFromXMLFile(String type) throws ConfigurationException {
	   	
	    headerID = conf.getString("connectors/connector[id='" + connectorID + "']/" + type + "/header_id");

	    if(headerID == null){return;};
		   	    
	    properties.put(type + ".header_id", headerID);
	    
	    connectionXPath = "headers/header[id='" + headerID + "']";
		List<String> headerProporties = ConnectorUtil.getXMLParameters(conf, connectionXPath);
		
		if(!headerProporties.isEmpty()){
	  	   for(String headerProperty : headerProporties){
	  		   String headerType = conf.getString(headerProperty + "/@type");
	  		   if(headerType!=null){
		  		   properties.put(type + ".header." + headerType + "." + headerProperty.substring(connectionXPath.length() + 1), conf.getString(headerProperty));
	  		   }
    	   }
		}
	}	
}