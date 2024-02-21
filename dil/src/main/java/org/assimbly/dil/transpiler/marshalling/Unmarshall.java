package org.assimbly.dil.transpiler.marshalling;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.objects.XObject;
import org.assimbly.dil.transpiler.marshalling.core.*;
import org.assimbly.util.DependencyUtil;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.util.*;
import java.util.stream.IntStream;

// This class unmarshalls an XML file into a Java treemap object
// The XML file must be in DIL (Data Integration Language) format
public class Unmarshall {

	private Document doc;
	private TreeMap<String, String> properties;
	private XMLConfiguration conf;
	private String flowId;
	private String flowDependencies;
	CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
	XPathFactory xf = new XPathFactoryImpl();

	public TreeMap<String, String> getProperties(XMLConfiguration configuration, String flowId) throws Exception{

		this.flowId = flowId;
		doc = configuration.getDocument();
		conf = configuration;
		properties = new TreeMap<String, String>();

		setFlows();

		setSteps();

		return properties;

	}

	private String getFlowSelector() throws Exception{

		String selector = "1";

		Integer numberOfFlows = Integer.parseInt(evaluateXpath("count(//flows/flow)"));

		if(numberOfFlows > 1){

			//originalFlowId is the flowId as parameter
			String originalFlowId = flowId;
			selector = "id='" + originalFlowId + "'";

			flowId = evaluateXpath("//flows/flow[" + selector + "]/id");

			if(!originalFlowId.equals(flowId)) {
				ConfigurationException configurationException = new ConfigurationException("The flow ID " + originalFlowId + " doesn't exists in XML Configuration");
				configurationException.initCause(new Throwable("The flow ID  " + originalFlowId + " doesn't exists in XML Configuration"));
				throw configurationException;
			}
		}else{
			flowId = evaluateXpath("//flows/flow[" + selector + "]/id");
		}

		return selector;

	}

	public void setFlows() throws Exception {

		String flowSelector = getFlowSelector();
		String flowName = evaluateXpath("//flows/flow[" + flowSelector + "]/name");
		String flowType = evaluateXpath("//flows/flow[" + flowSelector + "]/type");
		String flowVersion = evaluateXpath("//flows/flow[" + flowSelector + "]/version");
		String flowLogLevel = evaluateXpath("//flows/flow[" + flowSelector + "]/options/logLevel");

		String integrationXPath = "integrations/integration/flows/flow[" + flowSelector + "]";

		String[] integrationProporties = conf.getStringArray(integrationXPath);

		if(integrationProporties.length > 0){
			for(String integrationProperty : integrationProporties){
				properties.put(integrationProperty.substring(integrationXPath.length() + 1), conf.getString(integrationProperty));
			}
		}

		String environment = evaluateXpath("//integrations/integration[1]/options/environment");

		String[] dependencies = conf.getStringArray("integrations/integration/flows/flow[" + flowSelector + "]/dependencies/dependency");

		for(String dependency : dependencies){
			if(flowDependencies==null){
				flowDependencies = dependency;
			}else{
				flowDependencies = flowDependencies + "," + dependency;
			}
		}

		properties.put("id",flowId);
		properties.put("environment",environment);

		properties.put("flow.name",flowName);
		properties.put("flow.type",flowType);
		properties.put("flow.version",flowVersion);
		properties.put("flow.dependencies",flowDependencies);
		properties.put("flow.logLevel",flowLogLevel);

	}

	private void setSteps() throws Exception {

		String[] steps = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step/id");

		//set all steps in parallel
		IntStream.range(1, steps.length + 1)
				.parallel()
				.forEach(index -> {
					try {

						String stepId = evaluateXpath2("/dil/integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/id");
						String type = evaluateXpath2("/dil/integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/type");

						setUri(index, stepId, type);
						setBlocks(index, stepId, type);

					} catch (Exception e) {
						e.printStackTrace();
					}
				});
	}

	private void setUri(int index, String stepId, String type) throws Exception {

		String baseUri = evaluateXpath2("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		String uri = baseUri;
		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/options");
		String options = getOptions(optionProperties);

		if (options != null && !options.isEmpty()) {
			uri = baseUri + "?" + options;
		}

		if(uri != null){
			properties.put(type + "." + stepId + ".uri", uri);
		}
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


	private void setBlocks(int index, String stepId, String type) throws Exception {

		String blocksXPath = "/dil/integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/blocks/block/";
		List<String> routeTemplateList = Arrays.asList("source", "action", "router", "sink", "message", "script");

		String messageId = evaluateXpath2(blocksXPath + "options/message_id");
		String connectionId = evaluateXpath2(blocksXPath + "options/connection_id");
		String routeId = evaluateXpath2(blocksXPath + "options/route_id");
		String routeConfigurationId = evaluateXpath2(blocksXPath + "options/routeconfiguration_id");

		if(messageId != null  && !messageId.isEmpty())
			properties =  new Message(properties, conf).setHeader(type, stepId, messageId);

		if(connectionId != null && !connectionId.isEmpty())
			properties =  new Connection(properties, conf).setConnection(type, stepId, connectionId);

		if(routeId != null && !routeId.isEmpty())
			properties =  new Route(properties, conf, doc).setRoute(type, flowId, stepId, routeId);

		if(routeConfigurationId != null  && !routeConfigurationId.isEmpty())
			properties =  new RouteConfiguration(properties, conf).setRouteConfiguration(type, stepId, routeConfigurationId);

		if(routeTemplateList.contains(type))
			setRouteTemplate(index, stepId, type);
	}

	private void setRouteTemplate(int index, String stepId, String type) throws Exception {

		String stepXPath = "integrations/integration/flows/flow[id='" + flowId + "']/steps/step[" + index + "]/";
		String[] links = conf.getStringArray("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/links/link/id");
		String baseUri = evaluateXpath2("//flows/flow[id='" + flowId + "']/steps/step[" + index + "]/uri");
		List<String> optionProperties = IntegrationUtil.getXMLParameters(conf, stepXPath + "options");
		String options = getOptions(optionProperties);

		if(baseUri.startsWith("blocks") || baseUri.startsWith("component")){
			properties =  new RouteTemplate(properties, conf).setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);
		}else{
			String scheme = StringUtils.substringBefore(baseUri,":");
			//if(DependencyUtil.PredefinedBlocks.hasBlock(scheme)){
			//	baseUri = "block:" + baseUri;
			//};
			properties =  new RouteTemplate(properties, conf).setRouteTemplate(type,flowId, stepId, optionProperties, links, stepXPath, baseUri, options);
		}

	}

	private String evaluateXpath(String xpath) throws TransformerException, XPathExpressionException {
		XObject xObject = cachedXPathAPI.eval(doc, xpath);
		return xObject.xstr(cachedXPathAPI.getXPathContext()).toString();
	}

	private String evaluateXpath2(String xpath) throws TransformerException, XPathExpressionException {
		XPathExpression xp = xf.newXPath().compile(xpath);
		return xp.evaluate(doc);
	}

}