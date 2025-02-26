package org.assimbly.integrationrest.event;

import org.apache.camel.impl.event.ExchangeFailedEvent;
import org.apache.camel.impl.event.ExchangeFailureHandledEvent;
import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// This class listens to failure events in camel exchanges (routes)
// Check the following page for all EventObject instances of Camel: http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/management/event/package-summary.html

@Component
public class FailureCollector extends EventNotifierSupport {

   private String flowId;

    public boolean isEnabled(CamelEvent event) {

      //only notify on failures
        return event instanceof ExchangeFailureHandledEvent || event instanceof ExchangeFailedEvent;

    }

    protected void doStart() throws Exception {
        // noop
    }

    protected void doStop() throws Exception {
        // noop
    }

	@Override
	public void notify(CamelEvent event) throws Exception {

		if (event instanceof ExchangeFailureHandledEvent exchangeFailedEvent) {

            flowId = exchangeFailedEvent.getExchange().getFromRouteId();

            int flowIdPart = flowId.indexOf('-'); //this finds the first occurrence of "."

            if (flowIdPart != -1)
			{
				flowId= flowId.substring(0 , flowIdPart);
			}

  		}else if (event instanceof ExchangeFailedEvent exchangeFailedEvent) {

            flowId = exchangeFailedEvent.getExchange().getFromRouteId();

            int flowIdPart = flowId.indexOf('-'); //this finds the first occurrence of "."

            if (flowIdPart != -1)
			{
				flowId= flowId.substring(0 , flowIdPart); //this will give abc
			}
            
	    }
	}
}
