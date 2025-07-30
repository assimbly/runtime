package org.assimbly.util.domain;

import static org.apache.commons.text.StringEscapeUtils.unescapeXml;

public class FlowInfo {

    private String environment;
    private String flowId;
    private String flowName;
    private String flowGroupId;
    private String flowGroup;
    private String flowVersion;
    private String tenantName;

    private FlowState state;

    public FlowInfo() { }

    public FlowInfo(String environment, String flowId, String flowName, String flowGroupId, String flowGroup, String flowVersion, String tenantName, FlowState state) {
        this.environment = unescapeXml(environment);
        this.flowId = unescapeXml(flowId);
        this.flowName = unescapeXml(flowName);
        this.flowGroupId = unescapeXml(flowGroupId);
        this.flowGroup = unescapeXml(flowGroup);
        this.flowVersion = unescapeXml(flowVersion);
        this.tenantName = unescapeXml(tenantName);
        this.state = state;
    }

    public void register() {
        FlowRegistry.getInstance().register(this);
    }

    public void setFlowId(String flowId) {
        this.flowId = unescapeXml(flowId);
    }

    public String getFlowId() {
        return flowId;
    }

    public void setEnvironment(String environment) {
        this.environment = unescapeXml(environment);
    }

    public String getEnvironment() {
        return environment;
    }

    public void setFlowName(String flowName) {
        this.flowName = unescapeXml(flowName);
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowGroupId(String flowGroupId) {
        this.flowGroupId = unescapeXml(flowGroupId);
    }

    public String getFlowGroupId() {
        return flowGroupId;
    }

    public void setFlowGroup(String flowGroup) {
        this.flowGroup = unescapeXml(flowGroup);
    }

    public String getFlowGroup() {
        return flowGroup;
    }

    public void setFlowVersion(String flowVersion) {
        this.flowVersion = unescapeXml(flowVersion);
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = unescapeXml(tenantName);
    }

    public String getTenantName() {
        return tenantName;
    }

    public FlowState getState() {
        return state;
    }

    public void setState(FlowState state) {
        this.state = state;
    }
}
