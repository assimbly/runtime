package org.assimbly.integration.impl.manager;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A cached flow that fails to restore is left out; the other flows still start. */
class FailedFlowRestoreTest {

    @TempDir
    Path directory;

    private CamelContext context;
    private FlowManager flowManager;
    private InstalledFlowsManager index;

    @BeforeEach
    void startContext() {
        context = new DefaultCamelContext();
        index = new InstalledFlowsManager(directory.resolve("installed-flows.index"));
        flowManager = new FlowManager(context, index);
        context.start();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void failingFlowIsLeftOutAndTheOthersStart() {
        index.register("bad", "9", "tenant");
        index.register("good", "1", "tenant");

        // an endpoint that can't be created, like an AS2 client whose partner doesn't answer
        assertFalse(flowManager.restoreFlow("bad", flow("bad", "Bad flow", "unknowncomponent:nowhere")));
        assertTrue(flowManager.restoreFlow("good", flow("good", "Good flow", "direct:good")));

        assertEquals("started", flowManager.getFlowStatus("good"));
        assertFalse(flowManager.hasFlow("bad"));
        // nothing of the failed flow is left in the context
        assertTrue(((ModelCamelContext) context).getRouteDefinitions().stream().noneMatch(d -> "bad".equals(d.getGroup())));
        // it stays installed, so the next start tries it again
        assertEquals("started", index.get("bad").getStatus());
    }

    @Test
    void failedFlowRestoresOnALaterTry() {
        index.register("flow", "1", "tenant");

        assertFalse(flowManager.restoreFlow("flow", flow("flow", "Flow", "unknowncomponent:nowhere")));
        assertFalse(flowManager.hasFlow("flow"));

        assertTrue(flowManager.restoreFlow("flow", flow("flow", "Flow", "direct:flow")));
        assertEquals("started", flowManager.getFlowStatus("flow"));
    }

    @Test
    void flowThatAlreadyRunsIsLeftAsItIs() {
        index.register("flow", "1", "tenant");
        flowManager.restoreFlow("flow", flow("flow", "Flow", "direct:flow"));

        // restoring again (for example after the flow was installed meanwhile) doesn't touch it
        assertTrue(flowManager.restoreFlow("flow", flow("flow", "Flow", "unknowncomponent:nowhere")));

        assertEquals("started", flowManager.getFlowStatus("flow"));
    }

    @Test
    void pausedFlowIsRestoredWithoutStarting() {
        index.register("flow", "1", "tenant", InstalledFlowsManager.FlowEntry.STATUS_PAUSED);

        // restored into a context that already runs, so its routes must not start
        assertTrue(flowManager.restoreFlow("flow", flow("flow", "Flow", "direct:flow")));

        assertTrue(flowManager.hasFlow("flow"));
        assertEquals("suspended", flowManager.getFlowStatus("flow"));
    }

    @Test
    void flowWaitingToBeRestoredIsRestarting() {
        index.register("flow", "1", "tenant");
        flowManager.markToRestore(List.of("flow"));

        assertEquals("restarting", flowManager.getFlowStatus("flow"));

        flowManager.restoreFlow("flow", flow("flow", "Flow", "direct:flow"));
        flowManager.restoreDone("flow");
        assertEquals("started", flowManager.getFlowStatus("flow"));
    }

    @Test
    void flowThatFailedToRestoreIsNoLongerRestarting() {
        index.register("flow", "1", "tenant");
        flowManager.markToRestore(List.of("flow"));

        flowManager.restoreFlow("flow", flow("flow", "Flow", "unknowncomponent:nowhere"));
        flowManager.restoreDone("flow");

        assertEquals("unconfigured", flowManager.getFlowStatus("flow"));
    }

    private static TreeMap<String, String> flow(String id, String name, String toUri) {
        TreeMap<String, String> properties = new TreeMap<>();
        properties.put("id", id);
        properties.put(FlowManager.PROPERTY_FLOW_NAME, name);
        properties.put(FlowManager.PROPERTY_FLOW_VERSION, "bad".equals(id) ? "9" : "1");
        properties.put(FlowManager.PROPERTY_FLOW_TENANT, "tenant");
        properties.put("route.1.route", """
                <routes xmlns="http://camel.apache.org/schema/xml-io">
                  <route id="%s-1" group="%s">
                    <from uri="direct:in-%s"/>
                    <to uri="%s"/>
                  </route>
                </routes>""".formatted(id, id, id, toUri));
        properties.put("route.1.route.id", id + "-1");
        return properties;
    }
}
