package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

/*
Note sometimes different names are used to be in sync with Karaf indices in Elastic

This must be changed later after Camel2 isn't used). Change the JSONProperties to
their Java name to make them the same to DIL/Camel3.
 */

public class LogEvent {

    private static final ObjectMapper mapper = new ObjectMapper();
    private String logLevel;
    private long timestamp;
    private String flowId;
    private String tag;
    private String message;
    private String exception;

    public LogEvent(long timestamp, String flowId, String logLevel, String tag, String message, String exception) {
        this.timestamp = timestamp;
        this.flowId = flowId;
        this.logLevel = logLevel;
        this.tag = tag;
        this.message = message;
        this.exception = exception;
    }

    @JsonProperty("logLevel")
    public String getLogLevel() {
        return this.logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    @JsonProperty("timestamp")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("camelContextId")
    public String getFlowId() {
        return this.flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    @JsonProperty("tag")
    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    @JsonProperty("message")
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("exception")
    public String getException() {
        return this.exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public String toJson() {

        try {
            return mapper.writeValueAsString(this);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }
}