package org.assimbly.connector.configuration;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.*;
import java.util.stream.Collectors;

//This class marshalls (converts) the Java treemap object to XML
public class Marshall {

	private Document doc;
	private Element rootElement;
	private Element flows;
	private Element services;
	private Element headers;
	private Element flow;
	private Element connector;

	private List<String> servicesList;
	private List<String> headersList;

	private Element endpoints;

	public Document setProperties(Document document, String connectorId, List<TreeMap<String, String>> configurations) throws Exception {

		doc = document;

		setGeneralProperties(connectorId);

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
}