package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.dil.blocks.errorhandler.TryCatchRouteConfiguration;
import org.assimbly.dil.blocks.processors.TryCatchResetProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The trycatch-router kamelet routes to its catch branch when a later sync step fails, instead of to the dead letter channel. */
class TryCatchKameletTest {

    private CamelContext context;
    private MockEndpoint dead;
    private MockEndpoint caught;
    private MockEndpoint passed;

    @BeforeEach
    void startContext() throws Exception {
        context = new DefaultCamelContext();
        context.addComponent("sync", new DirectComponent());
        context.getRegistry().bind("TryCatchResetProcessor", new TryCatchResetProcessor());
        context.getCamelContextExtension().setErrorHandlerFactory(
            new ErrorHandler(new DeadLetterChannelBuilder("mock:dead"), new TreeMap<>(), "flow").configure());
        context.addRoutes(new TryCatchRouteConfiguration());

        PluginHelper.getRoutesLoader(context).loadRoutes(
            ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/trycatch-router.kamelet.yaml"));

        TemplatedRouteBuilder.builder(context, "trycatch-router")
            .routeId("flow-trycatch")
            .parameter("routeId", "flow-trycatch")
            .parameter("stepId", "2")
            .parameter("in", "direct:start")
            .parameter("out_default", "sync:validator")
            .parameter("out_catch", "sync:catch")
            .add();

        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("sync:validator").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .filter(body().isEqualTo("invalid"))
                        .throwException(new IllegalStateException("invalid xml"))
                    .end()
                    .to("mock:passed");

                from("sync:catch").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .filter(header("failInCatch").isEqualTo(true))
                        .throwException(new IllegalStateException("catch branch failed"))
                    .end()
                    .setBody(simple("Invalid: ${exception.message}"))
                    .to("mock:caught");
            }
        });

        context.start();

        dead = context.getEndpoint("mock:dead", MockEndpoint.class);
        caught = context.getEndpoint("mock:caught", MockEndpoint.class);
        passed = context.getEndpoint("mock:passed", MockEndpoint.class);
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void validMessageGoesToDefaultBranch() throws Exception {
        passed.expectedBodiesReceived("valid");
        caught.expectedMessageCount(0);
        dead.expectedMessageCount(0);

        Exchange result = send("valid", false);

        MockEndpoint.assertIsSatisfied(context);
        assertNull(result.getException());
        assertNull(result.getProperty(TryCatchRouteConfiguration.PROPERTY));
    }

    @Test
    void failureInLaterStepGoesToCatchBranchWithExceptionMessage() throws Exception {
        passed.expectedMessageCount(0);
        caught.expectedBodiesReceived("Invalid: invalid xml");
        dead.expectedMessageCount(0);

        Exchange result = send("invalid", false);

        MockEndpoint.assertIsSatisfied(context);
        assertNull(result.getException());
        assertNull(result.getProperty(TryCatchRouteConfiguration.PROPERTY));
    }

    @Test
    void failureInCatchBranchGoesToDeadLetterChannel() throws Exception {
        caught.expectedMessageCount(0);
        dead.expectedMessageCount(1);

        send("invalid", true);

        MockEndpoint.assertIsSatisfied(context);
        assertEquals("catch branch failed",
            dead.getReceivedExchanges().getFirst().getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class).getMessage());
    }

    private Exchange send(String body, boolean failInCatch) {
        return context.createProducerTemplate().send("direct:start", exchange -> {
            exchange.getMessage().setBody(body);
            exchange.getMessage().setHeader("failInCatch", failInCatch);
        });
    }

}
