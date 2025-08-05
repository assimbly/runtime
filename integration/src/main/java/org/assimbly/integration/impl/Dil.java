package org.assimbly.integration.impl;

import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public record Dil(@JsonProperty("dil") Root root) { }

record Root(@JsonProperty("integrations") Integrations integrations) { }

record Integrations(@JsonProperty("integration") Integration integration) { }

record Integration(@JsonProperty("flows") Flows flows) { }

record Flows(@JsonProperty("flow") Flow flow) { }

record Flow(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("steps") Steps steps
) { }

record Steps(@JsonProperty("step") List<Step> step) { }

record Step(
        @JsonProperty("id") String id,
        @JsonProperty("type") String type,
        @JsonProperty("uri") String uri,
        @JsonProperty("links") Links links,
        @JsonProperty("options") Options options
) { }

record Links(@JsonProperty("link") List<Link> link) { }

record Link(
        @JsonProperty("id") String id,
        @JsonProperty("transport") String transport,
        @JsonProperty("bound") String bound
) { }

record Options(
        @JsonAnySetter
        Map<String, Object> properties
) { }