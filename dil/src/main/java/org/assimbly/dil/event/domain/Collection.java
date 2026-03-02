package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.util.ArrayList;

public class Collection {

    private static final ObjectMapper mapper = new ObjectMapper();
    private String id;
    private String type;
    private String flowId;
    private String flowVersion;
    private ArrayList<String> events;
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

    public Collection fromJson(String json) throws JacksonException {
        return mapper.readValue(json, Collection.class);
    }

    public String toJson() {

        try {
            return mapper.writeValueAsString(this);
        } catch (JacksonException e) {
            throw new RuntimeException(e);
        }
    }

}