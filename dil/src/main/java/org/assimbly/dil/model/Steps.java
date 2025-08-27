package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Steps(@JsonProperty("step") List<Step> step) { }