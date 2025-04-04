package org.assimbly.dil.transpiler.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.marshalling.core.*;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;

// This class unmarshalls an XML file into a Java treemap object
// The XML file must be in DIL (Data Integration Language) format
public class Unmarshall {

	private Document doc;
	private TreeMap<String, String> properties;
	private XMLConfiguration conf;
	private String flowId;
	List<String> routeTemplateList = Arrays.asList("source", "action", "router", "sink", "message", "script");

	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		this.flowId = flowId;
		doc = configuration.getDocument();
		conf = configuration;
		properties = new TreeMap<>();

		setFlow();

		return properties;

	}

	public void setFlow() throws Exception {

		properties.put("id",flowId);

		Element flowElement = getFlowElement();

        assert flowElement != null;
        addProperty(flowElement, "name","flow.");
		addProperty(flowElement, "type","flow.");
		addProperty(flowElement, "version","flow.");

		Node node = doc.getElementsByTagName("frontend").item(0);

		if(node!=null){
			properties.put("frontend",node.getFirstChild().getTextContent());
		}

		addDependencies(flowElement);

		addSteps(flowElement);

	}

	private Element getFlowElement(){

		NodeList flow = doc.getElementsByTagName("flow");

		for (int i = 0; i < flow.getLength(); i++) {
			Node node = flow.item(i);
			if (node instanceof Element element) {
                String id = element.getElementsByTagName("id").item(0).getFirstChild().getTextContent();

				if (flowId.equals(id)) {
					return element;
				}
			}
		}

		return null;
	}

	private void addProperty(Element element, String name,String prefix){
		Node node = element.getElementsByTagName(name).item(0);

		if(node!=null){
			properties.put(prefix + name,node.getFirstChild().getTextContent());
		}

	}

	private void addDependencies(Element element){

		Node dependencies = element.getElementsByTagName("dependencies").item(0);

		if(dependencies==null || !dependencies.hasChildNodes()) {
			return;
		}

		StringBuilder flowDependencies = new StringBuilder();
		NodeList dependenciesList = dependencies.getChildNodes();

		for (int i = 0; i < dependenciesList.getLength(); i++) {
			Node dependency = dependenciesList.item(i);
			if (dependency instanceof Element) {
				if(i == 0){
					flowDependencies.append(dependency.getTextContent());
				}else{
                    assert flowDependencies != null;
                    flowDependencies.append(",").append(dependency.getTextContent());
				}

			}
		}

		properties.put("flow.dependencies", flowDependencies.toString());

	}

	private void addSteps(Element flow) throws Exception {

		NodeList steps = flow.getElementsByTagName("step");

		for (int i = 0; i < steps.getLength(); i++) {

			Element stepElement = (Element) steps.item(i);

			String stepId = stepElement.getElementsByTagName("id").item(0).getFirstChild().getTextContent();
			String type = stepElement.getElementsByTagName("type").item(0).getFirstChild().getTextContent();
			Node uriList = stepElement.getElementsByTagName("uri").item(0);

			if(uriList != null && uriList.hasChildNodes()){
				String uri = uriList.getFirstChild().getTextContent();
				setUri(uri, stepId, type, i + 1);
			}

			if(routeTemplateList.contains(type)) {
				setBlocks(stepElement, stepId, type);
				setRouteTemplate(i + 1, stepId, type);
			}else{
				setBlocks(stepElement, stepId, type);
			}

		}

	}

	private void setUri(String uri, String stepId, String type, int index) {

		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");
		String options = getOptions(optionProperties);

		if (!options.isEmpty()) {
			uri = uri + "?" + options;
		}

		properties.put(type + "." + stepId + ".uri", uri);

	}

	private String getOptions(List<String> optionProperties){

		StringBuilder options = new StringBuilder();

		for (String optionProperty : optionProperties) {
			String name = optionProperty.split("options.")[1];
			String value = conf.getProperty(optionProperty).toString();

			options.append(name).append("=").append(value).append("&");
		}

		if(!options.isEmpty()){
			options = new StringBuilder(options.substring(0, options.length() - 1));
		}

		return options.toString();
	}

	private void setBlocks(Element stepElement, String stepId, String type) throws Exception {

		NodeList block = stepElement.getElementsByTagName("block");

		for (int i = 0; i < block.getLength(); i++) {

			Element blockElement = (Element) block.item(i);

			Node blockId = blockElement.getElementsByTagName("id").item(0);
			Node blockType = blockElement.getElementsByTagName("type").item(0);

			if(blockId!=null && blockType!=null){

				String blockTypeValue = blockType.getTextContent();

				if(blockTypeValue.equalsIgnoreCase("message")){
					properties =  new Message(properties, conf).setHeader(type, stepId, blockId.getTextContent());
				}else if (blockTypeValue.equalsIgnoreCase("connection")) {
					properties = new Connection(properties, conf).setConnection(type, stepId, blockId.getTextContent());
				}else if (blockTypeValue.equalsIgnoreCase("route")) {
					properties = new Route(properties, doc).setRoute(type, flowId, stepId, blockId.getTextContent());
				}else if (blockTypeValue.equalsIgnoreCase("routeconfiguration")) {
					properties = new RouteConfiguration(properties, conf).setRouteConfiguration(type, stepId, blockId.getTextContent());
				}

			}

		}

	}

	private void setRouteTemplate(int index, String stepId, String type) throws Exception {

		String stepXPath = "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/";
		String[] links = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/links/link/id");
		String baseUri = conf.getString("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");
		String options = getOptions(optionProperties);
		options = addConnectionFactoryOption(baseUri, options, stepId, type);

		RouteTemplate routeTemplate = new RouteTemplate(properties, conf);

		properties =  routeTemplate.setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);

	}

	private String addConnectionFactoryOption(String baseUri, String options, String stepId, String type) {

		String connectionId = properties.get(type + "." + stepId + ".connection.id");
		String componentType = baseUri.split(":")[0];

		String[] connectionTypes = {
				"activemq", "amazonmq", "jms", "sjms", "sjms2",
				"amqp", "amqps", "rabbitmq", "spring-rabbitmq"
		};
		boolean hasConnectionFactory = Arrays.asList(connectionTypes).contains(componentType);


		if(connectionId != null && !options.contains("connectionFactory") && hasConnectionFactory){

			String option = "connectionFactory=#bean:" + connectionId;

			if(options.isEmpty()){
				options = option;
			}else{
				options = options + "&" + option;
			}

		}

		return options;

	}

}
