package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.util.ArrayList;

public class Collection {

    private static final ObjectMapper mapper = new ObjectMapper();
    private String id;
    private String type;
    private String flowId;
    private String flowVersion;
    private ArrayList<String> events;
    private ArrayList<String> failedEvents;
    private ArrayList<Store> stores;
    private ArrayList<Filter> filters;

    @JsonProperty("id")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("type")
    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonProperty("flowId")
    public String getFlowId() {
        return this.flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    @JsonProperty("flowVersion")
    public String getFlowVersion() {
        return this.flowVersion;
    }

    public void setFlowVersion(String flowVersion) {
        this.flowVersion = flowVersion;
    }

    @JsonProperty("events")
    public ArrayList<String> getEvents() {
        return this.events;
    }

    public void setEvents(ArrayList<String> events) {
        this.events = events;
    }

    @JsonProperty("failedEvents")
    public ArrayList<String> getFailedEvents() {
        return failedEvents;
    }

    public void setFailedEvents(ArrayList<String> failedEvents) {
        this.failedEvents = failedEvents;
    }

    @JsonProperty("stores")
    public ArrayList<Store> getStores() {
        return this.stores;
    }

    public void setStores(ArrayList<Store> stores) {
        this.stores = stores;
    }

    @JsonProperty("filters")
    public ArrayList<Filter> getFilters() {
        return this.filters;
    }

    public void setFilters(ArrayList<Filter> filters) {
        this.filters = filters;
    }

    public Collection fromJson(String json) throws JsonProcessingException {

        Collection myObject = mapper.readValue(json, Collection.class);

        return myObject;
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