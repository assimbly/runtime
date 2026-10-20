package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Flow's transactions go up once per message (route group), its exchanges once per Step (routes). */
class StatsManagerTest {

    private static final String FLOW_ID = "1";
    private static final String UNHANDLED_FLOW_ID = "2";

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
        // a Flow without error handler, so its failure is unhandled
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:unhandled").routeId(UNHANDLED_FLOW_ID + "-10").group(UNHANDLED_FLOW_ID)
                        .throwException(new IllegalStateException("Unhandled"));
            }
        });
        context.start();
        statsManager = new StatsManager(context, new FlowManager(context));

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBody("direct:source", "ok");
            template.sendBody("direct:source", "fail");
            assertThrows(Exception.class, () -> template.sendBody("direct:unhandled", "fail"));
        }
    }

    @AfterEach
    void stopFlow() {
        context.stop();
    }

    @Test
    void flowStatsCountTransactionsOncePerMessageAndExchangesOncePerStep() {
        JSONObject flow = new JSONObject(statsManager.getFlowStats(FLOW_ID, false, false, false, flowsMap)).getJSONObject("flow");

        // over sync links the failed message is the same exchange in every step, so it fails in all three
        assertEquals(6, flow.getLong("exchangesTotal"));
        assertEquals(3, flow.getLong("exchangesCompleted"));
        assertEquals(3, flow.getLong("exchangesFailed"));
        assertEquals(0, flow.getLong("exchangesPending"));
        assertEquals(2, flow.getLong("transactionsTotal"));
        assertEquals(1, flow.getLong("transactionsCompleted"));
        assertEquals(1, flow.getLong("transactionsFailed"));
        assertEquals(0, flow.getLong("transactionsPending"));
    }

    @Test
    void flowStatsKeepIdFirstAndCountersInOrder() {
        JsonNode flow = JsonMapper.builder().build().readTree(statsManager.getFlowStats(FLOW_ID, false, false, false, flowsMap)).get("flow");

        assertEquals(List.of("id",
                        "exchangesTotal", "exchangesCompleted", "exchangesFailed", "exchangesPending",
                        "transactionsTotal", "transactionsCompleted", "transactionsFailed", "transactionsPending"),
                List.copyOf(flow.propertyNames()));
    }

    @Test
    void flowExchangesAndTransactionsUseShortKeys() {
        JSONObject exchanges = new JSONObject(statsManager.getFlowExchanges(FLOW_ID, true, "application/json")).getJSONObject("flow");
        JSONObject transactions = new JSONObject(statsManager.getFlowTransactions(FLOW_ID, "application/json")).getJSONObject("flow");

        assertEquals(6, exchanges.getLong("total"));
        assertEquals(3, exchanges.getJSONArray("steps").length());
        assertEquals(2, transactions.getLong("total"));
        assertEquals(1, transactions.getLong("failed"));
    }

    @Test
    void singleValueCountersMatchFlowStats() {
        JSONObject flow = new JSONObject(statsManager.getFlowStats(FLOW_ID, false, false, false, flowsMap)).getJSONObject("flow");

        assertEquals(Long.toString(flow.getLong("exchangesTotal")), statsManager.getFlowTotalExchanges(FLOW_ID));
        assertEquals(Long.toString(flow.getLong("exchangesFailed")), statsManager.getFlowFailedExchanges(FLOW_ID));
        assertEquals(Long.toString(flow.getLong("transactionsTotal")), statsManager.getFlowTotalTransactions(FLOW_ID));
        assertEquals(Long.toString(flow.getLong("transactionsCompleted")), statsManager.getFlowCompletedTransactions(FLOW_ID));
        assertEquals(Long.toString(flow.getLong("transactionsFailed")), statsManager.getFlowFailedTransactions(FLOW_ID));
        assertEquals(Long.toString(flow.getLong("transactionsPending")), statsManager.getFlowPendingTransactions(FLOW_ID));
    }

    @Test
    void stepExchanges() {
        JSONObject step = new JSONObject(statsManager.getStepExchanges(FLOW_ID, "12", "application/json")).getJSONObject("step");

        assertEquals(2, step.getLong("total"));
        assertEquals(1, step.getLong("failed"));
    }

    @Test
    void runtimeStatsCountTransactionsOnceAndExchangesPerStep() {
        JSONObject stats = new JSONObject(statsManager.getStats("application/json"));

        // flow 1: 2 messages over 3 steps, flow 2: 1 message over 1 step
        assertEquals(3, stats.getLong("transactionsTotal"));
        assertEquals(1, stats.getLong("transactionsCompleted"));
        assertEquals(2, stats.getLong("transactionsFailed"));
        assertEquals(7, stats.getLong("exchangesTotal"));
        assertEquals(4, stats.getLong("exchangesFailed"));
    }

    @Test
    void lastFailedIncludesUnhandledFailures() {
        JSONObject flow = new JSONObject(statsManager.getFlowStats(UNHANDLED_FLOW_ID, true, false, false, flowsMap)).getJSONObject("flow");

        assertEquals(1, flow.getLong("transactionsFailed"));
        assertTrue(flow.getLong("lastFailed") > 0);
    }
}
