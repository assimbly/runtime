package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A Flow's Completed and Failed counters go up once per message, not once per Step. */
class StatsManagerTest {

    private static final String FLOW_ID = "1";

    private CamelContext context;
    private StatsManager statsManager;
    private final ConcurrentMap<String, TreeMap<String, String>> flowsMap = new ConcurrentHashMap<>();

    @BeforeEach
    void startFlow() throws Exception {
        context = new DefaultCamelContext();
        // like a Flow: three Steps over sync links, each its own route in the Flow's group, with a dead letter error handler
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                errorHandler(deadLetterChannel("log:dead"));
                from("direct:source").routeId(FLOW_ID + "-10").group(FLOW_ID).to("direct:1-11");
                from("direct:1-11").routeId(FLOW_ID + "-11").group(FLOW_ID).to("direct:1-12");
                from("direct:1-12").routeId(FLOW_ID + "-12").group(FLOW_ID)
                        .filter(body().isEqualTo("fail")).throwException(new IllegalStateException("Sink failed")).end();
            }
        });
        context.start();
        statsManager = new StatsManager(context, new FlowManager(context));

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:source", "ok");
            template.sendBody("direct:source", "fail");
        }
    }

    @AfterEach
    void stopFlow() {
        context.stop();
    }

    @Test
    void countsEachMessageOnceAtTheSource() {
        TreeMap<String, String> props = new TreeMap<>();
        props.put("source.10.uri", "direct:source");
        props.put("action.11.uri", "setbody:Hello");
        props.put("sink.12.uri", "file:out");
        flowsMap.put(FLOW_ID, props);

        JSONObject messages = new JSONObject(statsManager.getFlowMessages(FLOW_ID, false, "application/json", flowsMap));

        assertEquals(2, messages.getLong("totalTransactions"));
        assertEquals(1, messages.getLong("completedTransactions"));
        assertEquals(1, messages.getLong("failedTransactions"));
    }

    @Test
    void withoutASourceStepCountsEveryRoute() {
        flowsMap.put(FLOW_ID, new TreeMap<>());

        JSONObject messages = new JSONObject(statsManager.getFlowMessages(FLOW_ID, false, "application/json", flowsMap));

        assertEquals(6, messages.getLong("totalTransactions"));
    }
}
