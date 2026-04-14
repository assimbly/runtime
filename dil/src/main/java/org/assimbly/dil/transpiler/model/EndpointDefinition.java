package org.assimbly.dil.transpiler.model;

public class EndpointDefinition {

    private final String flowId;

    private final EndpointType type;

    private final String key;

    public EndpointDefinition(String flowId, EndpointType type, String key) {
        this.flowId = flowId;
        this.type = type;
        this.key = key;
    }

    public String getFlowId() {
        return flowId;
    }

    public EndpointType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }
}