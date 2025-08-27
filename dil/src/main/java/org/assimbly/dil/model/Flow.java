package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Flow(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("steps") Steps steps
) { }