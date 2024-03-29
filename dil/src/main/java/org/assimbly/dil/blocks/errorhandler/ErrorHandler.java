package org.assimbly.dil.blocks.errorhandler;

import org.apache.camel.*;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.assimbly.dil.blocks.processors.FailureProcessor;

import java.util.TreeMap;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ErrorHandler {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private DeadLetterChannelBuilder deadLetterChannelBuilder;
	
	private int maximumRedeliveries;
	private int redeliveryDelay;
	private int maximumRedeliveryDelay;
	private int backOffMultiplier;

	private TreeMap<String, String> props;

	private String flowId;

	public ErrorHandler(DeadLetterChannelBuilder deadLetterChannelBuilder, final TreeMap<String, String> props, String flowId){
		this.deadLetterChannelBuilder = deadLetterChannelBuilder;
		this.props = props;
		this.flowId = flowId;
	}
	
	
	public DeadLetterChannelBuilder configure() throws Exception {

		Processor failureProcessor = new FailureProcessor(props);
		
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
			String redeliveryDelayAsString = props.get("flow.redeliveryDelay");
			if(StringUtils.isNumeric(redeliveryDelayAsString)) {
				redeliveryDelay = Integer.parseInt(redeliveryDelayAsString);
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