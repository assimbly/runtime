package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetOriginalMessageProcessor implements Processor {

	private static String DOVETAIL_ORIGINAL_HTTP_MESSAGE_METHOD_PROP = "DOVETAIL_originalHttpMessageMethod";

	private static String DOVETAIL_RETRY_ATTEMPTS_HEADER = "DOVETAIL_RetryAttempts";
	private static String DOVETAIL_ORIGINAL_HTTP_MESSAGE_VARIABLE_ID_HEADER = "DOVETAIL_OriginalHttpMessageVariableId";

	private static String CAMEL_GLOBAL_VARIABLE_PREFIX = "global:";

	public enum HttpMethod {GET, SET, DEL}

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void process(Exchange exchange) throws Exception {
		String variableId;

		String methodStr = (String) exchange.getProperty(DOVETAIL_ORIGINAL_HTTP_MESSAGE_METHOD_PROP);
		HttpMethod originalHttpMessageMethod = getHttpMethod(methodStr);

		switch (originalHttpMessageMethod) {
			case GET:
				log.info(" > GET OriginalHttpMessage");
				// load the original http message into the current exchange from the camel global variable

				// load retry attempts
				Integer retryAttempts = exchange.getMessage().getHeader(DOVETAIL_RETRY_ATTEMPTS_HEADER, Integer.class);
				retryAttempts++;

				// load original http message variable id
				variableId = exchange.getMessage().getHeader(DOVETAIL_ORIGINAL_HTTP_MESSAGE_VARIABLE_ID_HEADER, String.class);

				// load original http message from global variable
				Message originalHttpMessage = exchange.getVariable(CAMEL_GLOBAL_VARIABLE_PREFIX + variableId, Message.class);

				// set retry attempts
				originalHttpMessage.setHeader(DOVETAIL_RETRY_ATTEMPTS_HEADER, retryAttempts);

				// load original http message into exchange
				exchange.setMessage(originalHttpMessage);
				break;

			case SET:
				log.info(" > SET OriginalHttpMessage");
				// set the current exchange message as the original http message in the camel global variable

				// set retry attempts to 1
				exchange.getMessage().setHeader(DOVETAIL_RETRY_ATTEMPTS_HEADER, 0);

				// using exchangeId as http message variable id
				String exchangeId = exchange.getExchangeId();
				// set original http message variable id
				exchange.getMessage().setHeader(DOVETAIL_ORIGINAL_HTTP_MESSAGE_VARIABLE_ID_HEADER, exchangeId);

				// set original http message on a global variable
				exchange.setVariable(CAMEL_GLOBAL_VARIABLE_PREFIX + exchangeId, exchange.getMessage().copy());
				break;

			case DEL:
				log.info(" > DEL OriginalHttpMessage");
				// delete the original http message from the camel global variables
				variableId = exchange.getMessage().getHeader(DOVETAIL_ORIGINAL_HTTP_MESSAGE_VARIABLE_ID_HEADER, String.class);
				exchange.removeVariable(CAMEL_GLOBAL_VARIABLE_PREFIX + variableId);
				break;

			default:
				// do nothing
				break;

		}
	}

	private HttpMethod getHttpMethod(String methodStr) {
		try {
			return HttpMethod.valueOf(methodStr);
		} catch (IllegalArgumentException | NullPointerException e) {
			log.error("Invalid HTTP method: " + methodStr + ". Using default: " + HttpMethod.GET);
			return HttpMethod.GET;
		}
	}

}