package org.assimbly.dil.transpiler.marshalling;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

//This class marshalls (converts) the Java treemap object to XML
public class Marshall {

	private Document doc;
	private Element flows;
	
	private Element routes;
	private Element routeConfigurations;
	private Element connections;
	private Element messages;

	private List<String> routesList;
	private List<String> connectionsList;
	private List<String> messageList;

	private Element steps;

	public Document setProperties(Document document, String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		doc = document;

		setGeneralProperties(integrationId);

		for (TreeMap<String, String> configuration : configurations) {
			setFlowFromConfiguration(configuration);
		}

		return doc;

	}


	public Document setProperties(Document document, TreeMap<String, String> configuration) throws Exception {

		doc = document;

		setGeneralProperties("live");

		setFlowFromConfiguration(configuration);

		return doc;

	}



	private void setGeneralProperties(String integrationId) {

		Element rootElement = doc.createElement("integrations");
		doc.appendChild(rootElement);

		Element integration = doc.createElement("integration");
		rootElement.appendChild(integration);

		Element id = doc.createElement("id");
		id.appendChild(doc.createTextNode(integrationId));
		integration.appendChild(id);

		flows = doc.createElement("flows");
		routes = doc.createElement("routes");
		routeConfigurations = doc.createElement("routeConfigurations");
		connections = doc.createElement("connections");
		messages = doc.createElement("messages");

		integration.appendChild(flows);
		integration.appendChild(routes);
		integration.appendChild(routeConfigurations);
		integration.appendChild(connections);
		integration.appendChild(messages);

		//List to ensure no double entries
		routesList = new ArrayList<>();
		connectionsList = new ArrayList<>();
		messageList = new ArrayList<>();

	}


	private void setFlowFromConfiguration(TreeMap<String, String> configuration) throws Exception {

		Element flow = doc.createElement("flow");
		flows.appendChild(flow);

		addElement(configuration, flow, "id", "id");

		addElement(configuration, flow, "name", "flow.name");

		addElement(configuration, flow, "type", "flow.type");

		addElement(configuration, flow, "version", "flow.version");

		addElement(configuration, flow, "autostart", "flow.autostart");

		addElement(configuration, flow, "parallelProcessing", "flow.parallelProcessing");

		addElement(configuration, flow, "maximumRedeliveries", "flow.maximumRedeliveries");

		addElement(configuration, flow, "redeliveryDelay", "flow.redeliveryDelay");

		addElement(configuration, flow, "logLevel", "flow.logLevel");

		addElement(configuration, flow, "notes", "flow.notes");

		Element components = doc.createElement("components");
		flow.appendChild(components);

		String[] confComponentsSplitted = StringUtils.split(configuration.get("flow.components"),",");
		if(confComponentsSplitted!=null){
			for(String confComponentSplitted : confComponentsSplitted){
				Element flowComponentNode = doc.createElement("component");
				flowComponentNode.appendChild(doc.createTextNode(confComponentSplitted));
				components.appendChild(flowComponentNode);
			}
		}

		steps = doc.createElement("steps");
		flow.appendChild(steps);

		//set steps
		setFlowSteps(configuration);

	}

	private void setFlowSteps(TreeMap<String, String> configuration) throws Exception {

		List<String> confUriKeyList = configuration.keySet().stream().filter(k -> k.endsWith("uri")).toList();

			for(String confUriKey : confUriKeyList){
				String confUri = configuration.get(confUriKey);
				String[] confUriKeySplitted = StringUtils.split(confUriKey,".");
				String confType = confUriKeySplitted[0];
				String confStepId = confUriKeySplitted[1];

				String confConnectionId = configuration.get(confType + "." + confStepId + ".connection.id");
				String confmessageid = configuration.get(confType + "." + confStepId + ".message.id");
				String confRouteId = configuration.get(confType + "." + confStepId + ".route.id");

				Element step = doc.createElement("step");

				setStepFromConfiguration(confType, confUri, confStepId, step);
				setStepIds(confConnectionId, confmessageid, confRouteId, confRouteId, step, configuration);
			}
	}

	private void setStepFromConfiguration(String confType, String confUri, String confstepId, Element step) throws Exception {

		Element uri = doc.createElement("uri");
		Element type = doc.createElement("type");
		Element stepId = doc.createElement("id");
		Element options = doc.createElement("options");

		steps.appendChild(step);

		String[] confUriSplitted = confUri.split("\\?");

		if(confUriSplitted.length<=1) {
			if(confUri.startsWith("sonicmq")) {
				String updatedConfUri = confUri.replaceFirst("sonicmq.*:", "sonicmq:");
				uri.setTextContent(updatedConfUri);
			}else{
				uri.setTextContent(confUri);
			}

			stepId.setTextContent(confstepId);
			type.setTextContent(confType);
			

			step.appendChild(stepId);
			step.appendChild(type);
			step.appendChild(uri);

		}else {
			if(confUriSplitted[0].startsWith("sonicmq")) {
				confUriSplitted[0] = confUriSplitted[0].replaceFirst("sonicmq.*:", "sonicmq:");
			}
			step.setTextContent(confstepId);
			uri.setTextContent(confUri);
			type.setTextContent(confType);

			step.appendChild(stepId);
			step.appendChild(type);
			step.appendChild(uri);
			step.appendChild(options);

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
	}

	private void setStepIds(String confConnectionId, String confmessageid, String confRouteId, String confResponseId, Element step, TreeMap<String, String> configuration) throws Exception {
		Element connectionid = doc.createElement("connection_id");
		Element messageid = doc.createElement("message_id");
		Element responseId = doc.createElement("response_id");
		Element routeId = doc.createElement("route_id");

		if(confResponseId != null) {
			responseId.setTextContent(confResponseId);
			step.appendChild(responseId);
		}

		if(confRouteId != null) {
			routeId.setTextContent(confRouteId);
			step.appendChild(routeId);
			setRouteFromConfiguration(confRouteId, configuration);
		}

		if(confConnectionId!=null) {
			connectionid.setTextContent(confConnectionId);
			step.appendChild(connectionid);
			setConnectionFromConfiguration(confConnectionId, configuration);
		}

		if(confmessageid!=null) {
			step.appendChild(messageid);
			messageid.setTextContent(confmessageid);
			setMessageFromConfiguration(confmessageid, configuration);
		}

	}


	private void setRouteFromConfiguration(String routeid, TreeMap<String, String> configuration) throws Exception {

		if(!routesList.contains(routeid)) {
			routesList.add(routeid);

			for(Map.Entry<String,String> entry : configuration.entrySet()) {
				String key = entry.getKey();
				String parameterValue = entry.getValue();

				if(key.endsWith(routeid + ".route") && parameterValue!=null) {

					Document document = getDocument(parameterValue);
                    Node node = doc.importNode(document.getDocumentElement(), true);
					if(parameterValue.startsWith("<route")) {
                    	routes.appendChild(node);
					}else if(parameterValue.startsWith("<routeConfiguration")) {
						routeConfigurations.appendChild(node);
					}
				}
			}
		}
	}

	private void setConnectionFromConfiguration(String connectionid, TreeMap<String, String> configuration) throws Exception {

		if(!connectionsList.contains(connectionid)) {
			connectionsList.add(connectionid);

			Element connection = doc.createElement("connection");
			connections.appendChild(connection);

			Element connectionIdParameter = doc.createElement("id");
			connectionIdParameter.setTextContent(connectionid);
			connection.appendChild(connectionIdParameter);

			for(Map.Entry<String,String> entry : configuration.entrySet()) {
				String key = entry.getKey();
				String parameterValue = entry.getValue();

				if(key.startsWith("connection." + connectionid) && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "connection." + connectionid + ".");
					Element connectionParameter = doc.createElement(parameterName);
					connectionParameter.setTextContent(parameterValue);
					connection.appendChild(connectionParameter);
				}
			}
		}
	}

	private void setMessageFromConfiguration(String messageid, TreeMap<String, String> configuration) throws Exception {

		if(!messageList.contains(messageid)) {

			messageList.add(messageid);

			Element message = doc.createElement("message");
			messages.appendChild(message);

			Element messageidParameter = doc.createElement("id");
			messageidParameter.setTextContent(messageid);
			message.appendChild(messageidParameter);

			for(Map.Entry<String,String> entry : configuration.entrySet()) {
				String key = entry.getKey();
				String parameterValue = entry.getValue();

				if(key.startsWith("message." + messageid + ".xpath") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "xpath.");
					Element messageParameter = doc.createElement(parameterName);
					messageParameter.setTextContent(parameterValue);
					messageParameter.setAttribute("type", "xpath");
					message.appendChild(messageParameter);
				}else if(key.startsWith("message." + messageid +  ".constant") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "constant.");
					Element messageParameter = doc.createElement(parameterName);
					messageParameter.setTextContent(parameterValue);
					messageParameter.setAttribute("type", "constant");
					message.appendChild(messageParameter);
				}else if(key.startsWith("message." + messageid +  ".simple") && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "simple.");
					Element messageParameter = doc.createElement(parameterName);
					messageParameter.setTextContent(parameterValue);
					messageParameter.setAttribute("type", "simple");
					message.appendChild(messageParameter);
				}else if(key.startsWith("message." + messageid) && parameterValue!=null) {
					String parameterName = StringUtils.substringAfterLast(key, "message." + messageid + ".");
					Element messageParameter = doc.createElement(parameterName);
					messageParameter.setTextContent(parameterValue);
					message.appendChild(messageParameter);
				}
			}
		}
	}

	private void addElement(TreeMap<String, String> configuration, Element parent, String name, String key) {
		String value = configuration.get(key);
		Element child = doc.createElement(name);
		child.appendChild(doc.createTextNode(value));
		parent.appendChild(child);
	}

	private Document getDocument(String xml) throws Exception {

        DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

    }
}