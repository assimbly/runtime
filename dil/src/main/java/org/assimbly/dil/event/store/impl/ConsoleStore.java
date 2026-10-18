package org.assimbly.dil.event.store.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Writes events to the log, so they show wherever the log goes (console, log file, the Gateway's Log viewer)
public class ConsoleStore {

    private final Logger log;

    public ConsoleStore(String collectorId, org.assimbly.dil.event.domain.Store store) {
        log = LoggerFactory.getLogger("org.assimbly.collector." + collectorId);
    }

    public void store(String json){
        log.info(json);
    }
}
