package org.assimbly.integration.configuration.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
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
	private String stepXPath;
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
	private String stepId;
	private String responseId;
	private String routeId;
	private String routeConfigurationId;
	private String baseUri;


	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		//create a Treemap for the configuration
		properties = new TreeMap<String, String>();

		//get the XML as document
		doc = configuration.getDocument();
		conf = configuration;
		this.flowId = flowId;

		//get general flow properties
		getFlowsFromXMLFile();

		//get step properties
		getStepsFromXMLFile();
					
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
			properties.put("offramp.uri.list",properties.get("offramp.uri.list") + ",direct:offloadingstep=0");

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

	private void getStepsFromXMLFile() throws Exception {

		String stepsXpath = "//flows/flow[id='" + flowId + "']/steps/step/id";

		String[] steps = conf.getStringArray(stepsXpath);

		int index = 1;

		for(String step : steps){

			getStepFromXMLFile(index);

			index++;
		}
	}

	private void getStepFromXMLFile(int index) throws Exception {

		String offrampUri = "";
		//A maximum of 3 from components per route is allowed
		int maxFromTypes = 3;
		int numFromType = 0;

		stepXPath = "integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/";

		stepId = conf.getString(stepXPath + "id");

		String type = conf.getString(stepXPath + "type");

		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, stepXPath + "options");
		options = createOptions(optionProperties);

		String[] links = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/links/link/id");

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		baseUri = expr.evaluate(doc);

		uri = createUri(baseUri);

		if(uri != null){
			properties.put(type + "." + stepId + ".uri", uri);
		}

		serviceId = conf.getString(stepXPath + "service_id");

		if(serviceId != null){
			getServiceFromXMLFile(type, stepId, serviceId);
		}

		headerId = conf.getString(stepXPath + "header_id");

		if(headerId != null){
			getHeaderFromXMLFile(type,stepId, headerId);
		}

		routeId = conf.getString(stepXPath + "route_id");

		if(routeId != null){
			getRouteFromXMLFile(type,stepId, routeId);
		}

		routeConfigurationId = conf.getString(stepXPath + "routeconfiguration_id");

		if(routeConfigurationId != null){
			getRouteConfigurationFromXMLFile(type,stepId, routeConfigurationId);
		}


		if (type.equals("source") || type.equals("action") || type.equals("router") || type.equals("sink")) {
			getRouteTemplateFromXMLFile(type,flowId, stepId, optionProperties, links);
		} else if (type.equals("response")) {
			responseId = conf.getString(stepXPath + "response_id");
			if(responseId != null) {
				properties.put(type + "." + stepId + ".response.id", responseId);
			}
		} else if(type.equals("from")) {
			if(numFromType >= maxFromTypes){
				// maximum from steps reached on a route
				// jump to the next iteration
			}
			numFromType++;
		} else if(type.equals("to")) {

			if(stepId != null){
				if(offrampUri.isEmpty()) {
					offrampUri = "direct:flow=" + flowId + "step=" + stepId;
				}else {
					offrampUri = offrampUri + ",direct:flow=" + flowId + "step=" + stepId;
				}
			}

			responseId = conf.getString(stepXPath + "response_id");

			if(responseId != null) {
				properties.put(type + "." + stepId + ".response.id", responseId);
			}
			properties.put("offramp.uri.list", offrampUri);
		}


	}

	private void getServiceFromXMLFile(String type, String stepId, String serviceId) throws ConfigurationException {

		String serviceXPath = "integration/services/service[id='" + serviceId + "']/";
		List<String> serviceProporties = IntegrationUtil.getXMLParameters(conf, serviceXPath + "keys");

		if(!serviceProporties.isEmpty()){

			for(String serviceProperty : serviceProporties){
				properties.put("service." + serviceId + "." + serviceProperty.substring(serviceXPath.length() + 5).toLowerCase(), conf.getString(serviceProperty));
			}

			properties.put(type + "." + stepId + ".service.id", serviceId);

			String serviceName = conf.getString(serviceXPath + "name");

			if(!serviceName.isEmpty()) {
				properties.put(type + "." + stepId + ".service.name", serviceName);
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

	private void getHeaderFromXMLFile(String type, String stepId, String headerId) throws ConfigurationException {

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

			properties.put(type + "." + stepId + ".header.id", headerId);


			String headerName = conf.getString("integration/headers/header[id='" + headerId + "']/name");
			if(!headerName.isEmpty()) {
				properties.put("header." + headerId + ".name", headerName);
			}
		}
	}

	private void getRouteFromXMLFile(String type, String stepId, String routeId) throws Exception {

		Document doc = conf.getDocument();

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/integrations/integration/routes/route[@id='" + routeId + "']");
		Node node = (Node)expr.evaluate(doc, XPathConstants.NODE);

		String routeAsString = convertNodeToString(node);

		if(routeAsString.contains("<customDataFormat ref")){
			expr = xpath.compile("/integrations/integration/routeConfigurations/routeConfiguration/dataFormats");
			node = (Node)expr.evaluate(doc, XPathConstants.NODE);		
			String dataFormatAsString = convertNodeToString(node);
			dataFormatAsString = StringUtils.substringBetween(dataFormatAsString, "<dataFormats>",  "</dataFormats");
			routeAsString = routeAsString.replaceAll("<customDataFormat ref=(.*)", dataFormatAsString);
		}

		properties.put(type + "." + stepId + ".route", routeAsString);
		properties.put(type + "." + stepId + ".route.id", routeId);

	}


	private void getRouteConfigurationFromXMLFile(String type, String stepId, String routeConfigurationId) throws Exception {

		Document doc = conf.getDocument();

		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression expr = xpath.compile("/integrations/integration/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");
		Node node = (Node)expr.evaluate(doc, XPathConstants.NODE);

		String routeAsString = convertNodeToString(node);

		properties.put(type + "." + stepId + ".routeconfiguration", routeAsString);
		properties.put(type + "." + stepId + ".routeconfiguration.id", routeConfigurationId);

	}

	private void getRouteTemplateFromXMLFile(String type, String flowId, String stepId, List<String> optionProperties, String[] links) throws Exception {

		String templateId = createTemplateId(baseUri, type);
		String routeId = flowId + "-" + stepId;

		Document templateDoc = createNewDocument();

		Element templatedRoutes = templateDoc.createElementNS("http://camel.apache.org/schema/spring", "templatedRoutes");
		templateDoc.appendChild(templatedRoutes);

		Element templatedRoute = templateDoc.createElement("templatedRoute");
		templatedRoute.setAttribute("routeTemplateRef", templateId);
		templatedRoute.setAttribute("routeId", routeId);
		templatedRoutes.appendChild(templatedRoute);

		if(baseUri.startsWith("template:")){
			baseUri = StringUtils.substringAfter(baseUri,"template:");
			uri = StringUtils.substringAfter(uri,"template:");
		}

		Element parameter = createParameter(templateDoc,"uri",uri);
		templatedRoute.appendChild(parameter);

		String scheme = baseUri;
		if(baseUri.contains(":")){
			scheme = baseUri.split(":",2)[0];
		}

		parameter = createParameter(templateDoc,"scheme",scheme);
		templatedRoute.appendChild(parameter);

		String path = "";
		if(baseUri.contains(":")){
			path = baseUri.split(":",2)[1];
		}

		parameter = createParameter(templateDoc,"path",path);
		templatedRoute.appendChild(parameter);

		parameter = createParameter(templateDoc,"options",options);
		templatedRoute.appendChild(parameter);

		for (String optionProperty : optionProperties) {
			String name = optionProperty.split("options.")[1];
			String value = conf.getProperty(optionProperty).toString();

			parameter = createParameter(templateDoc,name,value);
			templatedRoute.appendChild(parameter);

		}

		String transport = Objects.toString(conf.getProperty("integration/flows/flow[id='" + flowId + "']/transport"), null);

		if(transport==null){
			transport = "sync";
		}

		if(links.length > 0){

			int index = 1;

			for(String link : links) {

				String linkXPath = stepXPath + "links/link[" + index + "]/";

				String value = "";

				String bound = Objects.toString(conf.getProperty(linkXPath + "bound"), null);
				String linktransport = Objects.toString(conf.getProperty(linkXPath + "transport"), null);
				String id = Objects.toString(conf.getProperty(linkXPath + "id"), null);
				options = Objects.toString(conf.getProperty(linkXPath + "options"), null);

				if(linktransport!=null){
					transport = linktransport;
				}

				if (options == null || options.isEmpty()) {
					value = transport + ":" + id;
				} else {
					value = transport + ":" + id + "?" + options;
				}

				parameter = createParameter(templateDoc,bound, value);
				templatedRoute.appendChild(parameter);

				index++;

			}

		}else{

			String stepIndex = StringUtils.substringBetween(stepXPath,"step[","]");
			String value = "";

			if(StringUtils.isNumeric(stepIndex)){

				if(type.equals("source") || type.equals("action")){

					Integer nextStepIndex = Integer.parseInt(stepIndex) + 1;

					String nextStepXpath  = StringUtils.replace(stepXPath,"/step[" + stepIndex + "]","/step[" + nextStepIndex + "]");

					String nextStepId = Objects.toString(conf.getProperty(nextStepXpath + "/id"), null);

					value = transport + ":" + flowId + "-" + nextStepId;

					parameter = createParameter(templateDoc,"out", value);
					templatedRoute.appendChild(parameter);

				}

				if(type.equals("sink") || type.equals("action")){

					String currentStepId = Objects.toString(conf.getProperty(stepXPath + "/id"), null);

					value = transport + ":" + flowId + "-" + currentStepId;

					parameter = createParameter(templateDoc,"in", value);
					templatedRoute.appendChild(parameter);

				}

			}

		}

		String routeConfiguratinID = Objects.toString(conf.getProperty("integration/flows/flow/steps/step[type='error']/routeconfiguration_id"), null);

		if(routeConfiguratinID!=null){
			parameter = createParameter(templateDoc,"routeconfigurationid", routeConfiguratinID);
			templatedRoute.appendChild(parameter);
		}

		String routeTemplate = DocConverter.convertDocToString(templateDoc);

		properties.put(type + "." + stepId + ".route", routeTemplate);
		properties.put(type + "." + stepId + ".route.id",  routeId);

	}

	private Document createNewDocument() throws ParserConfigurationException {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// root elements
		Document doc = docBuilder.newDocument();

		return doc;
	}

	private String createTemplateId(String uri,String type){

		String templateId = "generic-" + type;

		if(uri.startsWith("template")){
			String componentName = uri.split(":")[1];
			templateId = componentName + "-" + type;
		}

		return templateId;
	}

	private Element createParameter(Document doc, String name, String value){

		Element parameter = doc.createElement("parameter");
		parameter.setAttribute("name", name);
		parameter.setAttribute("value", value);

		return parameter;

	}

	private String createUri(String path) {

		if (!options.isEmpty()) {
			uri = path + "?" + options;
		} else {
			uri = path;
		}

		return uri;
	}

	private String createOptions(List<String> optionProperties){

		options = "";

		for (String optionProperty : optionProperties) {
			String name = optionProperty.split("options.")[1];
			String value = conf.getProperty(optionProperty).toString();

			options += name + "=" + value + "&";
		}

		if(!options.isEmpty()){
			options = options.substring(0,options.length() -1);
		}

		return options;
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

