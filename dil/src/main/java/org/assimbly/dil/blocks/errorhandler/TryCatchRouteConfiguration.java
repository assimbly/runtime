package org.assimbly.dil.blocks.errorhandler;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteConfigurationBuilder;

/**
 * Lets an exception travel back to a try-catch step instead of being dead-lettered in the step where it occurred.
 * Applies only while the try-catch step has set the AssimblyTryCatch property. Camel's own CamelTryRouteBlock
 * marker is not used, because it is also set by the doTry inside other kamelets, such as http-action.
 */
public class TryCatchRouteConfiguration extends RouteConfigurationBuilder {

    // Step kamelets default their routeConfigurationId to "0"
    public static final String ID = "0";

    public static final String PROPERTY = "AssimblyTryCatch";

    @Override
    public void configuration() {
        // The log output is required: without an output the dead letter channel becomes the failure processor and handles the exception
        routeConfiguration(ID)
            .onException(Exception.class)
            .onWhen(exchangeProperty(PROPERTY).isEqualTo(true))
            .handled(false)
            .log(LoggingLevel.DEBUG, "org.assimbly.errorhandler.trycatch", "Propagating exception to try-catch step");
    }

}
