package org.assimbly.dil.event.collect;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;

import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;


public class LogCollector extends AppenderBase {

    private final StoreManager storeManager;
    private final String collectorId;
    private final String tag;
    private final ArrayList<Filter> filters;

    public LogCollector(String collectorId, String tag, ArrayList<String> events, ArrayList<Filter> filters, ArrayList<org.assimbly.dil.event.domain.Store> stores) {
        this.collectorId = collectorId;
        this.tag = tag;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);
    }

    @Override
    protected void append(Object o) {

        LoggingEvent event = (LoggingEvent) o;
        if(event!=null){

            String message = event.getMessage();

            if(filters==null){
                processEvent(event, message);
            }else if(EventUtil.isFiltered(filters, message)){
                processEvent(event, message);
            }

        }
    }

    private void processEvent(LoggingEvent event, String message){

        //set fields
        String timestamp = EventUtil.getTimestamp();
        String logLevel = event.getLevel().toString();
        String exception = (logLevel.equalsIgnoreCase("error")) ? message : "";

        //create json
        LogEvent logEvent = new LogEvent(timestamp, collectorId, logLevel, tag, message, exception);
        String json = logEvent.toJson();

        //store the event
        storeManager.storeEvent(json);

    }

}
