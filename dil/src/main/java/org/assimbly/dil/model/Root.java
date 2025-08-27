package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Root(@JsonProperty("integrations") Integrations integrations) { }