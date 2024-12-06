package org.assimbly.dil.event.collect;

import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.util.ArrayList;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class FailureCollector extends EventNotifierSupport {

    private final String flowId;
    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;
    private final StoreManager storeManager;

    public FailureCollector(String collectorId, String flowId, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.flowId = flowId;
        this.events = events;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);

    }

    @Override
    public void notify(CamelEvent event) throws Exception {

        String type = event.getType().name();

        if(event instanceof CamelEvent.FailureEvent) {

            //Cast to route event
            CamelEvent.FailureEvent failureEvent = (CamelEvent.FailureEvent) event;

            //process and store the exchange
            if(failureEvent!=null && filters==null){
                processEvent(failureEvent);
            }

        }

    }

    private void processEvent(CamelEvent.FailureEvent failureEvent){

        //set fields
        String timestamp = Long.toString(failureEvent.getTimestamp());
        String logLevel = "ERROR";
        String message = failureEvent.getClass().getName();
        String exception = failureEvent.getCause().getMessage();

        //create json
        LogEvent logEvent = new LogEvent(timestamp, "", logLevel, "FAILURE", message, exception);
        String json = logEvent.toJson();

        System.out.println("FailureCollector: " + json);

        //store event
        storeManager.storeEvent(json);

    }

}
