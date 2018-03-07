package org.assimbly.connector.configuration;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.io.FileHandler;
import org.assimbly.connector.connect.util.ConnectorUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLFileConfiguration {
	
	private TreeMap<String, String> properties;
	private String routeID;
	
	private XMLConfiguration conf;

	private String serviceID;
	private String serviceXPath;
	private String connectorXPath;
	
	private String uri;
	private String component;
	private String options;
	private String headerID;
	private Element rootElement;
	private Document doc;
	private Element routes;
	private Element services;
	private Element headers;
	private Element route;
	private String xmlconfiguration;
	
	public TreeMap<String, String> get(String routeID, String xml) throws Exception {

	    this.routeID = routeID;
	
	    conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()).getConfiguration();
	    FileHandler fh = new FileHandler(conf);
	    fh.load(ConnectorUtil.convertStringToStream(xml));
	    
	   setProperties();

	   return properties;
	
	}
	
	public TreeMap<String, String> get(String routeID, URI uri) throws Exception {

	   this.routeID = routeID;

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

	   setProperties();
	   
	   return properties;
	}

	
	private void setProperties() throws Exception{
		
		   //create Treemap from configuration
		   properties = new TreeMap<String, String>();

		   //set general properties
		   getGeneralPropertiesFromXMLFile();
		   
		   //set from properties
		   getURIfromXMLFile("from");
		   getServiceFromXMLFile("from");
		   getHeaderFromXMLFile("from");
		   
		   //set to properties
		   getURIfromXMLFile("to");		   
		   getServiceFromXMLFile("to");
		   getHeaderFromXMLFile("to");
		   
		   //set error properties
		   getURIfromXMLFile("error");
		   getServiceFromXMLFile("error");
		   getHeaderFromXMLFile("error");
		   
		   //set up defaults settings if null -->
			if(routeID == null){    			
				routeID = "route" + System.currentTimeMillis();					
			}
			properties.put("id",routeID);	
		   
		   	if(properties.get("from.uri") != null){
				properties.put("route","default");
			}else{
				properties.put("route", "none");
			}

			if(properties.get("to.uri") == null){
				properties.put("to.uri","mock:wastebin");		
			}
	   	   	   
			properties.put("header.contenttype", "text/xml;charset=UTF-8");
		
	}

	private void getGeneralPropertiesFromXMLFile() throws Exception{
		
		Document doc = conf.getDocument();
		   
	    XPath xPath = XPathFactory.newInstance().newXPath();
	    routeID = xPath.evaluate("//routes/route[id='" + routeID + "']/id",doc);
		
		if(routeID==null || routeID.isEmpty()) {throw new ConfigurationException("ID (route) doesn't exists in XML Configuration");}

		connectorXPath = "routes/route[id='" + routeID + "']";

		List<String> connectorProporties = ConnectorUtil.getXMLParameters(conf, connectorXPath);
		
		if(!connectorProporties.isEmpty()){
	  	   for(String connectorProperty : connectorProporties){
	  		   properties.put(connectorProperty.substring(connectorXPath.length() + 1), conf.getString(connectorProperty));
    	   }
		}
	}
	
	private void getURIfromXMLFile(String type) throws Exception {

		   options = "";	
		   
		   Document doc = conf.getDocument();
		   
		   XPath xPath = XPathFactory.newInstance().newXPath();
		   component = xPath.evaluate("//routes/route[id='" + routeID + "']/" + type + "/uri",doc);
		   
		   if(component == null || component.isEmpty()){return;};
		   
		   List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, "routes/route[id='" + routeID + "']/" + type + "/options");
		   
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

	private void getServiceFromXMLFile(String type) throws ConfigurationException {
		   	
	    serviceID = conf.getString("routes/route[id='" + routeID + "']/" + type + "/service_id");
	    if(serviceID == null){return;};
	    
	    properties.put(type + ".service_id", serviceID);
	    
	    serviceXPath = "services/services[service_id='" + serviceID + "']";
		List<String> serviceProporties = ConnectorUtil.getXMLParameters(conf, serviceXPath);
		
		if(!serviceProporties.isEmpty()){
	  	   for(String serviceProperty : serviceProporties){
	  		   properties.put(type + ".service." + serviceProperty.substring(serviceXPath.length() + 1), conf.getString(serviceProperty));
    	   }
		}
	}
	
	private void getHeaderFromXMLFile(String type) throws ConfigurationException {
	   	
	    headerID = conf.getString("routes/route[id='" + routeID + "']/" + type + "/header_id");

	    if(headerID == null){return;};
	    
	    properties.put(type + ".header_id", headerID);
	    
	    serviceXPath = "headers/header[id='" + headerID + "']";
		List<String> headerProporties = ConnectorUtil.getXMLParameters(conf, serviceXPath);
		
		if(!headerProporties.isEmpty()){
	  	   for(String headerProperty : headerProporties){
	  		   String headerType = conf.getString(headerProperty + "/@type");
	  		   if(headerType!=null){
		  		   properties.put(type + ".header." + headerType + "." + headerProperty.substring(serviceXPath.length() + 1), conf.getString(headerProperty));
	  		   }
    	   }
		}
	}

	public String create(String gatewayid, List<TreeMap<String, String>> configurations) throws Exception {

	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	    doc = docBuilder.newDocument();
		
	    setGeneralProperties(gatewayid);

		for (TreeMap<String, String> configuration : configurations) {
		    setRouteFromConfiguration(configuration);
		}

	    String xmlconfiguration = ConnectorUtil.convertDocToString(doc);
	    
		return xmlconfiguration;

	}
	

	public String create(TreeMap<String, String> configuration) throws Exception {

	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	    doc = docBuilder.newDocument();
		
	    setGeneralProperties("live");
	    
	    setRouteFromConfiguration(configuration);
	    
	    if(doc!=null) {
		    xmlconfiguration = ConnectorUtil.convertDocToString(doc);
	    }else {
	    	xmlconfiguration = "Can't create XML File";
	    }
	    
		return xmlconfiguration;
	}
	
	private void setGeneralProperties(String gatewayid) {
		
	    rootElement = doc.createElement("connector");
	    doc.appendChild(rootElement);

	    Element id = doc.createElement("id");
	    id.appendChild(doc.createTextNode(gatewayid));
	    rootElement.appendChild(id);
	    
	    routes = doc.createElement("routes");
	    services = doc.createElement("services");
	    headers = doc.createElement("headers");
	    
	    rootElement.appendChild(routes);
	    rootElement.appendChild(services);
	    rootElement.appendChild(headers);
		
	}
	
	
	private void setRouteFromConfiguration(TreeMap<String, String> configuration) throws Exception {

	    route = doc.createElement("route");
	    routes.appendChild(route);
	    
	    //set id
	    String routeID = configuration.get("id");	    
	    Element id = doc.createElement("id");
	    id.appendChild(doc.createTextNode(routeID));
	    route.appendChild(id);

	    //set id
	    String routeType = configuration.get("route");	    
	    Element routeTypeNode = doc.createElement("route");
	    routeTypeNode.appendChild(doc.createTextNode(routeType));
	    route.appendChild(routeTypeNode);	    
	    
	    //set endpoints
	    setRouteEndpoint("from",configuration);
	    setRouteEndpoint("to",configuration);
	    setRouteEndpoint("error",configuration);
	    
	}
	
	private void setRouteEndpoint(String type, TreeMap<String, String> configuration) throws Exception {

		String confUri = configuration.get(type + ".uri");
		String confServiceId = configuration.get(type + ".service.id");
		String confHeaderId = configuration.get(type + ".header.id");

	    Element endpoint = doc.createElement(type);
	    Element uri = doc.createElement("uri");
	    Element options = doc.createElement("options");
	    Element serviceid = doc.createElement("service_id");
	    Element headerid = doc.createElement("header_id");

		if(confUri!=null) {

			route.appendChild(endpoint);

			String[] confUriSplitted = confUri.split("\\?");
			
			if(confUriSplitted.length<=1) {
			    uri.setTextContent(confUri);	
				endpoint.appendChild(uri);
			}else {
			    uri.setTextContent(confUriSplitted[0]);
			    endpoint.appendChild(uri);
			    endpoint.appendChild(options);
			    
			    String[] confOptions = confUriSplitted[1].split(",");
			    
			    for(String confOption : confOptions) {
			    	String[] confOptionSplitted = confOption.split("=");

			    	if(confOptionSplitted.length>1){
			    		Element option = doc.createElement(confOptionSplitted[0]);
					    option.setTextContent(confOptionSplitted[1]);	
			    		options.appendChild(option);
			    	}
			    }
			}
			
		    if(confServiceId!=null) {
			    serviceid.setTextContent(confServiceId);
		    	endpoint.appendChild(serviceid);
			    setServiceFromConfiguration(confServiceId, type, configuration);
			}

		    if(confHeaderId!=null) {
			    endpoint.appendChild(headerid);
			    headerid.setTextContent(confHeaderId);
			    setHeaderFromConfiguration(confHeaderId, type, configuration);
			}
		}
	}

	private void setServiceFromConfiguration(String serviceid, String type, TreeMap<String, String> configuration) throws Exception {

	    Element service = doc.createElement("service");
	    services.appendChild(service);

		for(Map.Entry<String,String> entry : configuration.entrySet()) {
			  String key = entry.getKey();
			  String parameterValue = entry.getValue();
			  
			  if(key.startsWith(type + ".service") && parameterValue!=null) {
				  String parameterName = key.substring(key.lastIndexOf(".service.") + 9);				  
				  Element serviceParameter = doc.createElement(parameterName);
				  serviceParameter.setTextContent(parameterValue);
				  service.appendChild(serviceParameter);
			  }
		 }
	}
	
	private void setHeaderFromConfiguration(String headerid, String type, TreeMap<String, String> configuration) throws Exception {

	    Element header = doc.createElement("header");
	    Element id = doc.createElement("id");
	    
	    headers.appendChild(header);
	    id.appendChild(doc.createTextNode(headerid));
	    header.appendChild(id);

		for(Map.Entry<String,String> entry : configuration.entrySet()) {
		  String key = entry.getKey();
		  String parameterValue = entry.getValue();
		  
		  if(key.startsWith(type + ".header") && parameterValue!=null) {
			  String parameterName = key.substring(key.lastIndexOf(".header.") + 8);			  
			  Element headerParameter = doc.createElement(parameterName);
			  headerParameter.setTextContent(parameterValue);
			  header.appendChild(headerParameter);
		  }
		}
	}	
}