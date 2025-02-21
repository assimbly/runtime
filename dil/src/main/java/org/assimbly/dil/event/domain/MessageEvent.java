package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.assimbly.dil.event.collect.StepCollector;
import org.quartz.impl.StdScheduler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
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

    private static final String PROPERTY_NAME = "name";
    private static final String PROPERTY_VALUE = "value";
    private static final String PROPERTY_UNIT = "unit";

    private static final String UNIT_MILLISECONDS = "milliseconds";
    private static final String UNIT_BYTES = "bytes";

    private static final Set<String> PROPERTIES_MILLISECONDS_UNIT_SET = Set.of(
            StepCollector.RESPONSE_TIME_PROPERTY
    );
    private static final Set<String> PROPERTIES_BYTES_UNIT_SET = Set.of(
            StepCollector.MESSAGE_BODY_SIZE_PROPERTY,
            StepCollector.MESSAGE_HEADERS_SIZE_PROPERTY
    );
    private static final Set<String> PROPERTIES_NO_UNIT_SET = Set.of(
            StepCollector.TIMESTAMP_PROPERTY,
            StepCollector.MESSAGE_BODY_TYPE_PROPERTY,
            StepCollector.EXCHANGE_PATTERN_PROPERTY
    );
    private static final Set<String> PROPERTIES_FILTER_SET;

    static {
        PROPERTIES_FILTER_SET = new HashSet<>();
        PROPERTIES_FILTER_SET.addAll(PROPERTIES_MILLISECONDS_UNIT_SET);
        PROPERTIES_FILTER_SET.addAll(PROPERTIES_BYTES_UNIT_SET);
        PROPERTIES_FILTER_SET.addAll(PROPERTIES_NO_UNIT_SET);
    }

    private final String id;
    private final String flowId;
    private final String flowVersion;
    private final String previousFlowId;
    private final String previousFlowVersion;
    private final String stepId;
    private final String timestamp;
    private final String expiryDate;
    private final Map<String, Object> headers;
    private final Map<String, Object> properties;
    private final String body;
    private final boolean failedExchange;

    public MessageEvent(
            String timestamp, String id, String flowId, String flowVersion, String previousFlowId, String previousFlowVersion,
            String stepId, Map<String, Object> headers, Map<String, Object> properties, String body, String expiryDate,
            boolean failedExchange
    ) {
        this.timestamp = timestamp;
        this.id = id;
        this.flowId = flowId;
        this.flowVersion = flowVersion;
        this.previousFlowId = previousFlowId;
        this.previousFlowVersion = previousFlowVersion;
        this.stepId = stepId;
        this.headers = headers;
        this.properties = properties;
        this.body = body;
        this.expiryDate = expiryDate;
        this.failedExchange = failedExchange;
    }

    @JsonProperty("timestamp")
    public String getTimestamp() {
        return timestamp;
    }

    @JsonProperty("transactionId")
    public String getId() {
        return id;
    }

    @JsonProperty("flowId")
    public String getFlowId() {
        return flowId;
    }

    @JsonProperty("flowVersion")
    public String getFlowVersion() {
        return flowVersion;
    }

    @JsonProperty("previousFlowId")
    public String getPreviousFlowId() {
        return previousFlowId;
    }

    @JsonProperty("previousFlowVersion")
    public String getPreviousFlowVersion() {
        return previousFlowVersion;
    }

    @JsonProperty("component")
    public String getStep() {
        return stepId;
    }

    @JsonProperty("failedExchange")
    public boolean isFailedExchange() {
        return failedExchange;
    }

    /**
     * We strip away StdScheduler object because it contains circular references back to the Camel Context.
     * which causes trouble for the serialization to JSON.
     **/
    @JsonProperty("headers")
    public Map<String, Object> getHeaders() {
        return filterHeaders(headers);
    }

    @JsonProperty("properties")
    public List<Map<String, Object>> getProperties() {
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> propsMap = filterProperties(properties, PROPERTIES_FILTER_SET);

        for (Map.Entry<String, Object> entry : propsMap.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            String unit = getUnit(name);

            Map<String, Object> item = new HashMap<>();
            item.put(PROPERTY_NAME, name);
            item.put(PROPERTY_VALUE, value);
            item.put(PROPERTY_UNIT, unit);

            result.add(item);
        }

        return result;
    }

    @JsonProperty("body")
    public String getBody() {
        return body;
    }

    @JsonProperty("expiryDate")
    public String getExpiryDate() {
        return expiryDate;
    }

    public String toJson() {
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        try {
            return mapper.writeValueAsString(this);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> filterHeaders(Map<String, Object> headers) {
        return headers.entrySet()
                .stream()
                .filter(header -> !header.getKey().startsWith(JMS_PREFIX))
                .filter(header -> header.getValue() != null)
                .filter(header -> !header.getKey().equals(StepCollector.COMPONENT_INIT_TIME_HEADER))
                .filter(header -> !(header.getValue() instanceof StdScheduler))
                .filter(header -> !(header.getValue() instanceof ScheduledThreadPoolExecutor))
                .filter(header -> !(header.getValue() instanceof org.eclipse.jetty.ee10.servlet.ServletApiRequest))
                .filter(header -> !(header.getValue() instanceof org.eclipse.jetty.ee10.servlet.ServletApiResponse))
                .map(entry -> {
                    if(entry.getKey().toLowerCase().contains("firetime")
                            && entry.getValue() instanceof Date) {
                        return new AbstractMap.SimpleEntry<>(entry.getKey(), entry.getValue().toString());
                    }

                    return entry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static Map<String, Object> filterProperties(Map<String, Object> properties, Set<String> propertiesFilter) {
        return properties.entrySet()
                .stream()
                .filter(property -> property.getValue() != null)
                .filter(property -> propertiesFilter.contains(property.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String getUnit(String propertyName) {
        if (PROPERTIES_MILLISECONDS_UNIT_SET.contains(propertyName)) {
            return UNIT_MILLISECONDS;
        } else if (PROPERTIES_BYTES_UNIT_SET.contains(propertyName)) {
            return UNIT_BYTES;
        } else if (PROPERTIES_NO_UNIT_SET.contains(propertyName)) {
            return "";
        }
        return null; // No matching unit found
    }
}