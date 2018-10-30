package org.assimbly.connector.event;

import java.util.Date;

public class FlowEvent {

	private String flowId;
	private Date timestamp;
	private String error;
	
	public FlowEvent(String string, Date timestamp, String error) {
		    this.flowId = string;
		    this.timestamp = timestamp;
		    this.error = error;
		  }
	  
	public FlowEvent() {
		// TODO Auto-generated constructor stub
	}

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public Object getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
	
}
