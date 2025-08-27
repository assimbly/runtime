package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.Map;

public record Options(
        @JsonAnySetter
        Map<String, Object> properties
) {
}
