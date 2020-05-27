package org.assimbly.connector.routes;

import java.util.Map;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.connector.processors.ConvertProcessor;
import org.assimbly.connector.processors.FailureProcessor;
import org.assimbly.connector.processors.HeadersProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultRoute extends RouteBuilder {
	
	Map<String, String> props;
	private DefaultErrorHandlerBuilder routeErrorHandler;
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.connector.routes.DefaultRoute");
	private String flowId;
	private int maximumRedeliveries;
	private int redeliveryDelay;
	private int maximumRedeliveryDelay;
	private int backOffMultiplier;
	private String logMessage;
	
	private String[] offrampUriList;
	
	public DefaultRoute(final Map<String, String> props){
		this.props = props;
	}

	public DefaultRoute() {}

	public interface FailureProcessorListener {
		 public void onFailure();
  	}
	
	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring default route");
		
		getContext().setTracing(true); 
		
		Processor headerProcessor = new HeadersProcessor(props);
		Processor failureProcessor = new FailureProcessor();
		Processor convertProcessor = new ConvertProcessor();

		offrampUriList = getOfframpUriList();
		flowId = props.get("id");
		
		if (this.props.containsKey("flow.maximumRedeliveries")){
			String maximumRedeliveriesAsString = props.get("flow.maximumRedeliveries");
			if(StringUtils.isNumeric(maximumRedeliveriesAsString)) {
				maximumRedeliveries = Integer.parseInt(maximumRedeliveriesAsString);
			}else {
				maximumRedeliveries = 0;
			}
		}else {
			maximumRedeliveries = 0;
		}
		
		if (this.props.containsKey("flowredeliveryDelay")){
			String RedeliveryDelayAsString = props.get("flow.redeliveryDelay");
			if(StringUtils.isNumeric(RedeliveryDelayAsString)) {
				redeliveryDelay = Integer.parseInt(RedeliveryDelayAsString);
				maximumRedeliveryDelay = redeliveryDelay * 10;
			}else {
				redeliveryDelay = 3000;
				maximumRedeliveryDelay = 60000;
			}
		}else {
			redeliveryDelay = 3000;
			maximumRedeliveryDelay = 60000;
		}
		
		if (this.props.containsKey("flow.backOffMultiplier")){
			String backOffMultiplierAsString = props.get("flow.backOffMultiplier");
			if(StringUtils.isNumeric(backOffMultiplierAsString)) {
				backOffMultiplier = Integer.parseInt(backOffMultiplierAsString);
			}else {
				backOffMultiplier = 0;
			}
		}else {
			backOffMultiplier = 0;
		}
		
		if (this.props.containsKey("flow.logLevel")){
			String logLevelAsString = props.get("flow.logLevel");
			String routeName = props.get("flow.name");
			logMessage = "log:RouteName." + routeName + "?level=" + logLevelAsString + "&showAll=true&multiline=true&style=Fixed";
		}else {
			String routeName = props.get("flow.name");
			logMessage = "log:RouteName." + routeName + "level=OFF&showAll=true&multiline=true&style=Fixed";
		}		
		
		if (this.props.containsKey("error.uri")){
			routeErrorHandler = deadLetterChannel(props.get("error.uri"))		
			.allowRedeliveryWhileStopping(false)
			.asyncDelayedRedelivery()			
			.maximumRedeliveries(maximumRedeliveries)
			.redeliveryDelay(redeliveryDelay)
			.maximumRedeliveryDelay(maximumRedeliveryDelay)			
			.backOffMultiplier(backOffMultiplier)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.log(log)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true);					
		}
		else{
			routeErrorHandler = defaultErrorHandler()
			.allowRedeliveryWhileStopping(false)
			.asyncDelayedRedelivery()
			.maximumRedeliveries(maximumRedeliveries)
			.redeliveryDelay(redeliveryDelay)
			.maximumRedeliveryDelay(maximumRedeliveryDelay)
			.backOffMultiplier(backOffMultiplier)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.onExceptionOccurred(failureProcessor)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageHistory(true)
			.log(logger);
		}
		
		routeErrorHandler.setAsyncDelayedRedelivery(true);
		
		
		
		//The default Camel route (onramp)
		from(props.get("from.uri"))	
			.errorHandler(routeErrorHandler)	
			.setHeader("AssimblyFlowID", constant(flowId))
			.setHeader("AssimblyHeaderId", constant(props.get("from.header.id")))
			.setHeader("AssimblyFrom", constant(props.get("from.uri")))
			.to(logMessage)
			.process(headerProcessor)
			.id("headerProcessor" + flowId)
			.multicast()
			.shareUnitOfWork()
			.parallelProcessing()
			.to(offrampUriList)
        	.routeId(flowId);        			
        
        //The default Camel route (offramp)		
		for (String offrampUri : offrampUriList) 
		{	

			String endpointId = StringUtils.substringAfterLast(offrampUri, "endpoint=");			
			String toUri = props.get("to." + endpointId + ".uri");
			String headerId = props.get("to." + endpointId + ".header.id");
			
			from(offrampUri)			
			.errorHandler(routeErrorHandler)
			.setHeader("AssimblyHeaderId", constant(headerId))
			.setHeader("AssimblyTo", constant(toUri))
			.process(headerProcessor)
			.id("headerProcessor" + flowId + "-" + endpointId)
			.process(convertProcessor)
			.id("convertProcessor" + flowId + "-" + endpointId)
     	    .choice()    		    
	  		    .when(header("ReplyTo").convertToString().contains(":"))
	  		    	.to(logMessage)
			    	.to(toUri)			    	
			    	.toD("${header.ReplyTo}")
	  		    .when(header("ReplyTo").isNotNull())
	  		    	.to(logMessage)
	  		    	.to(toUri)
	  		    	.toD("vm://${header.ReplyTo}")
	  		    .otherwise()
	  		    	.to(toUri)
	  		 .end()
	  		 .to(logMessage)
	  		 .routeId(flowId + "-" + endpointId);			
		   
		}
		
	}
	
	//create a string array for all offramps
	private String[] getOfframpUriList() {
		
		String offrampUri = props.get("offramp.uri.list");
		
		return offrampUri.split(",");
		
	}	

}
