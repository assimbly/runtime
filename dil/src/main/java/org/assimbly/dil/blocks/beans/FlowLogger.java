package org.assimbly.dil.blocks.beans;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowLogger implements Processor {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {

        String messageToLog = exchange.getProperty("AssimblyLogMessage", String.class);
        String logLevel = exchange.getProperty("AssimblyLogLevel", String.class);

        if ("WARNING".equals(logLevel)) {
            log.warn(messageToLog);
        } else if ("ERROR".equals(logLevel)) {
            log.error(messageToLog);
        } else {
            log.info(messageToLog);
        }

        exchange.removeProperty("AssimblyLogMessage");
        exchange.removeProperty("AssimblyLogLevel");

    }

}
