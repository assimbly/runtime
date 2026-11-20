package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.component.once.OnceEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The once-source kamelet triggers a flow once at startup with optional body, headers, and delay. */
class OnceKameletTest {

    private CamelContext context;

    @BeforeEach
    void startContext() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        PluginHelper.getRoutesLoader(context).loadRoutes(
            ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/once-source.kamelet.yaml"));
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void triggersOnceWithBodyHeadersAndDelay() throws Exception {
        context.getRegistry().bind("onceHeaders", Map.of("X-Source", "once-test", "X-Env", "dev"));

        MockEndpoint out = context.getEndpoint("mock:out", MockEndpoint.class);
        out.expectedMessageCount(1);
        out.expectedBodiesReceived("{\"msg\":\"hello & world=1\"}");
        out.expectedHeaderReceived("X-Source", "once-test");
        out.expectedHeaderReceived("X-Env", "dev");

        TemplatedRouteBuilder.builder(context, "once-source")
            .routeId("flow-1")
            .parameter("routeId", "flow-1")
            .parameter("stepId", "step-1")
            .parameter("body", "{\"msg\":\"hello & world=1\"}")
            .parameter("headers", "#onceHeaders")
            .parameter("delay", "0")
            .parameter("out", "mock:out")
            .add();

        context.start();

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, out);

        Endpoint endpoint = context.getRoute("flow-1").getEndpoint();
        OnceEndpoint onceEndpoint = assertInstanceOf(OnceEndpoint.class, endpoint);
        assertEquals("step-1", onceEndpoint.getName());
        assertEquals("{\"msg\":\"hello & world=1\"}", onceEndpoint.getBody());
        assertEquals(0L, onceEndpoint.getDelay());
        assertTrue(onceEndpoint.isBridgeErrorHandler());
    }

    @Test
    void usesDefaultsWhenOptionalPropertiesOmitted() throws Exception {
        MockEndpoint out = context.getEndpoint("mock:defaultOut", MockEndpoint.class);
        out.expectedMessageCount(1);

        TemplatedRouteBuilder.builder(context, "once-source")
            .routeId("flow-2")
            .parameter("routeId", "flow-2")
            .parameter("stepId", "step-2")
            .parameter("out", "mock:defaultOut")
            .add();

        context.start();

        Endpoint endpoint = context.getRoute("flow-2").getEndpoint();
        OnceEndpoint onceEndpoint = assertInstanceOf(OnceEndpoint.class, endpoint);
        assertEquals("step-2", onceEndpoint.getName());
        assertNull(onceEndpoint.getBody());
        assertNull(onceEndpoint.getHeaders());
        assertEquals(1000L, onceEndpoint.getDelay());
        assertTrue(onceEndpoint.isBridgeErrorHandler());

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, out);
        assertNull(out.getExchanges().getFirst().getMessage().getBody());
    }
}
