package org.assimbly.integration.loader;

import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.Predicate;
import org.apache.camel.Processor;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.PredicateBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.dil.blocks.processors.ConvertProcessor;
import org.assimbly.dil.blocks.processors.FailureProcessor;
import org.assimbly.dil.blocks.processors.HeadersProcessor;
import org.assimbly.util.IntegrationUtil;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;


public class ConnectorRoute extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private TreeMap<String, String> props;

	private CamelContext context;
	private ExtendedCamelContext extendedCamelContext;

	private DeadLetterChannelBuilder routeErrorHandler;

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
		super();
		this.props = props;
	}

	public ConnectorRoute() {
		super();
	}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		context = getContext();

		decryptedProperties = decryptProperties(props);

		flowId = props.get("id");

		setFlowSettings();

		setStepKeys();
		
		setProcessors();
		
		setErrorHandler();
	
		setFromSteps();
		
		setToSteps();
		
		setResponseSteps();

	}

	private void setStepKeys() {
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

		if (this.props.containsKey(this.errorUriKeys.get(0))){
			String errorUri = this.props.get(this.errorUriKeys.get(0));
			errorHandler(deadLetterChannel(errorUri));

			routeErrorHandler = deadLetterChannel(errorUri);
		}else{
			routeErrorHandler = deadLetterChannel("log:org.assimbly.integration.routes.ConnectorRoute?level=ERROR");
		}

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props);

		routeErrorHandler = errorHandler.configure();
				
	}

	private void setFromSteps() throws Exception {

		for(String onrampUriKey : onrampUriKeys){

			String stepId = StringUtils.substringBetween(onrampUriKey, "from.", ".uri");
			String headerId = props.get("from." + stepId + ".header.id");
			String routeId = props.get("from." + stepId + ".route.id");

			String uri = decryptValue(props.get(onrampUriKey));
			String fromUri = props.get("from." + stepId + ".uri");			
			
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
				String xml = props.get("from." + stepId + ".route");
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
				.id("headerProcessor" + flowId + "-" + stepId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + stepId)
				.choice()
					.when(hasRoute)
						.to("direct:flow=" + flowId + "route=" + flowId + "-" + stepId + "-" + routeId)
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
				.routeId(flowId + "-" + stepId).description("from");
					
		}

	}
	
	private void setToSteps()  throws Exception {

		//The Connector To Camel route (offramp)
		for (String offrampUriKey : offrampUriKeys)
		{

			String uri = decryptValue(props.get(offrampUriKey));
			String offrampUri = offrampUriList[index++];
			String stepId = StringUtils.substringBetween(offrampUriKey, "to.", ".uri");
			String headerId = props.get("to." + stepId + ".header.id");
			String responseId = props.get("to." + stepId + ".response.id");
			String routeId = props.get("to." + stepId + ".route.id");

			Predicate hasAssimblyHeaders = PredicateBuilder.constant(assimblyHeaders);
			Predicate hasResponseStep = PredicateBuilder.constant(responseId != null && !responseId.isEmpty());			
			Predicate hasRoute = PredicateBuilder.constant(false);

			boolean hasDynamicStep = false;
			if(uri.contains("${")) {
				hasDynamicStep = true;
			}
			
			if(routeId!=null && !routeId.isEmpty()){
				hasRoute = PredicateBuilder.constant(true);
				String xml = props.get("to." + stepId + ".route");
				updateRoute(xml);
			}

			if(hasDynamicStep) {

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
				.id("headerProcessor" + flowId + "-" + stepId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + stepId)
				.choice()
				.when(hasRoute)
				.to("direct:flow=" + flowId + "route=" + flowId + "-" + stepId + "-" + routeId)
				.end()
				.log(hasResponseStep.toString())
				.choice()
					.when(hasResponseStep)
						.choice()
							.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
								.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
								.pollEnrich().simple(uri).timeout(20000)
							.endChoice()
						.otherwise()
							.toD(uri)
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
							.to("direct:flow=" + flowId + "step=" + responseId)
						.endChoice()
					.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
						.pollEnrich().simple(uri).timeout(20000)
						.endChoice()
					.otherwise()
						.toD(uri)
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.end()
				.routeId(flowId + "-" + stepId).description("to");
				
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
				.id("headerProcessor" + flowId + "-" + stepId)
				.process(convertProcessor)
				.id("convertProcessor" + flowId + "-" + stepId)
				.choice()
				.when(hasRoute)
				.to("direct:flow=" + flowId + "route=" + flowId + "-" + stepId + "-" + routeId)
				.end()
				.log(hasResponseStep.toString())
				.choice()
					.when(hasResponseStep)
						.choice()
							.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
								.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
								.pollEnrich().simple(uri).timeout(20000)
							.endChoice()
						.otherwise()
							.to(uri)
							.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
							.to("direct:flow=" + flowId + "step=" + responseId)
						.endChoice()
					.when(header("Enrich").convertToString().isEqualToIgnoreCase("to"))
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|ENRICH?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
						.pollEnrich().simple(uri).timeout(20000)
						.endChoice()
					.otherwise()
						.to(uri)
						.to("log:Flow=" + flowName + "|ID=" +  flowId + "|SEND?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed")
					.end()
				.routeId(flowId + "-" + stepId).description("to");
				
			}
				
		}

	}
	
	private void setResponseSteps() throws Exception {
		//The Connector Response Camel route (response)
		for(String responseUriKey : responseUriKeys){

			String uri = props.get(responseUriKey);
			String stepId = StringUtils.substringBetween(responseUriKey, "response.", ".uri");
			String headerId = props.get("response." + stepId + ".header.id");
			String responseId = props.get("response." + stepId + ".response.id");
			String routeId = props.get("response." + stepId + ".route.id");

			Predicate hasAssimblyHeaders = PredicateBuilder.constant(assimblyHeaders);
			Predicate hasRoute = PredicateBuilder.constant(false);

			if(routeId!=null && !routeId.isEmpty()){
				hasRoute = PredicateBuilder.constant(true);
				String xml = props.get("response." + stepId + ".route");
				updateRoute(xml);
			}

			from("direct:flow=" + flowId + "step=" + responseId)
					.errorHandler(routeErrorHandler)
					.setHeader("AssimblyHeaderId", constant(headerId))
					.choice()
						.when(hasAssimblyHeaders)
							.setHeader("AssimblyFlowID", constant(flowId))
							.setHeader("AssimblyResponse", constant(props.get("response." + stepId + ".uri")))
							.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
							.setHeader("AssimblyResponseTimestamp", groovy("new Date().getTime()"))
					.end()
					.process(headerProcessor)
					.id("headerProcessor" + flowId + "-" + stepId)
					.process(convertProcessor)
					.id("convertProcessor" + flowId + "-" + stepId)
					.choice()
						.when(hasRoute)
						.to("direct:flow=" + flowId + "route=" + flowId + "-" + stepId + "-" + routeId)
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
					.routeId(flowId + "-" + stepId).description("response");
		}

	}	
	
	//create a string array for all of a specific stepType
	private List<String> getUriKeys(String stepType) {

		List<String> keys = new ArrayList<>();

		for(String prop : props.keySet()){
			if(prop.startsWith(stepType) && prop.endsWith("uri")){
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

	private String decryptValue(String value){

		EncryptableProperties encryptionProperties = (EncryptableProperties) ((PropertiesComponent) getContext().getPropertiesComponent()).getInitialProperties();
		String[] encryptedList = StringUtils.substringsBetween(value, "ENC(", ")");

		String decryptedValue = value;
		if(encryptedList !=null && encryptedList.length>0){
			for (String encrypted: encryptedList) {
				encryptionProperties.setProperty("temp","ENC(" + encrypted + ")");
				String decrypted = encryptionProperties.getProperty("temp");
				decryptedValue = StringUtils.replace(value, "ENC(" + encrypted + ")", decrypted);
			}
		}

		return decryptedValue;

	}

}