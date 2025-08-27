package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Flows(@JsonProperty("flow") Flow flow) { }

