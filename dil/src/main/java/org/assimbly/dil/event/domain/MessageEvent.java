package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.quartz.impl.StdScheduler;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/*
Note:

1. Sometimes different names are used in the JSONProperties to be in sync with indices in Elastic
2. This must be changed later after Camel2 isn't used anymore. Change the JSONProperties to
their Java name to make them the same to DIL/Camel3.
 */

public class MessageEvent {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String JMS_PREFIX = "JMS";

    private final String id;
    private final String flowId;
    private final String flowVersion;

    private final String stepId;
    private final String timestamp;
    private final String expiryDate;
    private final Map<String, Object> headers;
    private final String body;

    public MessageEvent(String timestamp, String id, String flowId, String flowVersion, String stepId, Map<String, Object> headers, String body, String expiryDate) {
        this.timestamp = timestamp;
        this.id = id;
        this.flowId = flowId;
        this.flowVersion = flowVersion;
        this.stepId = stepId;
        this.headers = headers;
        this.body = body;
        this.expiryDate = expiryDate;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty("transactionId")
    public String getId() {
        return id;
    }

    @JsonProperty("bundleId")
    public String getFlowId() {
        return flowId;
    }

    @JsonProperty("flowVersion")
    public String getFlowVersion() {
        return flowVersion;
    }

    @JsonProperty("component")
    public String getStep() {
        return stepId;
    }

    /**
     * We strip away StdScheduler object because it contains circular references back to the Camel Context.
     * which causes trouble for the serialization to JSON.
     **/
    @JsonProperty("headers")
    public Map<String, Object> getHeaders() {
        return headers.entrySet()
                .stream()
                .filter(header -> !header.getKey().startsWith(JMS_PREFIX))
                .filter(header -> header.getValue() != null)
                .filter(header -> !(header.getValue() instanceof StdScheduler))
                .filter(header -> !(header.getValue() instanceof Response))
                .filter(header -> !(header.getValue() instanceof Request))
                .map(entry -> {
                    if(entry.getKey().toLowerCase().contains("firetime")
                            && entry.getValue() instanceof Date) {
                        return new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().toString());
                    }

                    return entry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("expiry")
    public String getExpiryDate() {
        return expiryDate;
    }

    public String toJson() {
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}