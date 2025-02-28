package org.assimbly.dil.event.collect;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.util.ArrayList;


public class LogCollector extends AppenderBase<ILoggingEvent> {

    private final StoreManager storeManager;
    private final String flowId;

    public LogCollector(String collectorId, String flowId, ArrayList<Store> stores) {
        this.flowId = flowId;
        this.storeManager = new StoreManager(collectorId, stores);
    }

    @Override
    protected void append(ILoggingEvent event) {

        if(event!=null){

            String message = event.getMessage();
            event.getLoggerName();

            processEvent(event, message);

        }
    }

    private void processEvent(ILoggingEvent event, String message){

        //set fields
        String timestamp = EventUtil.getTimestamp();
        String logLevel = event.getLevel().toString();
        String exception = (logLevel.equalsIgnoreCase("error")) ? message : "";

        //create json
        LogEvent logEvent = new LogEvent(timestamp, flowId, logLevel, "FLOW", message, exception);
        String json = logEvent.toJson();

        //store the event
        storeManager.storeEvent(json);

    }

}
