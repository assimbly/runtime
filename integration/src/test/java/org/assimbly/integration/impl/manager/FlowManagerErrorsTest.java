package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Flow's errors tell which node failed, where it is defined, and how often the same error repeated. */
class FlowManagerErrorsTest {

    private static final String FLOW_ID = "1";

    private CamelContext context;
    private FlowManager flowManager;

    @BeforeEach
    void startFlow() throws Exception {
        context = new DefaultCamelContext();
        flowManager = new FlowManager(context);
        // like CamelIntegration at startup: history metrics (with source locations) and the error registry
        new StatsManager(context, flowManager).setHistoryMetrics();
        context.getErrorRegistry().setEnabled(true);
        // loaded from XML like the routes of a Flow
        PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource("""
                <routes xmlns="http://camel.apache.org/schema/xml-io">
                  <route id="1-10" group="1">
                    <from uri="direct:source"/>
                    <step id="step-10">
                      <throwException id="fail-node" exceptionType="java.lang.IllegalStateException" message="Sink failed"/>
                    </step>
                  </route>
                </routes>"""));
        context.start();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            for (int i = 0; i < 2; i++) {
                template.send("direct:source", exchange -> exchange.getIn().setBody("fail"));
            }
        }
    }

    @AfterEach
    void stopFlow() {
        context.stop();
    }

    @Test
    void errorsNameTheFailingNodeAndCountRepeats() {
        JSONArray errors = new JSONArray(flowManager.getFlowErrors(FLOW_ID, 10, "application/json"));

        assertFalse(errors.isEmpty(), errors.toString());

        JSONObject error = errors.getJSONObject(0);
        assertEquals(FLOW_ID, error.getString("flowId"), error.toString());
        assertEquals("10", error.getString("stepId"), error.toString());
        assertEquals("fail-node", error.getString("nodeId"), error.toString());
        // the line of the failing node in the route XML
        assertTrue(error.getString("location").endsWith(":5"), error.toString());
        assertFalse(error.getBoolean("handled"), error.toString());

        long repeats = 0;
        for (int i = 0; i < errors.length(); i++) {
            repeats = Math.max(repeats, errors.getJSONObject(i).getLong("repeatCount"));
        }
        assertEquals(2, repeats, errors.toString());
    }
}
