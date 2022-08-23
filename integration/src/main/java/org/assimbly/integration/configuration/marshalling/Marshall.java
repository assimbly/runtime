package org.assimbly.integration.configuration.marshalling;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

//This class marshalls (converts) the Java treemap object to XML
public class Marshall {

	private Document doc;
	private Element rootElement;
	private Element flows;
	
	private Element routes;
	private Element routeConfigurations;
	private Element connections;
	private Element headers;
	private Element flow;
	private Element integration;

	private List<String> routesList;
	private List<String> connectionsList;
	private List<String> headersList;

	private Element steps;

	public Document setProperties(Document document, String integrationId, List<TreeMap<String, String>> configurations) throws Exception {

		doc = document;

		setGeneralProperties(integrationId);

		for (TreeMap<String, String> configuration : configurations) {
			setFlowFromConfiguration(configuration);
		}

		return doc;

	}


	public Document setProperties(Document document, String type, TreeMap<String, String> configuration) throws Exception {

		doc = document;

		setGeneralProperties("live");

		setFlowFromConfiguration(configuration);

		return doc;

	}



	private void setGeneralProperties(String integrationId) {

		rootElement = doc.createElement("integrations");
		doc.appendChild(rootElement);

		integration = doc.createElement("integration");
		rootElement.appendChild(integration);

		Element id = doc.createElement("id");
		id.appendChild(doc.createTextNode(integrationId));
		integration.appendChild(id);

		flows = doc.createElement("flows");
		routes = doc.createElement("routes");
		routeConfigurations = doc.createElement("routeConfigurations");
		connections = doc.createElement("connections");
		headers = doc.createElement("headers");

		integration.appendChild(flows);
		integration.appendChild(routes);
		integration.appendChild(routeConfigurations);
		integration.appendChild(connections);
		integration.appendChild(headers);

		//List to ensure no double entries
		routesList = new ArrayList<String>();
		connectionsList = new ArrayList<String>();
		headersList = new ArrayList<String>();

	}


	private void setFlowFromConfiguration(TreeMap<String, String> configuration) throws Exception {

		flow = doc.createElement("flow");
		flows.appendChild(flow);

		addElement(configuration, flow, "id", "id");

		addElement(configuration, flow, "name", "flow.name");

		addElement(configuration, flow, "type", "flow.type");

		//addElement(configuration, flow, "offloading", "flow.offloading");

		addElement(configuration, flow, "version", "flow.version");

		addElement(configuration, flow, "autostart", "flow.autostart");

		addElement(configuration, flow, "assimblyHeaders", "flow.assimblyHeaders");

		addElement(configuration, flow, "parallelProcessing", "flow.v");

		addElement(configuration, flow, "maximumRedeliveries", "flow.maximumRedeliveries");

		addElement(configuration, flow, "redeliveryDelay", "flow.redeliveryDelay");

		addElement(configuration, flow, "logLevel", "flow.logLevel");

		addElement(configuration, flow, "notes", "flow.notes");

		Element components = doc.createElement("components");
		flow.appendChild(components);

		String[] confComponentsSplitted = StringUtils.split(configuration.get("flow.components"),",");
		for(String confComponentSplitted : confComponentsSplitted){
			Element flowComponentNode = doc.createElement("component");
			flowComponentNode.appendChild(doc.createTextNode(confComponentSplitted));
			components.appendChild(flowComponentNode);
		}

		steps = doc.createElement("steps");
		flow.appendChild(steps);

		//set steps
		setFlowSteps(configuration);

	}

	private void setFlowSteps(TreeMap<String, String> configuration) throws Exception {

		List<String> confUriKeyList = configuration.keySet().stream().filter(k -> k.endsWith("uri")).collect(Collectors.toList());

			for(String confUriKey : confUriKeyList){
				String confUri = configuration.get(confUriKey);
				String[] confUriKeySplitted = StringUtils.split(confUriKey,".");
				String confType = confUriKeySplitted[0];
				String confStepId = confUriKeySplitted[1];

				String confConnectionId = configuration.get(confType + "." + confStepId + ".connection.id");
				String confHeaderId = configuration.get(confType + "." + confStepId + ".header.id");
				String confRouteId = configuration.get(confType + "." + confStepId + ".route.id");

				setStepFromConfiguration(confType, confUri, confStepId, confConnectionId, confHeaderId, confRouteId, confRouteId, configuration);
			}
	}

	private void setStepFromConfiguration(String confType, String confUri, String confstepId, String confConnectionId, String confHeaderId, String confResponseId,String confRouteId, TreeMap<String, String> configuration) throws Exception {

		Element step = doc.createElement("step");
		Element uri = doc.createElement("uri");
		Element type = doc.createElement("type");
		Element stepId = doc.createElement("id");
		Element options = doc.createElement("options");
		Element connectionid = doc.createElement("connection_id");
		Element headerid = doc.createElement("header_id");
		Element responseId = doc.createElement("response_id");
		Element routeId = doc.createElement("route_id");

		steps.appendChild(step);

		String[] confUriSplitted = confUri.split("\\?");

		if(confUriSplitted.length<=1) {
			if(confUri.startsWith("sonicmq")) {
				confUri = confUri.replaceFirst("sonicmq.*:", "sonicmq:");
			}

			stepId.setTextContent(confstepId);
			type.setTextContent(confType);
			uri.setTextContent(confUri);

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

		if(confResponseId != null) {
			responseId.setTextContent(confResponseId);
			step.appendChild(responseId);
		}

		if(confRouteId != null) {
			routeId.setTextContent(confRouteId);
			step.appendChild(routeId);
			setRouteFromConfiguration(confRouteId, confType, configuration);
		}

		if(confConnectionId!=null) {
			connectionid.setTextContent(confConnectionId);
			step.appendChild(connectionid);
			setConnectionFromConfiguration(confConnectionId, confType, configuration);
		}

		if(confHeaderId!=null) {
			step.appendChild(headerid);
			headerid.setTextContent(confHeaderId);
			setHeaderFromConfiguration(confHeaderId, confType, configuration);
		}

	}


	private void setRouteFromConfiguration(String routeid, String type, TreeMap<String, String> configuration) throws Exception {

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

	private void setConnectionFromConfiguration(String connectionid, String type, TreeMap<String, String> configuration) throws Exception {

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

	private void addElement(TreeMap<String, String> configuration, Element parent, String name, String key) {
		String value = configuration.get(key);
		Element child = doc.createElement(name);
		child.appendChild(doc.createTextNode(value));
		parent.appendChild(child);
	}

	private Document getDocument(String xml) throws Exception {

        DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(xml.getBytes()));

        return document;

    }
}