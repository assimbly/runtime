package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.util.Map;

//set headers for each step
public class SetOriginalMessageProcessor implements Processor {

	private static String DOVETAIL_SET_ORIGINAL_HTTP_MESSAGE_PROP = "DOVETAIL_setOriginalHttpMessage";
	private static String DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP = "DOVETAIL_originalHttpMessage";

	private static String DOVETAIL_RETRY_ATTEMPTS_HEAD = "DOVETAIL_RetryAttempts";

	public void process(Exchange exchange) throws Exception {
		Object setOriginalHttpMessage = exchange.getProperty(DOVETAIL_SET_ORIGINAL_HTTP_MESSAGE_PROP);

		if(setOriginalHttpMessage!=null) {
			// first removes DOVETAIL_setOriginalHttpMessage
			exchange.removeProperty(DOVETAIL_SET_ORIGINAL_HTTP_MESSAGE_PROP);
			// set DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP
			exchange.setProperty(DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP, exchange.getMessage().copy());

		} else {
			// load DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP
			Message originalHttpMessage = (Message)exchange.getProperty(DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP);
			// load retry attempts
			Object retryAttempts = originalHttpMessage.getHeader(DOVETAIL_RETRY_ATTEMPTS_HEAD);

			if(retryAttempts==null){
				originalHttpMessage.setHeader(DOVETAIL_RETRY_ATTEMPTS_HEAD,1);
			}else{
				originalHttpMessage.setHeader(DOVETAIL_RETRY_ATTEMPTS_HEAD,(Integer)retryAttempts + 1);
			}
			// set updated DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP
			exchange.setProperty(DOVETAIL_ORIGINAL_HTTP_MESSAGE_PROP, originalHttpMessage);

			// load original http message into exchange
			exchange.setMessage(originalHttpMessage);
		}
	}

}