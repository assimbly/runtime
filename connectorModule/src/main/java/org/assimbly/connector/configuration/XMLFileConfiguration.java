package org.assimbly.connector.configuration;

import java.io.File;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
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
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.util.ConnectorUtil;
import org.assimbly.docconverter.DocConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XMLFileConfiguration {

	private TreeMap<String, String> properties;
	private List<TreeMap<String, String>> propertiesList;

	private String flowId;

	private XMLConfiguration conf;

	private String serviceId;
	private String connectorXPath;
	private String endpointXPath;
	private String serviceXPath;
	private String headerXPath;

	private Document doc;
	private Element rootElement;
	private Element flows;
	private Element services;
	private Element headers;
	private Element flow;
	private Element connector;
	private String xmlFlowConfiguration;

	private List<String> servicesList;
	private List<String> headersList;

	private String uri;
	private String options;
	private String headerId;

	private String flowName;
	private String flowType;
	private String flowComponents;
	private String flowOffloading;
	private String flowMaximumRedeliveries;
	private String flowRedeliveryDelay;
	private String flowLogLevel;
	private Object offloadingId;
	private String wireTapUri;
	private String endpointId;
	private String responseId;
	private String routeId;
	private Element endpoints;


	public List<TreeMap<String, String>> getConfiguration(String connectorId, String xml) throws Exception {

		propertiesList = new ArrayList<>();
		Document doc = DocConverter.convertStringToDoc(xml);

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
		Document doc = DocConverter.convertUriToDoc(uri);

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

		DocumentBuilder docBuilder = setDocumentBuilder("connector.xsd");

		conf = new BasicConfigurationBuilder<>(XMLConfiguration.class).configure(new Parameters().xml()
				.setFileName("connector.xml")
				.setDocumentBuilder(docBuilder)
				.setSchemaValidation(true)
				.setExpressionEngine(new XPathExpressionEngine())
		).getConfiguration();

		FileHandler fh = new FileHandler(conf);
		fh.load(DocConverter.convertStringToStream(xml));

		setProperties();

		ConnectorUtil.printTreemap(properties);

		return properties;

	}

	public TreeMap<String, String> getFlowConfiguration(String flowId, URI uri) throws Exception {

		this.flowId = flowId;

		String scheme = uri.getScheme();
		//load uri to configuration
		Parameters params = new Parameters();

		DocumentBuilder docBuilder = setDocumentBuilder("connector.xsd");

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
									.setFileName("connector.xml")
									.setFile(xml)
									.setDocumentBuilder(docBuilder)
									.setSchemaValidation(true)
									.setExpressionEngine(new XPathExpressionEngine())
							);

			// This will throw a ConfigurationException if the XML document does not
			// conform to its Schema.
			conf = builder.getConfiguration();

		}else if (scheme.startsWith("http")) {

			URL Url = uri.toURL();

			FileBasedConfigurationBuilder<XMLConfiguration> builder =
					new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
							.configure(params.xml()
									.setURL(Url)
									.setFileName("connector.xml")
									.setDocumentBuilder(docBuilder)
									.setSchemaValidation(true)
									.setExpressionEngine(new XPathExpressionEngine())
							);

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

		if(configurations == null || configurations.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}else {
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			doc = docBuilder.newDocument();

			setGeneralProperties(connectorId);

			for (TreeMap<String, String> configuration : configurations) {
				setFlowFromConfiguration(configuration);
			}

			String xmlConfiguration = DocConverter.convertDocToString(doc);

			return xmlConfiguration;

		}

	}


	public String createFlowConfiguration(TreeMap<String, String> configuration) throws Exception {

		if(configuration == null || configuration.isEmpty()) {
			return "Error: Empty configuration (no configuration is set/running)";
		}

		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
		doc = docBuilder.newDocument();

		setGeneralProperties("live");

		setFlowFromConfiguration(configuration);

		if(doc!=null) {
			xmlFlowConfiguration = DocConverter.convertDocToString(doc);
		}else {
			xmlFlowConfiguration = "Error: Can't create configuration";
		}

		return xmlFlowConfiguration;
	}


	//--> private get/set methods

	private void setProperties() throws Exception{

		//create a Treemap for the configuration
		properties = new TreeMap<String, String>();

		//set general flow properties
		getGeneralPropertiesFromXMLFile();

		//set endpoint properties
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

	}

	private void getGeneralPropertiesFromXMLFile() throws Exception{

		Document doc = conf.getDocument();

		XPath xPath = XPathFactory.newInstance().newXPath();
		flowId = xPath.evaluate("//flows/flow[id='" + flowId + "']/id",doc);
		flowName = xPath.evaluate("//flows/flow[id='" + flowId + "']/name",doc);
		flowType = xPath.evaluate("//flows/flow[id='" + flowId + "']/type",doc);

		flowOffloading = xPath.evaluate("//flows/flow[id='" + flowId + "']/offloading",doc);
		flowMaximumRedeliveries = xPath.evaluate("//flows/flow[id='" + flowId + "']/maximumRedeliveries",doc);
		flowRedeliveryDelay = xPath.evaluate("//flows/flow[id='" + flowId + "']/redeliveryDelay",doc);
		flowLogLevel = xPath.evaluate("//flows/flow[id='" + flowId + "']/logLevel",doc);


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

		List<String> componentsProperties = ConnectorUtil.getXMLParameters(conf, "connector/flows/flow[id='" + flowId + "']/components");
		for(String componentProperty : componentsProperties){
			if(flowComponents==null){
				flowComponents = conf.getString(componentProperty);
			}else{
				flowComponents = flowComponents + "," + conf.getString(componentProperty);
			}
		}

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

		properties.put("id",flowId);
		properties.put("flow.name",flowName);
		properties.put("flow.type",flowType);

		properties.put("flow.components",flowComponents);

		properties.put("flow.offloading",flowOffloading);
		properties.put("flow.maximumRedeliveries",flowMaximumRedeliveries);
		properties.put("flow.redeliveryDelay",flowRedeliveryDelay);
		properties.put("flow.logLevel",flowLogLevel);

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

		String componentsXpath = "//flows/flow[id='" + flowId + "']/endpoints/endpoint/uri";

		String[] components = conf.getStringArray(componentsXpath);

		String offrampUri = "";

		int index = 1;

		//A maximum of 3 from components per route is allowed
		int maxFromTypes = 3;

		for(String component : components){

			endpointXPath = "connector/flows/flow[id='" + flowId + "']/endpoints/endpoint[" + index + "]/";
			options = "";
			String type = conf.getString(endpointXPath + "type");

			List<String> optionProperties = ConnectorUtil.getXMLParameters(conf, endpointXPath + "options");
			for(String optionProperty : optionProperties){
				options += optionProperty.split("options.")[1] + "=" + conf.getProperty(optionProperty) + "&";
			}

			if(options.isEmpty()){
				uri = component;
			}else{
				options = options.substring(0,options.length() -1);
				uri = component + "?" + options;

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


	private static List<String> getFlowIds(String connectorId, Document doc)  throws Exception {

		// Create XPath object
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		XPathExpression expr = xpath.compile("/connectors/connector[id=" + connectorId +"]/flows/flow/id/text()");

		// Create list of Ids
		List<String> list = new ArrayList<>();
		NodeList nodes = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			list.add(nodes.item(i).getNodeValue());
		}

		return list;
	}



	private void setGeneralProperties(String connectorId) {

		rootElement = doc.createElement("connectors");
		doc.appendChild(rootElement);

		connector = doc.createElement("connector");
		rootElement.appendChild(connector);

		Element id = doc.createElement("id");
		id.appendChild(doc.createTextNode(connectorId));
		connector.appendChild(id);

		flows = doc.createElement("flows");
		services = doc.createElement("services");
		headers = doc.createElement("headers");

		connector.appendChild(flows);
		connector.appendChild(services);
		connector.appendChild(headers);

		//List to ensure no double entries
		servicesList = new ArrayList<String>();
		headersList = new ArrayList<String>();

	}


	private void setFlowFromConfiguration(TreeMap<String, String> configuration) throws Exception {

		flow = doc.createElement("flow");
		flows.appendChild(flow);

		//set id
		String flowId = configuration.get("id");
		Element id = doc.createElement("id");
		id.appendChild(doc.createTextNode(flowId));
		flow.appendChild(id);

		//set type
		String flowType = configuration.get("flow.type");
		Element flowTypeNode = doc.createElement("type");
		flowTypeNode.appendChild(doc.createTextNode(flowType));
		flow.appendChild(flowTypeNode);

		//set name
		String flowName = configuration.get("flow.name");
		Element flowNameNode = doc.createElement("name");
		flowNameNode.appendChild(doc.createTextNode(flowName));
		flow.appendChild(flowNameNode);

		//set offloading
		String flowOffloading = configuration.get("flow.offloading");
		Element flowOffloadingNode = doc.createElement("offloading");
		flowOffloadingNode.appendChild(doc.createTextNode(flowOffloading));
		flow.appendChild(flowOffloadingNode);

		//set offloading
		String flowMaximumRedeliveries = configuration.get("flow.maximumRedeliveries");
		Element flowMaximumRedeliveriesNode = doc.createElement("maximumRedeliveries");
		flowMaximumRedeliveriesNode.appendChild(doc.createTextNode(flowMaximumRedeliveries));
		flow.appendChild(flowMaximumRedeliveriesNode);

		//set offloading
		String flowRedeliveryDelay = configuration.get("flow.redeliveryDelay");
		Element flowRedeliveryDelayNode = doc.createElement("redeliveryDelay");
		flowRedeliveryDelayNode.appendChild(doc.createTextNode(flowRedeliveryDelay));
		flow.appendChild(flowRedeliveryDelayNode);

		//set offloading
		String flowLogLevel = configuration.get("flow.logLevel");
		Element flowLogLevelNode = doc.createElement("logLevel");
		flowLogLevelNode.appendChild(doc.createTextNode(flowLogLevel));
		flow.appendChild(flowLogLevelNode);

		Element components = doc.createElement("components");
		flow.appendChild(components);

		String[] confComponentsSplitted = StringUtils.split(configuration.get("flow.components"),",");
		for(String confComponentSplitted : confComponentsSplitted){
			Element flowComponentNode = doc.createElement("component");
			flowComponentNode.appendChild(doc.createTextNode(confComponentSplitted));
			components.appendChild(flowComponentNode);
		}

		endpoints = doc.createElement("endpoints");
		flow.appendChild(endpoints);

		//set endpoints
		setFlowEndpoints(configuration);

	}

	private void setFlowEndpoints(TreeMap<String, String> configuration) throws Exception {

		List<String> confUriKeyList = configuration.keySet().stream().filter(k -> k.endsWith("uri")).collect(Collectors.toList());

			for(String confUriKey : confUriKeyList){
				String confUri = configuration.get(confUriKey);
				String[] confUriKeySplitted = StringUtils.split(confUriKey,".");
				String confType = confUriKeySplitted[0];
				String confEndpointId = confUriKeySplitted[1];

				String confServiceId = configuration.get(confType + "." + confEndpointId + ".service.id");
				String confHeaderId = configuration.get(confType + "." + confEndpointId + ".header.id");
				String confRouteId = configuration.get(confType + "." + confEndpointId + ".route.id");

				setEndpointFromConfiguration(confType, confUri, confEndpointId, confServiceId, confHeaderId, confRouteId, confRouteId, configuration);
			}
	}

	private void setEndpointFromConfiguration(String confType, String confUri, String confEndpointId, String confServiceId, String confHeaderId, String confResponseId,String confRouteId, TreeMap<String, String> configuration) throws Exception {

		Element endpoint = doc.createElement("endpoint");
		Element uri = doc.createElement("uri");
		Element type = doc.createElement("type");
		Element endpointId = doc.createElement("id");
		Element options = doc.createElement("options");
		Element serviceid = doc.createElement("service_id");
		Element headerid = doc.createElement("header_id");
		Element responseId = doc.createElement("response_id");
		Element routeId = doc.createElement("route_id");

		endpoints.appendChild(endpoint);

		String[] confUriSplitted = confUri.split("\\?");

		if(confUriSplitted.length<=1) {
			if(confUri.startsWith("sonicmq")) {
				confUri = confUri.replaceFirst("sonicmq.*:", "sonicmq:");
			}

			endpointId.setTextContent(confEndpointId);
			type.setTextContent(confType);
			uri.setTextContent(confUri);

			endpoint.appendChild(endpointId);
			endpoint.appendChild(type);
			endpoint.appendChild(uri);

		}else {
			if(confUriSplitted[0].startsWith("sonicmq")) {
				confUriSplitted[0] = confUriSplitted[0].replaceFirst("sonicmq.*:", "sonicmq:");
			}
			endpoint.setTextContent(confEndpointId);
			uri.setTextContent(confUri);
			type.setTextContent(confType);

			endpoint.appendChild(endpointId);
			endpoint.appendChild(type);
			endpoint.appendChild(uri);
			endpoint.appendChild(options);

			String[] confOptions = confUriSplitted[1].split("&");

			for(String confOption : confOptions) {
				String[] confOptionSplitted = confOption.split("=");

				if(confOptionSplitted.length>1){
					Element option = doc.createElement(confOptionSplitted[0]);
					option.setTextContent(confOptionSplitted[1]);
					options.appendChild(option);
				}
			}
		}

		if(confResponseId != null) {
			responseId.setTextContent(confResponseId);
			endpoint.appendChild(responseId);
		}

		if(confRouteId != null) {
			responseId.setTextContent(confRouteId);
			endpoint.appendChild(routeId);
		}

		if(confServiceId!=null) {
			serviceid.setTextContent(confServiceId);
			endpoint.appendChild(serviceid);
			setServiceFromConfiguration(confServiceId, confType, configuration);
		}

		if(confHeaderId!=null) {
			endpoint.appendChild(headerid);
			headerid.setTextContent(confHeaderId);
			setHeaderFromConfiguration(confHeaderId, confType, configuration);
		}

	}


	private void setServiceFromConfiguration(String serviceid, String type, TreeMap<String, String> configuration) throws Exception {

		if(!servicesList.contains(serviceid)) {
			servicesList.add(serviceid);

			Element service = doc.createElement("service");
			services.appendChild(service);

			Element serviceIdParameter = doc.createElement("id");
			serviceIdParameter.setTextContent(serviceid);
			service.appendChild(serviceIdParameter);

			for(Map.Entry<String,String> entry : configuration.entrySet()) {
				String key = entry.getKey();
				String parameterValue = entry.getValue();

				if(key.startsWith("service." + serviceid) && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "service." + serviceid + ".");
					Element serviceParameter = doc.createElement(parameterName);
					serviceParameter.setTextContent(parameterValue);
					service.appendChild(serviceParameter);
				}
			}
		}
	}

	private void setHeaderFromConfiguration(String headerid, String type, TreeMap<String, String> configuration) throws Exception {

		if(!headersList.contains(headerid)) {

			headersList.add(headerid);

			Element header = doc.createElement("header");
			headers.appendChild(header);

			Element headerIdParameter = doc.createElement("id");
			headerIdParameter.setTextContent(headerid);
			header.appendChild(headerIdParameter);

			for(Map.Entry<String,String> entry : configuration.entrySet()) {
				String key = entry.getKey();
				String parameterValue = entry.getValue();

				if(key.startsWith("header." + headerid + ".xpath") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "xpath.");
					Element headerParameter = doc.createElement(parameterName);
					headerParameter.setTextContent(parameterValue);
					headerParameter.setAttribute("type", "xpath");
					header.appendChild(headerParameter);
				}else if(key.startsWith("header." + headerid +  ".constant") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "constant.");
					Element headerParameter = doc.createElement(parameterName);
					headerParameter.setTextContent(parameterValue);
					headerParameter.setAttribute("type", "constant");
					header.appendChild(headerParameter);
				}else if(key.startsWith("header." + headerid +  ".simple") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "simple.");
					Element headerParameter = doc.createElement(parameterName);
					headerParameter.setTextContent(parameterValue);
					headerParameter.setAttribute("type", "simple");
					header.appendChild(headerParameter);
				}else if(key.startsWith("header." + headerid) && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "header." + headerid + ".");
					Element headerParameter = doc.createElement(parameterName);
					headerParameter.setTextContent(parameterValue);
					header.appendChild(headerParameter);
				}
			}
		}
	}

	private DocumentBuilder setDocumentBuilder(String schemaFilename) throws SAXException, ParserConfigurationException {

		URL schemaUrl = this.getClass().getResource("/" + schemaFilename);
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaUrl);

		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setSchema(schema);

		DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
		//if you want an exception to be thrown when there is invalid xml document,
		//you need to set your own ErrorHandler because the default
		//behavior is to just print an error message.
		docBuilder.setErrorHandler(new ErrorHandler() {
			@Override
			public void warning(SAXParseException exception) throws SAXException {
				throw exception;
			}

			@Override
			public void error(SAXParseException exception) throws SAXException {
				throw exception;
			}

			@Override
			public void fatalError(SAXParseException exception)  throws SAXException {
				throw exception;
			}
		});

		return docBuilder;
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