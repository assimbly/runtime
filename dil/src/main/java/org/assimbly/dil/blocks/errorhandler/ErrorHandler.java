package org.assimbly.dil.blocks.errorhandler;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.processors.FailureProcessor;
import java.util.TreeMap;


public class ErrorHandler {

	private final DeadLetterChannelBuilder deadLetterChannelBuilder;

	private final TreeMap<String, String> props;

	private final String flowId;

	public ErrorHandler(DeadLetterChannelBuilder deadLetterChannelBuilder, final TreeMap<String, String> props, String flowId){
		this.deadLetterChannelBuilder = deadLetterChannelBuilder;
		this.props = props;
		this.flowId = flowId;
	}
	
	public DeadLetterChannelBuilder configure() {

		int maximumRedeliveries = getErrorHandlerOption("maximumRedeliveries", 0);
		int redeliveryDelay = getErrorHandlerOption("redeliveryDelay", 0);
		int maximumRedeliveryDelay = getErrorHandlerOption("maximumRedeliveryDelay", redeliveryDelay * 10);
		int backOffMultiplier = getErrorHandlerOption("backOffMultiplier", 0);

		Processor failureProcessor = new FailureProcessor();

		deadLetterChannelBuilder.allowRedeliveryWhileStopping(false)
			.asyncDelayedRedelivery()
			.maximumRedeliveries(maximumRedeliveries)
			.redeliveryDelay(redeliveryDelay)
			.maximumRedeliveryDelay(maximumRedeliveryDelay)
			.backOffMultiplier(backOffMultiplier)
			.retriesExhaustedLogLevel(LoggingLevel.ERROR)
			.retryAttemptedLogLevel(LoggingLevel.DEBUG)
			.log("org.assimbly.errorhandler." + flowId)
			.logRetryStackTrace(false)
			.logStackTrace(true)
			.logHandled(true)
			.logExhausted(true)
			.logExhaustedMessageBody(true)
			.logExhaustedMessageHistory(true);

		deadLetterChannelBuilder.onExceptionOccurred(failureProcessor);

		deadLetterChannelBuilder.asyncDelayedRedelivery();

		return deadLetterChannelBuilder;
		
	}

	private int getErrorHandlerOption(String option, int defaultValue) {

		if (props.containsKey("flow." + option)){
			String errorHandlerOption = props.get("flow." + option);
			if(StringUtils.isNumeric(errorHandlerOption)) {
				return Integer.parseInt(errorHandlerOption);
			}
		}

		return defaultValue;

	}

}