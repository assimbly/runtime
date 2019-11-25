package org.assimbly.connector.routes;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.xpath.XPathFactory;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.xml.XPathBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.connector.event.FlowEvent;
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
	
	private String[] offrampUriList;
	
	public DefaultRoute(final Map<String, String> props){
		this.props = props;
	}

	public DefaultRoute() {
		// TODO Auto-generated constructor stub
	}

	public interface FailureProcessorListener {
		 public void onFailure();
  	}
	
	@Override
	public void configure() throws Exception {
			
		logger.info("Configuring default route");
		
		Processor setHeaders = new SetHeaders();
		Processor failureProcessor = new FailureProcessor();
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
			.process(setHeaders)
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
			.process(setHeaders)
			 .choice()
			    .when(header("convertBodyTo").isEqualTo("bytes"))
			    	.convertBodyTo(byte[].class, "UTF-8")
			    .otherwise()
					.convertBodyTo(String.class, "UTF-8")
			 .end()
			.choice()    		    
	  		    .when(header("ReplyTo").isNotNull())
	  		    	.to(toUri)
	  		    	.toD("vm://${header.ReplyTo}")
	  		    .otherwise()
	  		    	.to(toUri)
  		  .end()
  		  .routeId(flowId + "-" + endpointId);			
		   
		}
		
	}
	
	//create a string array for all offramps
	private String[] getOfframpUriList() {
		
		String offrampUri = props.get("offramp.uri.list");
		
		String[] offrampUriArray = offrampUri.split(",");
		
		return offrampUriArray;
	}	
	
	//set headers for each endpoint
	public class SetHeaders implements Processor {
		
		  public void process(Exchange exchange) throws Exception {
			  
				Message in = exchange.getIn();
				Object endpointIdObject = in.getHeader("AssimblyHeaderId");
				
				if(endpointIdObject!=null) {
					
					String endpointId = endpointIdObject.toString();
				
				    Map<String, String> headers = props.entrySet()
				    	      .stream()
				    	      .filter(map -> map.getKey().startsWith("header." + endpointId))
				    	      .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
					
					
					for (Map.Entry<String, String> entry : headers.entrySet()) {

						String key = entry.getKey();

						if (key.startsWith("header." + endpointId + ".constant")) {
							in.setHeader(StringUtils.substringAfterLast(key, "constant."), entry.getValue());
						}else if (key.startsWith("header." + endpointId + ".simple")) {
							in.setHeader(StringUtils.substringAfterLast(key, "simple."), simple(entry.getValue()).evaluate(exchange, String.class));
						}else if (key.startsWith("header." + endpointId + ".xpath")) {
							XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
							String xpathResult = XPathBuilder.xpath(entry.getValue()).factory(fac).evaluate(exchange, String.class);
							in.setHeader(StringUtils.substringAfterLast(key, "xpath."),xpathResult);						
						}
					}
				}
		  }		  
	}
	
	public class FailureProcessor implements Processor {
		
  	    private final String userHomeDir = System.getProperty("user.home");
  	    private FlowEvent flowEvent;
  	  
		public void process(Exchange exchange) throws Exception {
			  
			  Date date = new Date();
			  String today = new SimpleDateFormat("yyyyMMdd").format(date);
			  String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
			  flowEvent = new FlowEvent(exchange.getFromRouteId(),date,exchange.getException().getMessage());
			  			  
			  File file = new File(userHomeDir + "/.assimbly/logs/alerts/" + flowEvent.getFlowId() + "/" + today + "_alerts.log");
			  List<String> line = Arrays.asList(timestamp + " : " + flowEvent.getError());
			  FileUtils.writeLines(file, line, true);
			
		  }
		
	}

}
