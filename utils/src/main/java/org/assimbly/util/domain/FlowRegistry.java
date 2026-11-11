package org.assimbly.util.domain;

import org.assimbly.util.exception.FlowNotFoundException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class FlowRegistry {

    private static final FlowRegistry INSTANCE = new FlowRegistry();

    // flows are registered and looked up from several threads
    private static final Map<String, FlowInfo> registeredFlows = Collections.synchronizedMap(new HashMap<>());

    private FlowRegistry() {}

    public void register(FlowInfo flowInfo) {
        registeredFlows.put(flowInfo.getFlowId(), flowInfo);
    }

    public boolean belongsToTenant(String flowId, String tenant) {
        FlowInfo flowInfo = registeredFlows.get(flowId);

        if(flowInfo == null)
            return false;

        return tenant != null && tenant.equals(flowInfo.getTenantName());
    }

    public String findTenant(String flowId) throws FlowNotFoundException {
        FlowInfo flowInfo = registeredFlows.get(flowId);

        if(flowInfo == null)
            throw new FlowNotFoundException("Unable to find flow tenant");

        return flowInfo.getTenantName();
    }

    public FlowInfo getFlow(String flowId) {
        return registeredFlows.get(flowId);
    }

    public HashMap<String, FlowInfo> getRegisteredFlows() {
        synchronized (registeredFlows) {
            return new HashMap<>(registeredFlows);
        }
    }

    public void unregister(String flowId) { registeredFlows.remove(flowId); }

    public static FlowRegistry getInstance() {
        return INSTANCE;
    }
}
