package org.assimbly.connector.configuration.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.ConnectorUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
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

	private String connectorXPath;
	private String endpointXPath;
	private String serviceXPath;
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
		getGeneralPropertiesFromXMLFile();

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

	private void getGeneralPropertiesFromXMLFile() throws Exception{

		XPath xPath = XPathFactory.newInstance().newXPath();
		flowId = xPath.evaluate("//flows/flow[id='" + flowId + "']/id",doc);
		flowName = xPath.evaluate("//flows/flow[id='" + flowId + "']/name",doc);
		flowType = xPath.evaluate("//flows/flow[id='" + flowId + "']/type",doc);

		flowOffloading = xPath.evaluate("//flows/flow[id='" + flowId + "']/offloading",doc);
		flowMaximumRedeliveries = xPath.evaluate("//flows/flow[id='" + flowId + "']/maximumRedeliveries",doc);
		flowRedeliveryDelay = xPath.evaluate("//flows/flow[id='" + flowId + "']/redeliveryDelay",doc);
		flowLogLevel = xPath.evaluate("//flows/flow[id='" + flowId + "']/logLevel",doc);
		flowAssimblyHeaders = xPath.evaluate("//flows/flow[id='" + flowId + "']/assimblyHeaders",doc);
		flowParallelProcessing = xPath.evaluate("//flows/flow[id='" + flowId + "']/parallelProcessing",doc);


		if(flowId==null || flowId.isEmpty()) {
			ConfigurationException configurationException = new ConfigurationException("The flow ID doesn't exists in XML Configuration");
			configurationException.initCause(new Throwable("The flow ID doesn't exists in XML Configuration"));
			throw configurationException;
		}

		connectorXPath = "connectors/connector/flows/flow[id='" + flowId + "']";

		String[] connectorProporties = conf.getStringArray(connectorXPath);

		if(connectorProporties.length > 0){
			for(String connectorProperty : connectorProporties){
				properties.put(connectorProperty.substring(connectorXPath.length() + 1), conf.getString(connectorProperty));
			}
		}

		String componentsXpath = "connector/flows/flow[id='" + flowId + "']/components/component";

		String[] components = conf.getStringArray(componentsXpath);

		for(String component : components){
			if(flowComponents==null){
				flowComponents = component;
			}else{
				flowComponents = flowComponents + "," + component;
			}
		}


	/*
		List<String> componentsProperties2 = ConnectorUtil.getXMLParameters(conf, "connector/flows/flow[id='" + flowId + "']/components/component");
		System.out.println("componentsProperties2Length=" + componentsProperties2.size());

		List<String> componentsProperties = ConnectorUtil.getXMLParameters(conf, "connector/flows/flow[id='" + flowId + "']/components");
		System.out.println("componentsPropertiesLength=" + componentsProperties.size());
		for(String componentProperty : componentsProperties){
			System.out.println("componentProperty=" + componentProperty);

		}


	 */

		//set up defaults settings if null -->
		if(flowId == null){
			flowId = "flow" + System.currentTimeMillis();
		}

		if(flowType == null){
			flowType = "default";
		}

		if(flowOffloading == null){
			flowOffloading = "false";
		}

		if(flowMaximumRedeliveries == null){
			flowMaximumRedeliveries = "false";
		}

		if(flowRedeliveryDelay == null){
			flowRedeliveryDelay = "false";
		}

		if(flowLogLevel == null){
			flowLogLevel = "OFF";
		}

		if(flowAssimblyHeaders == null){
			flowAssimblyHeaders = "false";
		}

		if(flowParallelProcessing == null){
			flowParallelProcessing = "false";
		}


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

		offloadingId = conf.getString("connector/offloading/id");

		if (offloadingId != null) {

			options = "";

			wireTapUri = conf.getString("connector/offloading/uri");

			List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, "connector/offloading/options");
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

			serviceId = conf.getString("connector/offloading/service_id");

			if (serviceId != null) {
				getServiceFromXMLFile("to", "0", serviceId);
			}

			headerId = conf.getString("connector/offloading/header_id");

			if (headerId != null) {
				getHeaderFromXMLFile("to","0", headerId);
			}
		}
	}

	private void getEndpointsFromXMLFile() throws Exception {

		String endpointsXpath = "//flows/flow[id='" + flowId + "']/endpoints/endpoint/uri";

		String[] endpoints = conf.getStringArray(endpointsXpath);

		String offrampUri = "";

		int index = 1;

		//A maximum of 3 from components per route is allowed
		int maxFromTypes = 3;

		for(String endpoint : endpoints){

			endpointXPath = "connector/flows/flow[id='" + flowId + "']/endpoints/endpoint[" + index + "]/";
			options = "";
			String type = conf.getString(endpointXPath + "type");

			List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, endpointXPath + "options");
			for(String optionProperty : optionProperties){
				options += optionProperty.split("options.")[1] + "=" + conf.getProperty(optionProperty) + "&";
			}

			if(options.isEmpty()){
				uri = endpoint;
			}else{
				options = options.substring(0,options.length() -1);
				uri = endpoint + "?" + options;

			}

			endpointId = conf.getString(endpointXPath + "id");

			properties.put(type + "." + endpointId + ".uri", uri);

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
				if(index >= maxFromTypes){
					break;
				}
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

		serviceXPath = "connector/services/service[id='" + serviceId + "']/keys";
		List<String> serviceProporties = ConnectorUtil.getXMLParameters(conf, serviceXPath);

		if(!serviceProporties.isEmpty()){

			for(String serviceProperty : serviceProporties){
				properties.put("service." + serviceId + "." + serviceProperty.substring(serviceXPath.length() + 1).toLowerCase(), conf.getString(serviceProperty));
			}

			properties.put(type + "." + endpointId + ".service.id", serviceId);

			String serviceName = conf.getString("connector/services/service[id='" + serviceId + "']/name");
			if(!serviceName.isEmpty()) {
				properties.put(type + "." + endpointId + ".service.name", serviceName);
			}

		}
	}

	private void getHeaderFromXMLFile(String type, String endpointId, String headerId) throws ConfigurationException {

		headerXPath = "connector/headers/header[id='" + headerId + "']/keys";
		List<String> headerProporties = ConnectorUtil.getXMLParameters(conf, headerXPath);

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


			String headerName = conf.getString("connector/headers/header[id='" + headerId + "']/name");
			if(!headerName.isEmpty()) {
				properties.put("header." + headerId + ".name", headerName);
			}
		}
	}

	private void getRouteFromXMLFile(String type, String endpointId, String routeId) throws Exception {

		Document doc = conf.getDocument();

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/connectors/connector/routes/route[@id='" + routeId + "']");
		Node node = (Node)expr.evaluate(doc, XPathConstants.NODE);

		String routeAsString = convertNodeToString(node);

		String updatedRouteId = flowId + "-" + endpointId + "-" + routeId;
		routeAsString = StringUtils.replace(routeAsString, "id=\"" + routeId + "\"", "id=\"" + updatedRouteId + "\"");

		if(!flowType.equalsIgnoreCase("xml")){
			routeAsString = routeAsString.replaceAll("from uri=\"(.*)\"", "from uri=\"direct:flow=" + flowId + "route=" + updatedRouteId + "\"");
		}

		routeAsString = "<routes xmlns=\"http://camel.apache.org/schema/spring\">" + routeAsString + "</routes>";

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