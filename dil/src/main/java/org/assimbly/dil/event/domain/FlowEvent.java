package org.assimbly.dil.event.domain;

import java.time.Instant;

public class FlowEvent {

	private String flowId;
	private Instant timestamp;
	private String error;

	public FlowEvent(String flowId, Instant timestamp, String error) {
		this.flowId = flowId;
		this.timestamp = timestamp;
		this.error = error;
	}

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Instant timestamp) {
		this.timestamp = timestamp;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

}