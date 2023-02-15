package org.assimbly.dil.event.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Store{
    private String id;
    private String type;
    private String uri;

    private String expiryInHours;

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

    @JsonProperty("uri")
    public String getUri() {
        return this.uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @JsonProperty("expiryInHours")
    public String getExpiryInHours() {
        return this.expiryInHours;
    }

    public void setExpiryInHours(String expiryInHours) {
        this.expiryInHours = expiryInHours;
    }

}
