package org.assimbly.integration.routes;

import java.util.Map;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SimpleRoute extends RouteBuilder{

	protected Logger log = LoggerFactory.getLogger(getClass());
	
	Map<String, String> props;
	
	public SimpleRoute(final Map<String, String> props){
		this.props = props;
	}

	@Override
	public void configure() throws Exception {
			
		log.info("Configuring default route");
		
		if (this.props.containsKey("error.uri")){
			errorHandler(deadLetterChannel(props.get("error.uri"))
					.maximumRedeliveries(3).redeliveryDelay(1000)
					.maximumRedeliveryDelay(60000).backOffMultiplier(2)
					.retriesExhaustedLogLevel(LoggingLevel.ERROR)
					.retryAttemptedLogLevel(LoggingLevel.DEBUG)
					.logRetryStackTrace(false).logStackTrace(true)
					.logHandled(false).logExhausted(true)
					.logExhaustedMessageHistory(true));
		}
		else{
			errorHandler(defaultErrorHandler().maximumRedeliveries(0)
					.redeliveryDelay(1000).maximumRedeliveryDelay(60000)
					.backOffMultiplier(2)
					.retriesExhaustedLogLevel(LoggingLevel.ERROR)
					.retryAttemptedLogLevel(LoggingLevel.DEBUG)
					.logRetryStackTrace(false).logStackTrace(true)
					.logHandled(false).logExhausted(true)
					.logExhaustedMessageHistory(true));
		}
		
		from(props.get("from.uri"))
		.to(getToUriList())
		.routeId(props.get("id"));
	}

	//create arraylist from touri
	private String[] getToUriList() {
		String toUri = props.get("to.uri");
		String[] toUriArray = toUri.split(",");
		return toUriArray;
	}

}
