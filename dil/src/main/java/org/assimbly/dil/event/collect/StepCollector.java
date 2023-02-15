package org.assimbly.dil.event.collect;

import org.apache.camel.spi.CamelEvent;
import org.apache.camel.support.EventNotifierSupport;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.StepEvent;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//Check following page for all Event instances: https://www.javadoc.io/doc/org.apache.camel/camel-api/latest/org/apache/camel/spi/CamelEvent.html

public class StepCollector extends EventNotifierSupport {

    private final ArrayList<Filter> filters;
    private final ArrayList<String> events;
    private final StoreManager storeManager;

    public StepCollector(String collectorId, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
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
        StepEvent stepEvent = new StepEvent(timestamp, stepId, logLevel, "STEP", message, exception);
        String json = stepEvent.toJson();

        //store event
        storeManager.storeEvent(json);
    }

}
