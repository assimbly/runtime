package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Filter{
    private String id;
    private String filter;

    @JsonProperty("id")
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @JsonProperty("filter")
    public String getFilter() {
        return this.filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

}
