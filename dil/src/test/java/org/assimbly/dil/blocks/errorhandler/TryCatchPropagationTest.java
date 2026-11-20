package org.assimbly.dil.blocks.errorhandler;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.assimbly.dil.blocks.processors.TryCatchResetProcessor;
import org.assimbly.util.BaseDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A doTry in one step route catches exceptions from later step routes linked by the sync (direct) transport,
 * while every route uses the flow's dead letter channel.
 */
class TryCatchPropagationTest {

    private CamelContext context;
    private MockEndpoint dead;
    private MockEndpoint caught;
    private MockEndpoint passed;
    private String previousBaseDirectory;

    @TempDir
    Path baseDirectory;

    @BeforeEach
    void createContext() throws Exception {
        previousBaseDirectory = BaseDirectory.getInstance().getBaseDirectory();
        BaseDirectory.getInstance().setBaseDirectory(baseDirectory.toString());

        context = new DefaultCamelContext();
        context.addComponent("sync", new DirectComponent());
        context.getRegistry().bind("TryCatchResetProcessor", new TryCatchResetProcessor());

        TreeMap<String, String> props = new TreeMap<>();
        props.put("flow.failureProcessor", "true");
        context.getCamelContextExtension().setErrorHandlerFactory(
            new ErrorHandler(new DeadLetterChannelBuilder("mock:dead"), props, "flow").configure());

        context.addRoutes(new TryCatchRouteConfiguration());
        addRoutes();
        context.start();

        dead = context.getEndpoint("mock:dead", MockEndpoint.class);
        caught = context.getEndpoint("mock:caught", MockEndpoint.class);
        passed = context.getEndpoint("mock:passed", MockEndpoint.class);
    }

    @AfterEach
    void stopContext() {
        context.stop();
        BaseDirectory.getInstance().setBaseDirectory(previousBaseDirectory);
    }

    @Test
    void failureInLaterStepGoesToCatch() throws Exception {
        send("direct:try");

        assertCounts(0, 1, 0);
    }

    @Test
    void failureThroughIntermediateStepGoesToCatch() throws Exception {
        send("direct:tryTwoStepsDeep");

        assertCounts(0, 1, 0);
    }

    @Test
    void successPathContinuesWithoutCatch() throws Exception {
        send("direct:tryValid");

        assertCounts(0, 0, 1);
    }

    @Test
    void failureOutsideTryIsDeadLetteredOnce() throws Exception {
        send("direct:plain");

        assertCounts(1, 0, 0);
    }

    @Test
    void failureInCatchBranchIsDeadLetteredOnce() throws Exception {
        send("direct:tryFailingCatch");

        assertCounts(1, 0, 0);
    }

    @Test
    void nonMatchingCatchInsideOneStepIsDeadLetteredOnce() throws Exception {
        send("direct:beforeStepWithOwnTry");

        assertCounts(1, 0, 0);
        assertTrue(Files.exists(baseDirectory.resolve("alerts")));
    }

    @Test
    void nonMatchingCatchInsideStepGoesToOuterTryCatch() throws Exception {
        send("direct:tryAroundStepWithOwnTry");

        assertCounts(0, 1, 0);
    }

    @Test
    void caughtFailureWritesNoAlert() throws Exception {
        send("direct:try");

        assertCounts(0, 1, 0);
        assertFalse(Files.exists(baseDirectory.resolve("alerts")));
    }

    @Test
    void failureOutsideTryWritesAlert() throws Exception {
        send("direct:plain");

        assertCounts(1, 0, 0);
        assertTrue(Files.exists(baseDirectory.resolve("alerts")));
    }

    private void send(String uri) {
        context.createProducerTemplate().send(uri, exchange -> exchange.getMessage().setBody("<invalid/>"));
    }

    private void assertCounts(int deadCount, int caughtCount, int passedCount) throws Exception {
        dead.expectedMessageCount(deadCount);
        caught.expectedMessageCount(caughtCount);
        passed.expectedMessageCount(passedCount);
        MockEndpoint.assertIsSatisfied(context);
    }

    private void addRoutes() throws Exception {
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:try").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .setProperty(TryCatchRouteConfiguration.PROPERTY, constant(true))
                    .doTry()
                        .to("sync:validator")
                    .doCatch(Exception.class)
                        .process("TryCatchResetProcessor")
                        .to("mock:caught")
                    .end()
                    .removeProperty(TryCatchRouteConfiguration.PROPERTY);

                from("direct:tryTwoStepsDeep").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .setProperty(TryCatchRouteConfiguration.PROPERTY, constant(true))
                    .doTry()
                        .to("sync:intermediate")
                    .doCatch(Exception.class)
                        .process("TryCatchResetProcessor")
                        .to("mock:caught")
                    .end()
                    .removeProperty(TryCatchRouteConfiguration.PROPERTY);

                from("direct:tryValid").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .setProperty(TryCatchRouteConfiguration.PROPERTY, constant(true))
                    .doTry()
                        .to("mock:passed")
                    .doCatch(Exception.class)
                        .process("TryCatchResetProcessor")
                        .to("mock:caught")
                    .end()
                    .removeProperty(TryCatchRouteConfiguration.PROPERTY);

                from("direct:tryFailingCatch").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .setProperty(TryCatchRouteConfiguration.PROPERTY, constant(true))
                    .doTry()
                        .to("sync:validator")
                    .doCatch(Exception.class)
                        .process("TryCatchResetProcessor")
                        .to("sync:failingCatch")
                        .to("mock:caught")
                    .end()
                    .removeProperty(TryCatchRouteConfiguration.PROPERTY);

                from("direct:tryAroundStepWithOwnTry").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .setProperty(TryCatchRouteConfiguration.PROPERTY, constant(true))
                    .doTry()
                        .to("sync:stepWithOwnTry")
                    .doCatch(Exception.class)
                        .process("TryCatchResetProcessor")
                        .to("mock:caught")
                    .end()
                    .removeProperty(TryCatchRouteConfiguration.PROPERTY);

                from("direct:beforeStepWithOwnTry").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .to("sync:stepWithOwnTry");

                // Like the http kamelets: a doTry around the step's own call that catches only some exceptions
                from("sync:stepWithOwnTry").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .doTry()
                        .throwException(new IllegalStateException("http 500"))
                    .doCatch(SocketTimeoutException.class)
                        .to("mock:caught")
                    .end();

                from("direct:plain").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .to("sync:validator");

                from("sync:intermediate").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .to("sync:validator");

                from("sync:validator").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .throwException(new IllegalStateException("invalid xml"))
                    .to("mock:passed");

                from("sync:failingCatch").routeConfigurationId(TryCatchRouteConfiguration.ID)
                    .throwException(new IllegalStateException("catch branch failed"));
            }
        });
    }

}
