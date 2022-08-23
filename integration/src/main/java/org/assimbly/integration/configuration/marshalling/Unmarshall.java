package org.assimbly.integration.configuration.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assimbly.integration.configuration.marshalling.blocks.*;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import javax.xml.xpath.*;
import java.util.*;
import java.util.stream.Collectors;

//This class unmarshalls from a XML file to a Java treemap object
//Functionally a DIL (Data Integration Language) file is converted to an Assimbly configuration
public class Unmarshall {

	private TreeMap<String, String> properties;

	private XMLConfiguration conf;

	Document doc;

	private String integrationXPath;
	private String stepXPath;

	private String baseUri;
	private String uri;
	private String options;
	private String type;
	private List<String> optionProperties;
	private String[] links;

	private String flowId;
	private String flowName;
	private String flowType;
	private String flowComponents;
	private String flowMaximumRedeliveries;
	private String flowRedeliveryDelay;
	private String flowLogLevel;
	private String flowAssimblyHeaders;
	private String flowParallelProcessing;
	private String stepId;

	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		//create a Treemap for the configuration
		properties = new TreeMap<String, String>();

		//get the XML as document
		doc = configuration.getDocument();
		conf = configuration;
		this.flowId = flowId;

		setFlows();

		setSteps();
					
		setFlowType();

		return properties;

	}

	private void setFlows() throws Exception{

		setFlowDefaults();

		String flowSelector = setFlowSelector();

		setFlowProperties(flowSelector);

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

	public void setFlowProperties(String flowSelector) throws XPathExpressionException {

		XPath xPath = XPathFactory.newInstance().newXPath();

		flowName = xPath.evaluate("//flows/flow[" + flowSelector + "]/name",doc);
		flowType = xPath.evaluate("//flows/flow[" + flowSelector + "]/type",doc);

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

		String componentsXpath = "integrations/integration/flows/flow[" + flowSelector + "]/components/component";

		String[] components = conf.getStringArray(componentsXpath);

		for(String component : components){
			if(flowComponents==null){
				flowComponents = component;
			}else{
				flowComponents = flowComponents + "," + component;
			}
		}

		properties.put("id",flowId);
		properties.put("flow.name",flowName);
		properties.put("flow.type",flowType);

		properties.put("flow.components",flowComponents);

		properties.put("flow.maximumRedeliveries",flowMaximumRedeliveries);
		properties.put("flow.redeliveryDelay",flowRedeliveryDelay);
		properties.put("flow.logLevel",flowLogLevel);
		properties.put("flow.assimblyHeaders",flowAssimblyHeaders);
		properties.put("flow.parallelProcessing",flowParallelProcessing);

	}

	private void setFlowType(){
		Set<String> fromUriSet = properties.keySet().stream().filter(s -> s.startsWith("from.") && s.endsWith(".uri")).collect(Collectors.toSet());

		if(flowType.isEmpty() && !fromUriSet.isEmpty()){
			properties.put("flow.type","default");
		}else if (flowType.isEmpty()){
			properties.put("flow.type", "none");
		}
	}

	private void setSteps() throws Exception {

		String stepsXpath = "//flows/flow[id='" + flowId + "']/steps/step/id";

		String[] steps = conf.getStringArray(stepsXpath);

		int index = 1;

		for(String step : steps){

			stepXPath = "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/";

			setStepProperties(index);

			setStepBlocks();

			index++;
		}
	}

	private void setStepProperties(int index) throws XPathExpressionException, ConfigurationException {

		stepId = conf.getString(stepXPath + "id");
		type = conf.getString(stepXPath + "type");

		System.out.println("XPATH:" + stepXPath + "type");
		System.out.println("TYPE:" + type);

		optionProperties = IntegrationUtil.getXMLParameters(conf, stepXPath + "options");
		options = createOptions(optionProperties);

		baseUri = createBaseUri(index);
		uri = createUri(baseUri);

		links = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/links/link/id");

		if(uri != null){
			properties.put(type + "." + stepId + ".uri", uri);
		}

	}

	private void setStepBlocks() throws Exception {

		setHeader();

		setConnection();

		setRoute();

		setRouteConfiguration();

		setPropertiesByType(type);

	}

	public void setPropertiesByType(String type) throws Exception {

		switch (type) {
			case "source":
			case "action":
			case "router":
			case "sink":
				setRouteTemplate();
				break;
			case "response":
				setResponse();
				break;
			case "to":
				setTo();
				break;
		}

	}

	private void setHeader() throws ConfigurationException {
		String headerId = conf.getString(stepXPath + "header_id");

		if(headerId != null)
			properties =  new Header(properties, conf).setHeader(type, stepId, headerId);
	}

	private void setConnection() throws ConfigurationException {
		String connectionId = conf.getString(stepXPath + "connection_id");

		if(connectionId != null)
			properties =  new Connection(properties, conf).setConnection(type, stepId, connectionId);

	}

	private void setRoute() throws Exception {
		String routeId = conf.getString(stepXPath + "route_id");

		if(routeId != null)
			properties =  new Route(properties, conf).setRoute(type, stepId, routeId);
	}

	private void setRouteConfiguration() throws Exception {
		String routeConfigurationId = conf.getString(stepXPath + "routeconfiguration_id");

		if(routeConfigurationId != null)
			properties =  new RouteConfiguration(properties, conf).setRouteConfiguration(type, stepId, routeConfigurationId);
	}

	private void setRouteTemplate() throws Exception {
		properties =  new RouteTemplate(properties, conf).setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);
	}

	private void setResponse(){
		String responseId = conf.getString(stepXPath + "response_id");
		if(responseId != null) {
			properties.put(type + "." + stepId + ".response.id", responseId);
		}
	}

	private void setTo(){
		String offrampUri = "";

		if(stepId != null){
			if(offrampUri.isEmpty()) {
				offrampUri = "direct:flow=" + flowId + "step=" + stepId;
			}else {
				offrampUri = offrampUri + ",direct:flow=" + flowId + "step=" + stepId;
			}
		}

		String responseId = conf.getString(stepXPath + "response_id");

		if(responseId != null) {
			properties.put(type + "." + stepId + ".response.id", responseId);
		}
		properties.put("offramp.uri.list", offrampUri);
	}

	private String createBaseUri(int index) throws XPathExpressionException {

		XPath xpath = XPathFactory.newInstance().newXPath();

		XPathExpression expr = xpath.compile("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		baseUri = expr.evaluate(doc);

		return baseUri;

	}

	private String createUri(String baseUri) {

		if (options != null && !options.isEmpty()) {
			uri = baseUri + "?" + options;
		} else {
			uri = baseUri;
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

}