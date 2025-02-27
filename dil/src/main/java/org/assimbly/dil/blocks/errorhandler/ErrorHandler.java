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
	
	
	public DeadLetterChannelBuilder configure() throws Exception {

		int backOffMultiplier = 0;
		int maximumRedeliveries = 0;
		int redeliveryDelay = 30000;
		int maximumRedeliveryDelay = 60000;

		Processor failureProcessor = new FailureProcessor();


		if (props.containsKey("flow.maximumRedeliveries")) {
			String maximumRedeliveriesAsString = props.get("flow.maximumRedeliveries");
			if (StringUtils.isNumeric(maximumRedeliveriesAsString)) {
				maximumRedeliveries = Integer.parseInt(maximumRedeliveriesAsString);
			}
		}

		if (props.containsKey("flowredeliveryDelay")){
			String redeliveryDelayAsString = props.get("flow.redeliveryDelay");
			if(StringUtils.isNumeric(redeliveryDelayAsString)) {
				redeliveryDelay = Integer.parseInt(redeliveryDelayAsString);
				maximumRedeliveryDelay = redeliveryDelay * 10;
			}
		}

		if (props.containsKey("flow.backOffMultiplier")){
			String backOffMultiplierAsString = props.get("flow.backOffMultiplier");
			if(StringUtils.isNumeric(backOffMultiplierAsString)) {
				backOffMultiplier = Integer.parseInt(backOffMultiplierAsString);
			}
		}

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

}