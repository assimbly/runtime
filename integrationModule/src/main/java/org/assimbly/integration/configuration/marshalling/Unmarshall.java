package org.assimbly.integration.configuration.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

//This class unmarshalls (converts) a XML file to a Java treemap object
public class Unmarshall {

	private TreeMap<String, String> properties;

	private XMLConfiguration conf;

	Document doc;

	private String integrationXPath;
	private String endpointXPath;
	private String headerXPath;

	private String uri;
	private String options;
	private String headerId;
	private String serviceId;

	private String flowId;
	private String flowName;
	private String flowType;
	private String flowComponents;
	private String flowOffloading;
	private String flowMaximumRedeliveries;
	private String flowRedeliveryDelay;
	private String flowLogLevel;
	private String flowAssimblyHeaders;
	private String flowParallelProcessing;
	private Object offloadingId;
	private String wireTapUri;
	private String endpointId;
	private String responseId;
	private String routeId;

	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		//create a Treemap for the configuration
		properties = new TreeMap<String, String>();

		//get the XML as document
		doc = configuration.getDocument();
		conf = configuration;
		this.flowId = flowId;

		//get general flow properties
		getFlowsFromXMLFile();

		//get endpoint properties
		getEndpointsFromXMLFile();
					
		Set<String> fromUriSet = properties.keySet().stream().filter(s -> s.startsWith("from.") && s.endsWith(".uri")).collect(Collectors.toSet());
		
		if(flowType.isEmpty() && !fromUriSet.isEmpty()){
			properties.put("flow.type","default");
		}else if (flowType.isEmpty()){
			properties.put("flow.type", "none");
		}

		if(properties.get("flow.offloading").equals("true")){
			getOffloadingfromXMLFile();
		}

		return properties;

	}

	private void getFlowsFromXMLFile() throws Exception{

		XPath xPath = XPathFactory.newInstance().newXPath();
		
		String flowSelector = setFlowSelector();
		
		flowName = xPath.evaluate("//flows/flow[" + flowSelector + "]/name",doc);
		flowType = xPath.evaluate("//flows/flow[" + flowSelector + "]/type",doc);

		flowOffloading = xPath.evaluate("//flows/flow[" + flowSelector + "]/offloading",doc);
		flowMaximumRedeliveries = xPath.evaluate("//flows/flow[" + flowSelector + "]/maximumRedeliveries",doc);
		flowRedeliveryDelay = xPath.evaluate("//flows/flow[" + flowSelector + "]/redeliveryDelay",doc);
		flowLogLevel = xPath.evaluate("//flows/flow[" + flowSelector + "]/logLevel",doc);
		flowAssimblyHeaders = xPath.evaluate("//flows/flow[" + flowSelector + "]/assimblyHeaders",doc);
		flowParallelProcessing = xPath.evaluate("//flows/flow[" + flowSelector + "]/parallelProcessing",doc);

		integrationXPath = "integrations/integration/flows/flow[" + flowSelector + "]";

		String[] integrationProporties = conf.getStringArray(integrationXPath);

		if(integrationProporties.length > 0){
			for(String integrationProperty : integrationProporties){
				properties.put(integrationProperty.substring(integrationXPath.length() + 1), conf.getString(integrationProperty));
			}
		}

		String componentsXpath = "integration/flows/flow[" + flowSelector + "]/components/component";

		String[] components = conf.getStringArray(componentsXpath);

		for(String component : components){
			if(flowComponents==null){
				flowComponents = component;
			}else{
				flowComponents = flowComponents + "," + component;
			}
		}

		setFlowDefaults();

		setFlowProperties();

	}

	private String setFlowSelector() throws Exception{
		
		XPath xPath = XPathFactory.newInstance().newXPath();
		
		String selector = "1";

		Integer numberOfFlows = Integer.parseInt(xPath.evaluate("count(//flows/flow)",doc));
		
		if(numberOfFlows > 1){
			
			//originalFlowId is the flowId as parameter 
			String originalFlowId = flowId;				
			selector = "id='" + originalFlowId + "'";
			
			flowId = xPath.evaluate("//flows/flow[" + selector + "]/id",doc);
						
			if(!originalFlowId.equals(flowId)) {
				ConfigurationException configurationException = new ConfigurationException("The flow ID " + originalFlowId + " doesn't exists in XML Configuration");
				configurationException.initCause(new Throwable("The flow ID  " + originalFlowId + " doesn't exists in XML Configuration"));
				throw configurationException;
			}
		}else{
			flowId = xPath.evaluate("//flows/flow[" + selector + "]/id",doc);
		}

		return selector;	
		
	}

	//set up defaults settings for a flow if values are null or empty
	public void setFlowDefaults(){
		
		if(flowId == null || flowId.isEmpty()){
			flowId = "flow" + System.currentTimeMillis();
		}

		if(flowType == null || flowType.isEmpty()){
			flowType = "default";
		}

		if(flowOffloading == null || flowOffloading.isEmpty()){
			flowOffloading = "false";
		}

		if(flowMaximumRedeliveries == null || flowMaximumRedeliveries.isEmpty()){
			flowMaximumRedeliveries = "false";
		}

		if(flowRedeliveryDelay == null || flowRedeliveryDelay.isEmpty()){
			flowRedeliveryDelay = "false";
		}

		if(flowLogLevel == null || flowLogLevel.isEmpty()){
			flowLogLevel = "OFF";
		}

		if(flowAssimblyHeaders == null || flowAssimblyHeaders.isEmpty()){
			flowAssimblyHeaders = "false";
		}

		if(flowParallelProcessing == null || flowParallelProcessing.isEmpty()){
			flowParallelProcessing = "false";
		}
		
	}

	public void setFlowProperties(){

		properties.put("id",flowId);
		properties.put("flow.name",flowName);
		properties.put("flow.type",flowType);

		properties.put("flow.components",flowComponents);

		properties.put("flow.offloading",flowOffloading);
		properties.put("flow.maximumRedeliveries",flowMaximumRedeliveries);
		properties.put("flow.redeliveryDelay",flowRedeliveryDelay);
		properties.put("flow.logLevel",flowLogLevel);
		properties.put("flow.assimblyHeaders",flowAssimblyHeaders);
		properties.put("flow.parallelProcessing",flowParallelProcessing);

	}
	
	
	private void getOffloadingfromXMLFile() throws Exception {

		offloadingId = conf.getString("integration/offloading/id");

		if (offloadingId != null) {

			options = "";

			wireTapUri = conf.getString("integration/offloading/uri");

			List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integration/offloading/options");
			for (String optionProperty : optionProperties) {
				options += optionProperty.split("options.")[1] + "=" + conf.getProperty(optionProperty) + "&";
			}

			if (options.isEmpty()) {
				uri = wireTapUri;
			} else {
				options = options.substring(0, options.length() - 1);
				uri = wireTapUri + "?" + options;
			}

			properties.put("wiretap.uri",uri);
			properties.put("to.0.uri",uri);
			properties.put("offramp.uri.list",properties.get("offramp.uri.list") + ",direct:offloadingendpoint=0");

			serviceId = conf.getString("integration/offloading/service_id");

			if (serviceId != null) {
				getServiceFromXMLFile("to", "0", serviceId);
			}

			headerId = conf.getString("integration/offloading/header_id");

			if (headerId != null) {
				getHeaderFromXMLFile("to","0", headerId);
			}
		}
	}

	private void getEndpointsFromXMLFile() throws Exception {

		String endpointsXpath = "//flows/flow[id='" + flowId + "']/endpoints/endpoint/id";

		String[] endpoints = conf.getStringArray(endpointsXpath);

		String offrampUri = "";

		int index = 1;

		//A maximum of 3 from components per route is allowed
		int maxFromTypes = 3;
		int numFromType = 0;

		for(String endpoint : endpoints){
	
			endpointXPath = "integration/flows/flow[id='" + flowId + "']/endpoints/endpoint[" + index + "]/";
			
			options = "";
			String type = conf.getString(endpointXPath + "type");

			List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, endpointXPath + "options");
			for(String optionProperty : optionProperties){
				options += optionProperty.split("options.")[1] + "=" + conf.getProperty(optionProperty) + "&";
			}

			endpointId = conf.getString(endpointXPath + "id");
		
			if(options.isEmpty()){
				uri = conf.getString(endpointXPath + "uri");
			}else{
				options = options.substring(0,options.length() -1);
				uri = conf.getString(endpointXPath + "uri") + "?" + options;
			}

			if(uri != null){
				properties.put(type + "." + endpointId + ".uri", uri);
			}

			serviceId = conf.getString(endpointXPath + "service_id");

			if(serviceId != null){
				getServiceFromXMLFile(type, endpointId, serviceId);
			}

			headerId = conf.getString(endpointXPath + "header_id");

			if(headerId != null){
				getHeaderFromXMLFile(type,endpointId, headerId);
			}

			routeId = conf.getString(endpointXPath + "route_id");

			if(routeId != null){
				getRouteFromXMLFile(type,endpointId, routeId);
			}

			if (type.equals("response")) {
				responseId = conf.getString(endpointXPath + "response_id");
				if(responseId != null) {
					properties.put(type + "." + endpointId + ".response.id", responseId);
				}
			} else if(type.equals("from")) {
				if(numFromType >= maxFromTypes){
					// maximum from endpoints reached on a route
					// jump to the next iteration
					continue;
				}
				numFromType++;
			} else if(type.equals("to")) {

				if(endpointId != null){
					if(offrampUri.isEmpty()) {
						offrampUri = "direct:flow=" + flowId + "endpoint=" + endpointId;
					}else {
						offrampUri = offrampUri + ",direct:flow=" + flowId + "endpoint=" + endpointId;
					}
				}

				responseId = conf.getString(endpointXPath + "response_id");

				if(responseId != null) {
					properties.put(type + "." + endpointId + ".response.id", responseId);
				}
				properties.put("offramp.uri.list", offrampUri);
			}

			index++;
		}
	}

	private void getServiceFromXMLFile(String type, String endpointId, String serviceId) throws ConfigurationException {

		String serviceXPath = "integration/services/service[id='" + serviceId + "']/";
		List<String> serviceProporties = IntegrationUtil.getXMLParameters(conf, serviceXPath + "keys");

		if(!serviceProporties.isEmpty()){

			for(String serviceProperty : serviceProporties){
				properties.put("service." + serviceId + "." + serviceProperty.substring(serviceXPath.length() + 5).toLowerCase(), conf.getString(serviceProperty));
			}

			properties.put(type + "." + endpointId + ".service.id", serviceId);

			String serviceName = conf.getString(serviceXPath + "name");

			if(!serviceName.isEmpty()) {
				properties.put(type + "." + endpointId + ".service.name", serviceName);
				properties.put("service." + serviceId + ".name", serviceName);
			}

			String serviceType = conf.getString(serviceXPath + "type");
			
			if(!serviceType.isEmpty()) {
				properties.put("service." + serviceId + ".type", serviceType);
			}else{
				properties.put("service." + serviceId + ".type", "unknown");
			}


		}
	}

	private void getHeaderFromXMLFile(String type, String endpointId, String headerId) throws ConfigurationException {

		headerXPath = "integration/headers/header[id='" + headerId + "']/keys";
		List<String> headerProporties = IntegrationUtil.getXMLParameters(conf, headerXPath);

		if(!headerProporties.isEmpty()){
			for(String headerProperty : headerProporties){
				if(!headerProperty.endsWith("type")) {

					String headerKey = headerProperty.substring(headerXPath.length() + 1);
					String headerValue = conf.getProperty(headerProperty).toString();
					String headerType = conf.getString(headerProperty + "/@type");

					if(headerType==null){
						headerType = conf.getString(headerProperty + "/type");
					}
					properties.put("header." + headerId + "." + headerType + "." + headerKey, headerValue);
				}
			}

			properties.put(type + "." + endpointId + ".header.id", headerId);


			String headerName = conf.getString("integration/headers/header[id='" + headerId + "']/name");
			if(!headerName.isEmpty()) {
				properties.put("header." + headerId + ".name", headerName);
			}
		}
	}

	private void getRouteFromXMLFile(String type, String endpointId, String routeId) throws Exception {

		Document doc = conf.getDocument();

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/integrations/integration/routes/route[@id='" + routeId + "']");
		Node node = (Node)expr.evaluate(doc, XPathConstants.NODE);

		XPathExpression expr2 = xpath.compile("/integrations/integration/flows/flow/endpoints/endpoint[type='error']/id");
		String errorId = expr2.evaluate(doc);	
		
		expr2 = xpath.compile("/integrations/integration/flows/flow/endpoints/endpoint[type='error']/route_id");
		String errorRouteId = expr2.evaluate(doc);

		if(node!=null && errorId!=null){
			expr = xpath.compile("/integrations/integration/routeConfigurations/routeConfiguration[@id='" + errorRouteId + "']");
			Node nodeRouteConfiguration = (Node)expr.evaluate(doc, XPathConstants.NODE);
			if(nodeRouteConfiguration != null){
				((Element)node).setAttribute("routeConfigurationId","errorHandler-" + errorId);
			}
		}

		String updatedRouteId = flowId + "-" + endpointId;

		if(node==null){
			expr = xpath.compile("/integrations/integration/routeConfigurations/routeConfiguration[@id='" + routeId + "']");
			node = (Node)expr.evaluate(doc, XPathConstants.NODE);		
			updatedRouteId = "errorHandler" + "-" + endpointId;
		}

		String routeAsString = convertNodeToString(node);

		if(flowType.equalsIgnoreCase("esb")){
			routeAsString = StringUtils.replace(routeAsString, "id=\"" + routeId + "\"", "id=\"" + updatedRouteId + "\"");
		}else{
			updatedRouteId = updatedRouteId + "-" + routeId;
			routeAsString = StringUtils.replace(routeAsString, "id=\"" + routeId + "\"", "id=\"" + updatedRouteId + "\"");

			if(flowType.equalsIgnoreCase("default") || flowType.equalsIgnoreCase("connector")){
				routeAsString = routeAsString.replaceAll("from uri=\"(.*)\"", "from uri=\"direct:flow=" + flowId + "route=" + updatedRouteId + "\"");
			}	
		}

		properties.put(type + "." + endpointId + ".route", routeAsString);
		properties.put(type + "." + endpointId + ".route.id", routeId);

	}


	public String convertNodeToString(Node node) throws TransformerException {
		//Convert node to string
		StreamResult xmlOutput = new StreamResult(new StringWriter());
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(node), xmlOutput);
		String nodeAsAString = xmlOutput.getWriter().toString();

		return nodeAsAString;
	}

}

