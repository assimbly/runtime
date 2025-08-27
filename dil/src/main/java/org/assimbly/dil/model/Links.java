package org.assimbly.dil.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record Links(@JsonProperty("link") List<Link> link) {
}
