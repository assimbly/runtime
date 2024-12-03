package org.assimbly.dil.transpiler.marshalling;

import net.sf.saxon.s9api.*;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.configuration2.XMLConfiguration;
import org.assimbly.dil.transpiler.marshalling.core.*;
import org.assimbly.dil.transpiler.marshalling.core.Message;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.*;
import java.util.*;

// This class unmarshalls an XML file into a Java treemap object
// The XML file must be in DIL (Data Integration Language) format
public class Unmarshall {

	private Document doc;
	private TreeMap<String, String> properties;
	private XMLConfiguration conf;
	private String flowId;
	XPathFactory xf = new XPathFactoryImpl();
	List<String> routeTemplateList = Arrays.asList("source", "action", "router", "sink", "message", "script");

	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		this.flowId = flowId;
		doc = configuration.getDocument();
		conf = configuration;
		properties = new TreeMap<String, String>();

		setFlow();

		return properties;

	}

	public void setFlow() throws Exception {

		properties.put("id",flowId);

		Element flowElement = getFlowElement();

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
			if (node instanceof Element) {
				Element element = (Element) node;
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

		String flowDependencies = null;
		NodeList dependenciesList = dependencies.getChildNodes();

		for (int i = 0; i < dependenciesList.getLength(); i++) {
			Node dependency = dependenciesList.item(i);
			if (dependency instanceof Element) {
				if(i == 0){
					flowDependencies = dependency.getTextContent();
				}else{
					flowDependencies = flowDependencies + "," + dependency.getTextContent();
				}

			}
		}

		properties.put("flow.dependencies",flowDependencies);

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
				setRouteTemplate(i + 1, stepId, type);
				setBlocks(stepElement, stepId, type);
			}else{
				setBlocks(stepElement, stepId, type);
			}

		}

	}

	private void setUri(String uri, String stepId, String type, int index) throws Exception {

		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");
		String options = getOptions(optionProperties);

		if (options != null && !options.isEmpty()) {
			uri = uri + "?" + options;
		}

		properties.put(type + "." + stepId + ".uri", uri);

	}

	private String getOptions(List<String> optionProperties){

		String options = "";

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

	private void setBlocks(Element stepElement, String stepId, String type) throws Exception {

		NodeList block = stepElement.getElementsByTagName("block");

		for (int i = 0; i < block.getLength(); i++) {

			Element blockElement = (Element) block.item(i);

			Node messageId = blockElement.getElementsByTagName("message_id").item(0);
			Node connectionId = blockElement.getElementsByTagName("connection_id").item(0);
			Node routeId = blockElement.getElementsByTagName("route_id").item(0);
			Node routeconfigurationId = blockElement.getElementsByTagName("routeconfiguration_id").item(0);

			if(messageId != null)
				properties =  new Message(properties, conf).setHeader(type, stepId, messageId.getTextContent());

			if(connectionId != null)
				properties = new Connection(properties, conf).setConnection(type, stepId, connectionId.getTextContent());

			if(routeId != null)
				properties = new Route(properties, conf, doc).setRoute(type, flowId, stepId, routeId.getTextContent());

			if(routeconfigurationId != null)
				properties = new RouteConfiguration(properties, conf).setRouteConfiguration(type, stepId, routeconfigurationId.getTextContent());

		}

	}

	private void setRouteTemplate(int index, String stepId, String type) throws Exception {

		String stepXPath = "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/";
		String[] links = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/links/link/id");
		String baseUri = conf.getString("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");
		String options = getOptions(optionProperties);

		RouteTemplate routeTemplate = new RouteTemplate(properties, conf);

		if(baseUri.startsWith("blocks") || baseUri.startsWith("component")){
			properties =  routeTemplate.setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);
		}else{
			properties =  routeTemplate.setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);
		}

	}

}
