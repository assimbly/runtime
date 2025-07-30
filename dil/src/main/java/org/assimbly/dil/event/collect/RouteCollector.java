package org.assimbly.dil.event.collect;

import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.store.StoreManager;

import java.util.ArrayList;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class RouteCollector extends EventNotifierSupport {

    private final String flowId;
    private final ArrayList<String> events;
    private final StoreManager storeManager;

    public RouteCollector(String collectorId, String flowId, ArrayList<String> events, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.flowId = flowId;
        this.events = events;
        this.storeManager = new StoreManager(collectorId, stores);

    }

    @Override
    public void notify(CamelEvent event) {

        String type = event.getType().name();

        if(event instanceof CamelEvent.RouteEvent routeEvent && events!=null && events.contains(type)) {

            //Cast to route event

            //Set stepId from route
            String routeId = routeEvent.getRoute().getId();
            String stepId = StringUtils.substringAfter(routeId, flowId + "-");

            processEvent(routeEvent, stepId);

        }

    }

    private void processEvent(CamelEvent.RouteEvent routeEvent, String stepId){

        //set fields
        String timestamp = Long.toString(routeEvent.getTimestamp());
        String routeEventType = routeEvent.getType().name().substring(5);

        String logLevel = "INFO";
        String message = "Step: " + stepId + " | Event: " + routeEventType;
        String exception = "";

        //create json
        LogEvent logEvent = new LogEvent(timestamp, flowId, logLevel, "FLOW", message, exception);
        String json = logEvent.toJson();

        //store event
        storeManager.storeEvent(json);

    }

}
