package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Link(
        @JsonProperty("id") String id,
        @JsonProperty("transport") String transport,
        @JsonProperty("bound") String bound
) {
}
