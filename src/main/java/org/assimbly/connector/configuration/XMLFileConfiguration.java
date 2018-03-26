package org.assimbly.connector.configuration;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
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
import org.w3c.dom.NodeList;

public class XMLFileConfiguration {
	
	private TreeMap<String, String> properties;
	private List<TreeMap<String, String>> propertiesList;

	private String flowId;
	
	private XMLConfiguration conf;

	private String serviceId;
	private String serviceXPath;
	private String connectorXPath;
	
	private String uri;
	private String component;
	private String options;
	private String headerId;
	private Element rootElement;
	private Document doc;
	private Element flows;
	private Element services;
	private Element headers;
	private Element flow;
	private String xmlFlowConfiguration;
	

	public List<TreeMap<String, String>> getConfiguration(String connectorId, String xml) throws Exception {
		
		propertiesList = new ArrayList<>();
		Document doc = ConnectorUtil.convertStringToDoc(xml);

		List<String> flowIds = getFlowIds(connectorId,doc); 
 		
  	   for(String flowId : flowIds){
  		 
	  		TreeMap<String, String> flowConfiguration = getFlowConfiguration(flowId, xml);
	  		 
	  		if(flowConfiguration!=null) {
	  	  		 propertiesList.add(flowConfiguration);
	  		}
  	   }
  	   
	   return propertiesList;
	
	}

	public List<TreeMap<String, String>> getConfiguration(String connectorId, URI uri) throws Exception {

		propertiesList = new ArrayList<>();
		Document doc = ConnectorUtil.convertUriToDoc(uri);

		List<String> flowIds = getFlowIds(connectorId,doc); 
 		
  	   for(String flowId : flowIds){
  		   TreeMap<String, String> flowConfiguration = getFlowConfiguration(flowId, uri);
  		 
  		if(flowConfiguration!=null) {
  	  		 propertiesList.add(flowConfiguration);
  		}
  	   }
  	   
	   return propertiesList;
	
	}
	
	
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, String xml) throws Exception {

	   this.flowId = flowId;

	   conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()).getConfiguration();
	   FileHandler fh = new FileHandler(conf);
	   fh.load(ConnectorUtil.convertStringToStream(xml));

  	   setProperties();

	   return properties;
	
	}
	
	public TreeMap<String, String> getFlowConfiguration(String flowId, URI uri) throws Exception {

	   this.flowId = flowId;

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

	public String createConfiguration(String connectorId, List<TreeMap<String, String>> configurations) throws Exception {

	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	    doc = docBuilder.newDocument();
		
	    setGeneralProperties(connectorId);

		for (TreeMap<String, String> configuration : configurations) {
		    setFlowFromConfiguration(configuration);
		}

	    String xmlConfiguration = ConnectorUtil.convertDocToString(doc);
	    
		return xmlConfiguration;

	}
	

	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

	    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
	    DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
	    doc = docBuilder.newDocument();
		
	    setGeneralProperties("live");
	    
	    setFlowFromConfiguration(configuration);
	    
	    if(doc!=null) {
		    xmlFlowConfiguration = ConnectorUtil.convertDocToString(doc);
	    }else {
	    	xmlFlowConfiguration = "Can't create XML File";
	    }
	    
		return xmlFlowConfiguration;
	}
	

	//--> private get/set methods
	
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
			if(flowId == null){    			
				flowId = "flow" + System.currentTimeMillis();					
			}
			properties.put("id",flowId);	
		   
		   	if(properties.get("from.uri") != null){
				properties.put("flow","default");
			}else{
				properties.put("flow", "none");
			}

			if(properties.get("to.uri") == null){
				properties.put("to.uri","mock:wastebin");		
			}
	   	   	   
			properties.put("header.contenttype", "text/xml;charset=UTF-8");
		
	}

	private void getGeneralPropertiesFromXMLFile() throws Exception{

		Document doc = conf.getDocument();
		   
	    XPath xPath = XPathFactory.newInstance().newXPath();
	    flowId = xPath.evaluate("//flows/flow[id='" + flowId + "']/id",doc);
		
		if(flowId==null || flowId.isEmpty()) {throw new ConfigurationException("Id (flow) doesn't exists in XML Configuration");}

		connectorXPath = "flows/flow[id='" + flowId + "']";

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
		   component = xPath.evaluate("//flows/flow[id='" + flowId + "']/" + type + "/uri",doc);
		   
		   if(component == null || component.isEmpty()){return;};
		   
		   List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, "flows/flow[id='" + flowId + "']/" + type + "/options");
		   
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
		   	
	    serviceId = conf.getString("flows/flow[id='" + flowId + "']/" + type + "/service_id");
	    if(serviceId == null){return;};
	    
	    properties.put(type + ".service_id", serviceId);
	    
	    serviceXPath = "services/services[id='" + serviceId + "']";
		List<String> serviceProporties = ConnectorUtil.getXMLParameters(conf, serviceXPath);
		
		if(!serviceProporties.isEmpty()){
	  	   for(String serviceProperty : serviceProporties){
	  		   properties.put(type + ".service." + serviceProperty.substring(serviceXPath.length() + 1), conf.getString(serviceProperty));
    	   }
		}
	}
	
	private void getHeaderFromXMLFile(String type) throws ConfigurationException {
	   	
	    headerId = conf.getString("flows/flow[id='" + flowId + "']/" + type + "/header_id");

	    if(headerId == null){return;};
	    
	    properties.put(type + ".header_id", headerId);
	    
	    serviceXPath = "headers/header[id='" + headerId + "']";
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

    private static List<String> getFlowIds(String connectorId, Document doc)  throws Exception {

        // Create XPath object
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        XPathExpression expr = xpath.compile("/connector[id=" + connectorId +"]/flows/flow/id/text()");
    	
        // Create list of Ids
    	List<String> list = new ArrayList<>();
        NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            list.add(nodes.item(i).getNodeValue());
        }
        
        return list;
    }

	
	
	private void setGeneralProperties(String connectorId) {
		
	    rootElement = doc.createElement("connector");
	    doc.appendChild(rootElement);

	    Element id = doc.createElement("id");
	    id.appendChild(doc.createTextNode(connectorId));
	    rootElement.appendChild(id);
	    
	    flows = doc.createElement("flows");
	    services = doc.createElement("services");
	    headers = doc.createElement("headers");
	    
	    rootElement.appendChild(flows);
	    rootElement.appendChild(services);
	    rootElement.appendChild(headers);
		
	}
	
	
	private void setFlowFromConfiguration(TreeMap<String, String> configuration) throws Exception {

	    flow = doc.createElement("flow");
	    flows.appendChild(flow);
	    
	    //set id
	    String flowId = configuration.get("id");	    
	    Element id = doc.createElement("id");
	    id.appendChild(doc.createTextNode(flowId));
	    flow.appendChild(id);

	    //set id
	    String flowType = configuration.get("flow");	    
	    Element flowTypeNode = doc.createElement("flow");
	    flowTypeNode.appendChild(doc.createTextNode(flowType));
	    flow.appendChild(flowTypeNode);	    
	    
	    //set endpoints
	    setFlowEndpoint("from",configuration);
	    setFlowEndpoint("to",configuration);
	    setFlowEndpoint("error",configuration);
	    
	}
	
	private void setFlowEndpoint(String type, TreeMap<String, String> configuration) throws Exception {

		String confUri = configuration.get(type + ".uri");
		String confServiceId = configuration.get(type + ".service.id");
		String confHeaderId = configuration.get(type + ".header.id");

	    Element endpoint = doc.createElement(type);
	    Element uri = doc.createElement("uri");
	    Element options = doc.createElement("options");
	    Element serviceid = doc.createElement("service_id");
	    Element headerid = doc.createElement("header_id");

		if(confUri!=null) {

			flow.appendChild(endpoint);

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