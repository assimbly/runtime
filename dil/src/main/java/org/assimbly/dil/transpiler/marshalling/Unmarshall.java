package org.assimbly.dil.transpiler.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
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
	private String options;
	private final List<String> routeTemplateList = Arrays.asList("source", "action", "router", "sink", "message", "script");
	private final String[] connectionTypes = {
			"activemq", "amazonmq", "amq", "jms", "sjms", "sjms2",
			"amqp", "amqps", "rabbitmq", "spring-rabbitmq", "flowlink"
	};


	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		this.flowId = flowId;
		doc = configuration.getDocument();
		conf = configuration;
		properties = new TreeMap<>();

		properties.put("id",flowId);

		Element flowElement = getFlowElement();

		addProperty(flowElement, "name");
		addProperty(flowElement, "type");
		addProperty(flowElement, "version");

		createResources();

		addSteps(flowElement);

		return properties;

	}

	private Element getFlowElement(){

		NodeList flow = doc.getElementsByTagName("flow");

		for (int i = 0; i < flow.getLength(); i++) {
			Node node = flow.item(i);
			if (node instanceof Element element) {
                String id = element.getElementsByTagName("id").item(0).getFirstChild().getTextContent();

				if (flowId.equals(id)) {
					return element;
				}else{
					throw new RuntimeException("Configured flow ID: " + id + " in the DIL file does not match flow ID: " + flowId);
				}
			}
		}

		throw new RuntimeException("Configuration file misses flow element");

	}

	private void addProperty(Element element, String name){
		Node node = element.getElementsByTagName(name).item(0);

		if(node!=null){
			properties.put("flow." + name,node.getFirstChild().getTextContent());
		}

	}

	private void addSteps(Element flow) throws Exception {
		NodeList steps = flow.getElementsByTagName("step");
		for (int index = 1; index < steps.getLength() + 1; index++) {
			Element stepElement = (Element) steps.item(index - 1);
			processStep(stepElement, index);
		}
	}

	private void processStep(Element stepElement, int index) throws Exception {
		String stepId = "";
		String type = "";
		String uri = "";

		NodeList children = stepElement.getChildNodes();
		for (int j = 0; j < children.getLength(); j++) {
			Node node = children.item(j);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				switch (node.getNodeName()) {
					case "id":
						stepId = node.getTextContent().trim();
						break;
					case "type":
						type = node.getTextContent().trim();
						break;
					case "uri":
						uri = node.getTextContent().trim();
						break;
				}
			}
		}

		if (!uri.isEmpty()) {
			setUri(uri, stepId, type, index);
		}

		if (routeTemplateList.contains(type)) {
			setBlocks(stepElement, stepId, type);
			setRouteTemplate(index, stepId, type);
		} else {
			setBlocks(stepElement, stepId, type);
		}
	}

	private void setUri(String uri, String stepId, String type, int index) {

		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");

		if(optionProperties.isEmpty() || optionProperties.getFirst().endsWith("options")){
			options = "";
			properties.put(type + "." + stepId + ".uri", uri);
			return;
		}

		options = getOptions(optionProperties);

		properties.put(type + "." + stepId + ".uri", uri + "?" + options);

		if(type.equals("error")){
			setErrorHandlerOptions(optionProperties);
		}

	}

	private String getOptions(List<String> optionProperties) {

		StringBuilder uriOptions = new StringBuilder();

		for (int i = 0; i < optionProperties.size(); i++) {

			String optionProperty = optionProperties.get(i);
			int optionsIndex = optionProperty.indexOf("options") + 8;

			String name = optionProperty.substring(optionsIndex);
			String value = conf.getProperty(optionProperty).toString();

			if (i > 0) {
				uriOptions.append("&");
			}
			uriOptions.append(name).append("=").append(value);
		}

		return uriOptions.toString();

	}

	private void setErrorHandlerOptions(List<String> optionProperties){

        for (String optionProperty : optionProperties) {

            int optionsIndex = optionProperty.indexOf("options") + 8;

            String name = optionProperty.substring(optionsIndex);
            String value = conf.getProperty(optionProperty).toString();

            properties.put("flow." + name, value);
        }

	}

	private void setBlocks(Element stepElement, String stepId, String type) throws Exception {

		NodeList block = stepElement.getElementsByTagName("block");

		if(block.getLength() == 0){
			return;
		}

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

	private void setRouteTemplate(int stepIndex, String stepId, String type) throws Exception {

		String stepXPath = "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + stepIndex + "]/";

		String[] links = conf.getStringArray(stepXPath + "links/link/id");

		String baseUri = conf.getString(stepXPath + "uri");

		String scheme = baseUri;
		String path = "";
		int colonIndex = baseUri.indexOf(':');
		if(colonIndex > 0){
			scheme = baseUri.substring(0, colonIndex);
			path = baseUri.substring(colonIndex + 1);
		}

		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, stepXPath + "options");

		boolean hasConnectionFactory = Arrays.asList(connectionTypes).contains(scheme);

		if(hasConnectionFactory && !options.contains("connectionFactory") && !options.contains("transport=sync") && !options.contains("transport=async")) {
				System.out.println("3. Add connectionfactory");
				optionProperties.add(stepXPath + "options/connectionFactory");
				options = addConnectionFactoryOption(options, stepId, type, stepXPath);
		}

		RouteTemplate routeTemplate = new RouteTemplate(properties, conf);

		properties = routeTemplate.setRouteTemplate(flowId, stepId, type, baseUri, scheme, path, options, optionProperties, links, stepXPath, stepIndex);

	}

	private String addConnectionFactoryOption(String options, String stepId, String type, String stepXPath) {

		String connectionId = properties.get(type + "." + stepId + ".connection.id");

		String option = "connectionFactory=#" + connectionId;

		conf.addProperty(stepXPath + "options/connectionFactory","#" + connectionId);

		if(options.isEmpty()){
			options = option;
		}else{
			options = options + "&" + option;
		}

		return options;

	}

	private void createResources() {

		String[] resources = conf.getStringArray("core/resources/resource/content");

		for (int i = 1; i < resources.length + 1; i++) {
			String id = conf.getString("core/resources/resource[" + i + "]/id");
			String name = conf.getString("core/resources/resource[" + i + "]/name");

			if (id == null && name == null) {
				continue;
			}else if (id == null) {
				id = name;
			}

			String content = conf.getString("core/resources/resource[" + i	+ "]/content");
			properties.put("resource." + id, content);

		}

	}

}
