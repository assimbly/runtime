package org.assimbly.integration.routes.errorhandler;

import org.apache.camel.*;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.LegacyDefaultErrorHandlerBuilder;
import org.assimbly.integration.processors.FailureProcessor;

import java.util.List;
import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ErrorHandler {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private DeadLetterChannelBuilder errorHandler;
	
	private int maximumRedeliveries;
	private int redeliveryDelay;
	private int maximumRedeliveryDelay;
	private int backOffMultiplier;

	private Processor failureProcessor;

	TreeMap<String, String> props;
	List<String> errorUriKeys;
	
	public ErrorHandler(DeadLetterChannelBuilder errorHandler, final TreeMap<String, String> props){
		this.errorHandler = errorHandler;
		this.props = props;		
	}
	
	
	public DeadLetterChannelBuilder configure() throws Exception {
		
		failureProcessor = new FailureProcessor(props);
		
		if (props.containsKey("flow.maximumRedeliveries")){
			String maximumRedeliveriesAsString = props.get("flow.maximumRedeliveries");
			if(StringUtils.isNumeric(maximumRedeliveriesAsString)) {
				maximumRedeliveries = Integer.parseInt(maximumRedeliveriesAsString);
			}else {
				maximumRedeliveries = 0;
			}
		}else {
			maximumRedeliveries = 0;
		}

		if (props.containsKey("flowredeliveryDelay")){
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

		if (props.containsKey("flow.backOffMultiplier")){
			String backOffMultiplierAsString = props.get("flow.backOffMultiplier");
			if(StringUtils.isNumeric(backOffMultiplierAsString)) {
				backOffMultiplier = Integer.parseInt(backOffMultiplierAsString);
			}else {
				backOffMultiplier = 0;
			}
		}else {
			backOffMultiplier = 0;
		}

		errorHandler.allowRedeliveryWhileStopping(false)
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
			.log(log);

		errorHandler.asyncDelayedRedelivery();

		return errorHandler;
		
	}
	

}