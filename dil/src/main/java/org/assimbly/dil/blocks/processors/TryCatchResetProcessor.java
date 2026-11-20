package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.assimbly.dil.blocks.errorhandler.TryCatchRouteConfiguration;

/**
 * Runs first in the catch branch of a try-catch step, so failures from there on are dead-lettered again.
 * It ends the try scope, and clears the handled mark that the propagating onException left,
 * which would make every later error handler skip the exchange.
 */
public class TryCatchResetProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        exchange.removeProperty(TryCatchRouteConfiguration.PROPERTY);
        exchange.getExchangeExtension().setFailureHandled(false);
    }

}
