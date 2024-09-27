package org.assimbly.dil.blocks.beans;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowLogger implements Processor {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange exchange) throws Exception {

        String messageToLog = exchange.getProperty("DovetailLogMessage", String.class);
        String logLevel = exchange.getProperty("DovetailLogLevel", String.class);

        switch (logLevel) {
            case "INFO":
                log.info(messageToLog);
                break;
            case "WARNING":
                log.warn(messageToLog);
                break;
            case "ERROR":
                log.error(messageToLog);
                break;
            default:
                log.info(messageToLog);
                break;
        }

        exchange.removeProperty("DovetailLogMessage");
        exchange.removeProperty("DovetailLogLevel");

    }

}
