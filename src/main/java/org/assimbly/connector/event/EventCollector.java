package org.assimbly.connector.event;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.EventObject;
import java.util.List;

import org.apache.camel.impl.event.ExchangeFailedEvent;
import org.apache.camel.impl.event.ExchangeFailureHandledEvent;
import org.apache.camel.impl.event.RouteStartedEvent;
import org.apache.camel.impl.event.RouteStoppedEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.commons.io.FileUtils;
import org.assimbly.connector.connect.util.BaseDirectory;

//Check following page for all EventObject instances: http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/event/package-summary.html

public class EventCollector extends EventNotifierSupport {

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private Date date = new Date();
	private String error;
	private String today = new SimpleDateFormat("yyyyMMdd").format(date);
	private String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS Z").format(date);
	
	public void notify(EventObject eventObject) throws Exception {

	    if (eventObject instanceof RouteStartedEvent) {
	    	
	    	RouteStartedEvent routeStartedEvent = (RouteStartedEvent) eventObject;
	    	String flowId =  routeStartedEvent.getRoute().getId();
	    	File file = new File(baseDir + "/events/" + flowId + "/" + today + "_events.log");
			List<String> line = Arrays.asList(timestamp + " : flow started");
			FileUtils.writeLines(file, line, true);

	    }else if (eventObject instanceof RouteStoppedEvent) {
	    	
	    	RouteStoppedEvent routeStoppedEvent = (RouteStoppedEvent) eventObject;
	    	String flowId =  routeStoppedEvent.getRoute().getId();
	    	File file = new File(baseDir + "/events/" + flowId + "/" + today + "_events.log");
			List<String> line = Arrays.asList(timestamp + " : flow stopped");
			FileUtils.writeLines(file, line, true);
	    	

	    }else if (eventObject instanceof ExchangeFailedEvent) {
	        
	    	ExchangeFailedEvent exchangeFailedEvent = (ExchangeFailedEvent) eventObject;
	        String flowId = exchangeFailedEvent.getExchange().getFromRouteId();
	        String exchangeId = exchangeFailedEvent.getExchange().getExchangeId();

	        Throwable cause = exchangeFailedEvent.getExchange().getException();
	        
	        if(cause!=null) {
    	         error = "Message " + exchangeId + " failed. error=" + cause.getMessage();
	        }else {
	        	error = "Message " + exchangeId + " failed. (check gateway log for error details)";
	        }
	        
	        
	        error = exchangeFailedEvent.getExchange().getException().getMessage();
	    	File file = new File(baseDir + "/events/" + flowId + "/" + today + "_events.log");
	        List<String> line = Arrays.asList(timestamp + " : flow error (unhandled) --> " + error);
			FileUtils.writeLines(file, line, true);
			
	    }else if (eventObject instanceof ExchangeFailureHandledEvent) {
	        
	        ExchangeFailureHandledEvent exchangeFailedEvent = (ExchangeFailureHandledEvent) eventObject;
	        String flowId = exchangeFailedEvent.getExchange().getFromRouteId();
	        String exchangeId = exchangeFailedEvent.getExchange().getExchangeId();
	        String deadLetterUri = exchangeFailedEvent.getDeadLetterUri();

	        Throwable cause = exchangeFailedEvent.getExchange().getException();
	        
	        if(cause!=null) {
    	         error = "Message " + exchangeId + " is sent to error endpoint: " + deadLetterUri + " error=" + cause.getMessage();
	        }else {
	        	error = "Message " + exchangeId + " is sent to error endpoint: " + deadLetterUri + " (check gateway log for error details)";
	        }

	    	File file = new File(baseDir + "/events/" + flowId + "/" + today + "_events.log");
	        List<String> line = Arrays.asList(timestamp + " : flow error --> " + error);
			FileUtils.writeLines(file, line, true);
			
	    }
	    
	}
	
    public boolean isEnabled(EventObject event) {
        return true;
    }

    protected void doStart() throws Exception {
        // noop
    }

    protected void doStop() throws Exception {
        // noop
    }

	@Override
	public void notify(CamelEvent event) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
