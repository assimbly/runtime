package org.assimbly.integration.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.builder.LegacyDefaultErrorHandlerBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.integration.processors.ConvertProcessor;
import org.assimbly.integration.processors.FailureProcessor;
import org.assimbly.integration.processors.HeadersProcessor;
import org.assimbly.integration.routes.errorhandler.ErrorHandler;
import org.assimbly.util.IntegrationUtil;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectorRoute extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private TreeMap<String, String> props;

	private CamelContext context;
	private ExtendedCamelContext extendedCamelContext;

	private LegacyDefaultErrorHandlerBuilder routeErrorHandler;

	private String flowId;
	private String flowName;
	private boolean parallelProcessing;
	private boolean assimblyHeaders;
	private String logLevelAsString;

	private Processor headerProcessor;
	private Processor convertProcessor;
	
	private List<String> onrampUriKeys;
	private List<String> offrampUriKeys;
	private List<String> responseUriKeys;
	private List<String> errorUriKeys;
	private String[] offrampUriList;

	private int index = 0;

	private EncryptableProperties decryptedProperties;

	private FailureProcessor failureProcessor;
	
	
	public ConnectorRoute(final TreeMap<String, String> props){
		this.props = props;
	}

	public ConnectorRoute() {}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		context = getContext();

		decryptedProperties = decryptProperties(props);

		flowId = props.get("id");

		setFlowSettings();

		setEndpointKeys();
		
		setProcessors();
		
		setErrorHandler();
	
		setFromEndpoints();
		
		setToEndpoints();
		
		setResponseEndpoints();

	}

	private void setEndpointKeys() {
		errorUriKeys = getUriKeys("error");
		onrampUriKeys = getUriKeys("from");
		offrampUriKeys = getUriKeys("to");
		responseUriKeys = getUriKeys("response");
	}
	
	private void setProcessors() {
		headerProcessor = new HeadersProcessor(props);
		failureProcessor = new FailureProcessor(props);
		convertProcessor = new ConvertProcessor();
	}
	
	private void setFlowSettings() {

		if (this.props.containsKey("flow.parallelProcessing")){
			String parallelProcessingAsString = props.get("flow.parallelProcessing");
			if(parallelProcessingAsString.equalsIgnoreCase("true")) {
				parallelProcessing = true;
			}else {
				parallelProcessing = false;
			}
		}else {
			parallelProcessing = true;
		}

		if (this.props.containsKey("flow.assimblyHeaders")){
			String assimblyHeadersAsString = props.get("flow.assimblyHeaders");
			if(assimblyHeadersAsString.equalsIgnoreCase("true")) {
				assimblyHeaders = true;
			}else {
				assimblyHeaders = false;
			}
		}else {
			assimblyHeaders = true;
		}

		flowName = props.get("flow.name");
		if (this.props.containsKey("flow.logLevel")){
			logLevelAsString = props.get("flow.logLevel");
		}else {
			logLevelAsString = "OFF";
		}
		
		String offrampUri = props.get("offramp.uri.list");

		offrampUriList = offrampUri.split(",");

	}
	
	private void setErrorHandler() throws Exception {

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props);
		routeErrorHandler = errorHandler.configure();	
				
	}

	private void setFromEndpoints() throws Exception {

		for(String onrampUriKey : onrampUriKeys){

			String endpointId = StringUtils.substringBetween(onrampUriKey, "from.", ".uri");
			String headerId = props.get("from." + endpointId + ".header.id");
			String routeId = props.get("from." + endpointId + ".route.id");

			String uri = DecryptValue(props.get(onrampUriKey));
			String fromUri = props.get("from." + endpointId + ".uri");			
			
			Predicate hasParallelProcessing = PredicateBuilder.constant(parallelProcessing);
			Predicate hasAssimblyHeaders = PredicateBuilder.constant(assimblyHeaders);

			Predicate hasOneDestination = PredicateBuilder.constant(false);
			if(offrampUriList.length==1){
				hasOneDestination = PredicateBuilder.constant(true);
			}

			//this logic should be moved to a separate method getting config from service)
			if(uri.startsWith("rest")){
				String restHostAndPort = StringUtils.substringBetween(uri,"host=","&");
				if(restHostAndPort != null && !restHostAndPort.isEmpty()){
					String restHost = restHostAndPort.split(":")[0];
					String restPort = restHostAndPort.split(":")[1];
					restConfiguration().host(restHost).port(restPort).enableCORS(true);
				}
			}

			Predicate hasRoute = PredicateBuilder.constant(false);
			if(routeId!=null && !routeId.isEmpty()){
				hasRoute = PredicateBuilder.constant(true);
				String xml = props.get("from." + endpointId + ".route");
				updateRoute(xml);
			}
			
			from(uri)
				.errorHandler(routeErrorHandler)
				.setHeader("AssimblyHeaderId", constant(headerId))
				.choice()
					.when(hasAssimblyHeaders)
						.setHeader("AssimblyFlowID", constant(flowId))
						.setHeader("AssimblyFrom", constant(fromUri))
						.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
						.setHeader("AssimblyFromTimestamp", groovy("new Date().getTime()"))
				.end()
				.to("log:Flow=" + flowName + "|ID=" +  flowId + "|RECEIVED?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
				.process(headerProcessor)
				.id("headerProcessor" + flowId + "-" + endpointId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + endpointId)
				.choice()
					.when(hasRoute)
						.to("direct:flow=" + flowId + "route=" + flowId + "-" + endpointId + "-" + routeId)
				.end()
				.choice()
					.when(hasOneDestination)
						.to(offrampUriList)
					.endChoice()
					.when(hasParallelProcessing)
						.to(offrampUriList)
					.endChoice()
					.otherwise()
						.multicast()
						.shareUnitOfWork()
						.parallelProcessing()
						.to(offrampUriList)
				.end()
				.routeId(flowId + "-" + endpointId).description("from");
					
		}

	}
	
	private void setToEndpoints()  throws Exception {

		//The Connector To Camel route (offramp)
		for (String offrampUriKey : offrampUriKeys)
		{

			String uri = DecryptValue(props.get(offrampUriKey));
			String offrampUri = offrampUriList[index++];
			String endpointId = StringUtils.substringBetween(offrampUriKey, "to.", ".uri");
			String headerId = props.get("to." + endpointId + ".header.id");
			String responseId = props.get("to." + endpointId + ".response.id");
			String routeId = props.get("to." + endpointId + ".route.id");

			Predicate hasAssimblyHeaders = PredicateBuilder.constant(assimblyHeaders);
			Predicate hasResponseEndpoint = PredicateBuilder.constant(responseId != null && !responseId.isEmpty());			
			Predicate hasRoute = PredicateBuilder.constant(false);

			boolean hasDynamicEndpoint = false;
			if(uri.contains("${")) {
				hasDynamicEndpoint = true;
			}
			
			if(routeId!=null && !routeId.isEmpty()){
				hasRoute = PredicateBuilder.constant(true);
				String xml = props.get("to." + endpointId + ".route");
				updateRoute(xml);
			}

			if(hasDynamicEndpoint) {

				from(offrampUri)
				.errorHandler(routeErrorHandler)
				.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SENDING?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
				.setHeader("AssimblyHeaderId", constant(headerId))
				.choice()
					.when(hasAssimblyHeaders)
						.setHeader("AssimblyTo", constant(uri))
						.setHeader("AssimblyToTimestamp", groovy("new Date().getTime()"))
				.end()
				.process(headerProcessor)
				.id("headerProcessor" + flowId + "-" + endpointId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + endpointId)
				.choice()
				.when(hasRoute)
				.to("direct:flow=" + flowId + "route=" + flowId + "-" + endpointId + "-" + routeId)
				.end()
				.log(hasResponseEndpoint.toString())
				.choice()
					.when(hasResponseEndpoint)
						.choice()
							.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
								.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
								.pollEnrich().simple(uri).timeout(20000)
							.endChoice()
						.otherwise()
							.toD(uri)
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
							.to("direct:flow=" + flowId + "endpoint=" + responseId)
						.endChoice()
					.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
						.pollEnrich().simple(uri).timeout(20000)
						.endChoice()
					.otherwise()
						.toD(uri)
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.end()
				.routeId(flowId + "-" + endpointId).description("to");
				
			}else {			

				from(offrampUri)
				.errorHandler(routeErrorHandler)
				.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SENDING?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
				.setHeader("AssimblyHeaderId", constant(headerId))
				.choice()
					.when(hasAssimblyHeaders)
						.setHeader("AssimblyTo", constant(uri))
						.setHeader("AssimblyToTimestamp", groovy("new Date().getTime()"))
				.end()
				.process(headerProcessor)
				.id("headerProcessor" + flowId + "-" + endpointId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + endpointId)
				.choice()
				.when(hasRoute)
				.to("direct:flow=" + flowId + "route=" + flowId + "-" + endpointId + "-" + routeId)
				.end()
				.log(hasResponseEndpoint.toString())
				.choice()
					.when(hasResponseEndpoint)
						.choice()
							.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
								.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
								.pollEnrich().simple(uri).timeout(20000)
							.endChoice()
						.otherwise()
							.to(uri)
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
							.to("direct:flow=" + flowId + "endpoint=" + responseId)
						.endChoice()
					.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
						.pollEnrich().simple(uri).timeout(20000)
						.endChoice()
					.otherwise()
						.to(uri)
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.end()
				.routeId(flowId + "-" + endpointId).description("to");
				
			}
				
		}

	}
	
	private void setResponseEndpoints() throws Exception {
		//The Connector Response Camel route (response)
		for(String responseUriKey : responseUriKeys){

			String uri = props.get(responseUriKey);
			String endpointId = StringUtils.substringBetween(responseUriKey, "response.", ".uri");
			String headerId = props.get("response." + endpointId + ".header.id");
			String responseId = props.get("response." + endpointId + ".response.id");
			String routeId = props.get("response." + endpointId + ".route.id");

			Predicate hasAssimblyHeaders = PredicateBuilder.constant(assimblyHeaders);
			Predicate hasRoute = PredicateBuilder.constant(false);

			if(routeId!=null && !routeId.isEmpty()){
				hasRoute = PredicateBuilder.constant(true);
				String xml = props.get("response." + endpointId + ".route");
				updateRoute(xml);
			}

			from("direct:flow=" + flowId + "endpoint=" + responseId)
					.errorHandler(routeErrorHandler)
					.setHeader("AssimblyHeaderId", constant(headerId))
					.choice()
						.when(hasAssimblyHeaders)
							.setHeader("AssimblyFlowID", constant(flowId))
							.setHeader("AssimblyResponse", constant(props.get("response." + endpointId + ".uri")))
							.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
							.setHeader("AssimblyResponseTimestamp", groovy("new Date().getTime()"))
					.end()
					.process(headerProcessor)
					.id("headerProcessor" + flowId + "-" + endpointId)
					.process(convertProcessor)
					.id("convertProcessor" + flowId + "-" + endpointId)
					.choice()
						.when(hasRoute)
						.to("direct:flow=" + flowId + "route=" + flowId + "-" + endpointId + "-" + routeId)
					.end()
					.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SENDINGRESPONSE?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.choice()
						.when(header("Enrich").convertToString().isEqualToIgnoreCase("response"))
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
							.pollEnrich().simple(uri).timeout(20000)
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SENDRESPONSE?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
						.endChoice()
						.otherwise()
							.toD(uri)
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SENDRESPONSE?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.end()
					.routeId(flowId + "-" + endpointId).description("response");
		}

	}	
	
	//create a string array for all of a specific endpointType
	private List<String> getUriKeys(String endpointType) {

		List<String> keys = new ArrayList<>();

		for(String prop : props.keySet()){
			if(prop.startsWith(endpointType) && prop.endsWith("uri")){
				keys.add(prop);
			}
		}

		return keys;

	}

	//adds XML route
	private void updateRoute(String route) throws Exception {
		extendedCamelContext = context.adapt(ExtendedCamelContext.class);				
		RoutesLoader loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);		
		loader.updateRoutes(resource);
	}

	//
	private EncryptableProperties decryptProperties(TreeMap<String, String> properties) {
		EncryptableProperties decryptedProperties = (EncryptableProperties) ((PropertiesComponent) getContext().getPropertiesComponent()).getInitialProperties();
		decryptedProperties.putAll(properties);
		return decryptedProperties;
	}

	private String DecryptValue(String value){

		EncryptableProperties encryptionProperties = (EncryptableProperties) ((PropertiesComponent) getContext().getPropertiesComponent()).getInitialProperties();
		String[] encryptedList = StringUtils.substringsBetween(value, "ENC(", ")");

		if(encryptedList !=null && encryptedList.length>0){
			for (String encrypted: encryptedList) {
				encryptionProperties.setProperty("temp","ENC(" + encrypted + ")");
				String decrypted = encryptionProperties.getProperty("temp");
				value = StringUtils.replace(value, "ENC(" + encrypted + ")",decrypted);
			}
		}

		return value;

	}

}