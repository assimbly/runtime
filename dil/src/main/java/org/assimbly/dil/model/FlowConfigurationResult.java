package org.assimbly.dil.model;

import org.assimbly.dil.transpiler.model.EndpointDefinition;

import java.util.List;
import java.util.TreeMap;

public class FlowConfigurationResult {

    private final TreeMap<String, String> properties;
    private final List<EndpointDefinition> entryPoints;

    public FlowConfigurationResult(TreeMap<String, String> properties,
                                   List<EndpointDefinition> entryPoints) {
        this.properties = properties;
        this.entryPoints = entryPoints;
    }

    public TreeMap<String, String> getProperties() {
        return properties;
    }

    public List<EndpointDefinition> getEntryPoints() {
        return entryPoints;
    }
}
