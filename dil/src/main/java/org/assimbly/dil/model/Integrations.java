package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Integrations(@JsonProperty("integration") Integration integration) { }

