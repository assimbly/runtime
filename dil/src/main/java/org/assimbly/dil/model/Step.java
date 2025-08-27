package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Step(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("uri") String uri,
        @JsonProperty("links") Links links,
        @JsonProperty("options") Options options
) { }