package org.assimbly.dil.event.collect;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.assimbly.dil.event.domain.Filter;
import org.assimbly.dil.event.domain.LogEvent;
import org.assimbly.dil.event.domain.Store;
import org.assimbly.dil.event.store.StoreManager;
import org.assimbly.dil.event.util.EventUtil;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class LogCollector extends AppenderBase<ILoggingEvent> {

    private final StoreManager storeManager;
    private final String flowId;
    private final ArrayList<Filter> filters;

    // Use the same SHARED pool to keep thread count low
    private static final ThreadPoolExecutor logCollectionPool = new ThreadPoolExecutor(
            10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(5000),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    public LogCollector(String collectorId, String flowId, ArrayList<Filter> filters, ArrayList<Store> stores) {
        this.flowId = flowId;
        this.filters = filters;
        this.storeManager = new StoreManager(collectorId, stores);
    }

    private boolean isQueueReady() {
        // We are ready if the queue has more than 100 slots free
        return logCollectionPool.getQueue().remainingCapacity() > 200; // Keep a 10% buffer
    }

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        if (loggingEvent == null) return;

        // FAST-FAIL: If the store is full, don't even look at the log event
        if (!isQueueReady()) {
            return;
        }

        String message = loggingEvent.getFormattedMessage();
        String loggerName = loggingEvent.getLoggerName();

        // Filter check happens on the calling thread (fast)
        if (filters == null || EventUtil.isFiltered(filters, message + loggerName)) {

            // Hand off the JSON creation and storage to background
            logCollectionPool.submit(() -> {
                try {
                    processEvent(loggingEvent, message);
                } catch (Exception e) {
                    // Use standard syserr to avoid recursive logging loops!
                    System.err.println("LogCollector background failed: " + e.getMessage());
                }
            });
        }
    }

    private void processEvent(ILoggingEvent event, String message){

        //set fields
        long timestamp = event.getTimeStamp();
        String logLevel = event.getLevel().toString();
        String exception = (logLevel.equalsIgnoreCase("error")) ? message : "";

        //create json
        LogEvent logEvent = new LogEvent(timestamp, flowId, logLevel, "FLOW", message, exception);
        String json = logEvent.toJson();

        //store the event
        storeManager.storeEvent(json);

    }

}