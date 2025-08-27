package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Dil(@JsonProperty("dil") Root root) { }


