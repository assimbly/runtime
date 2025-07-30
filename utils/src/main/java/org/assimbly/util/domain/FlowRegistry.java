package org.assimbly.util.domain;

import org.assimbly.util.exception.FlowNotFoundException;

import java.util.HashMap;

public class FlowRegistry {

    private static final FlowRegistry INSTANCE = new FlowRegistry();

    private static final HashMap<String, FlowInfo> registeredFlows = new HashMap<>();

    private FlowRegistry() {}

    public void register(FlowInfo flowInfo) {
        registeredFlows.put(flowInfo.getFlowId(), flowInfo);
    }

    public boolean belongsToTenant(String flowId, String tenant) {
        FlowInfo flowInfo = registeredFlows.get(flowId);

        if(flowInfo == null)
            return false;

        return flowInfo.getTenantName().equals(tenant);
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

    public HashMap<String, FlowInfo> getRegisteredFlows() { return new HashMap<>(registeredFlows); }

    public void unregister(String flowId) { registeredFlows.remove(flowId); }

    public static FlowRegistry getInstance() {
        return INSTANCE;
    }
}