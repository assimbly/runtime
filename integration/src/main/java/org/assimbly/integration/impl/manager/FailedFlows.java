package org.assimbly.integration.impl.manager;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The installed flows that couldn't be restored from the cache, with the reason. A flow stays in the
 * installed-flows index, so the next start of the runtime tries it again; installing or uninstalling it
 * removes it from here.
 */
public class FailedFlows {

    public record FailedFlow(String flowId, String name, String version, String tenant, String message, Instant failedAt) {
    }

    private final Map<String, FailedFlow> flows = new ConcurrentHashMap<>();

    public void add(FailedFlow flow) {
        flows.put(flow.flowId(), flow);
    }

    public void remove(String flowId) {
        flows.remove(flowId);
    }

    public boolean contains(String flowId) {
        return flows.containsKey(flowId);
    }

    public List<FailedFlow> getAll() {
        return flows.values().stream().sorted(Comparator.comparing(FailedFlow::failedAt)).toList();
    }
}
