package org.assimbly.dil.event.collect;

import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.util.ArrayList;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class RouteCollector extends EventNotifierSupport {

    private final String flowId;
    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;
    private final StoreManager storeManager;

    public RouteCollector(String collectorId, String flowId, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.flowId = flowId;
        this.events = events;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);

    }

    @Override
    public void notify(CamelEvent event) throws Exception {

        String type = event.getType().name();

        if(event instanceof CamelEvent.RouteEvent && events!=null && events.contains(type)) {

            //Cast to route event
            CamelEvent.RouteEvent routeEvent = (CamelEvent.RouteEvent) event;

            //Set stepId from route
            String stepId = routeEvent.getRoute().getId();

            //process and store the exchange
            if(stepId!=null && filters==null){
                processEvent(routeEvent, stepId);
            }else if(stepId!=null && EventUtil.isFiltered(filters, stepId)){
                processEvent(routeEvent, stepId);
            }

        }

    }

    private void processEvent(CamelEvent.RouteEvent routeEvent, String stepId){

        //set fields
        String timestamp = EventUtil.getTimestamp();
        String logLevel = "INFO";
        String message = "Step " + stepId + " " + routeEvent.getType().name().substring(5);
        String exception = "";

        //create json
        LogEvent logEvent = new LogEvent(timestamp, flowId, logLevel, "FLOW", message, exception);
        String json = logEvent.toJson();

        //store event
        storeManager.storeEvent(json);
    }

}
