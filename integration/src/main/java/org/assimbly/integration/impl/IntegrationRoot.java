package org.assimbly.integration.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Root class
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IntegrationRoot {
    @JsonProperty("dil")
    private Dil dil;

    public IntegrationRoot() {}

    public Dil getDil() {
        return dil;
    }

    public void setDil(Dil dil) {
        this.dil = dil;
    }

    @Override
    public String toString() {
        return "IntegrationRoot{" +
                "dil=" + dil +
                '}';
    }
}

// Dil class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Dil {
    @JsonProperty("integrations")
    private Integrations integrations;

    public Dil() {}

    public Integrations getIntegrations() {
        return integrations;
    }

    public void setIntegrations(Integrations integrations) {
        this.integrations = integrations;
    }

    @Override
    public String toString() {
        return "Dil{" +
                "integrations=" + integrations +
                '}';
    }
}

// Integrations class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Integrations {
    @JsonProperty("integration")
    private Integration integration;

    public Integrations() {}

    public Integration getIntegration() {
        return integration;
    }

    public void setIntegration(Integration integration) {
        this.integration = integration;
    }

    @Override
    public String toString() {
        return "Integrations{" +
                "integration=" + integration +
                '}';
    }
}

// Integration class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Integration {
    @JsonProperty("flows")
    private Flows flows;

    public Integration() {}

    public Flows getFlows() {
        return flows;
    }

    public void setFlows(Flows flows) {
        this.flows = flows;
    }

    @Override
    public String toString() {
        return "Integration{" +
                "flows=" + flows +
                '}';
    }
}

// Flows class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Flows {
    @JsonProperty("flow")
    private Flow flow;

    public Flows() {}

    public Flow getFlow() {
        return flow;
    }

    public void setFlow(Flow flow) {
        this.flow = flow;
    }

    @Override
    public String toString() {
        return "Flows{" +
                "flow=" + flow +
                '}';
    }
}

// Flow class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Flow {
    @JsonProperty("id")
    private String id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("steps")
    private Steps steps;

    public Flow() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Steps getSteps() {
        return steps;
    }

    public void setSteps(Steps steps) {
        this.steps = steps;
    }

    @Override
    public String toString() {
        return "Flow{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", steps=" + steps +
                '}';
    }
}

// Steps class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Steps {
    @JsonProperty("step")
    private List<Step> step;

    public Steps() {}

    public List<Step> getStep() {
        return step;
    }

    public void setStep(List<Step> step) {
        this.step = step;
    }

    @Override
    public String toString() {
        return "Steps{" +
                "step=" + step +
                '}';
    }
}

// Step class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Step {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("uri")
    private String uri;

    @JsonProperty("options")
    private Map<String, Object> options;

    @JsonProperty("links")
    private Links links;

    public Step() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public void setOptions(Map<String, Object> options) {
        this.options = options;
    }

    // Helper methods for common option access
    public String getOption(String key) {
        return options != null ? options.get(key).toString() : null;
    }

    public Links getLinks() {
        return links;
    }

    public void setLinks(Links links) {
        this.links = links;
    }

    @Override
    public String toString() {
        return "step{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", uri='" + uri + '\'' +
                ", options=" + options +
                ", links=" + links +
                '}';
    }
}



// Links class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Links {
    @JsonProperty("link")
    private Object link; // Can be either Link or List<Link>

    public Links() {}

    public Object getLink() {
        return link;
    }

    public void setLink(Object link) {
        this.link = link;
    }

    // Helper methods for type-safe access
    public Link getSingleLink() {
        if (link instanceof Link) {
            return (Link) link;
        }
        return null;
    }

    public List<Link> getMultipleLinks() {
        if (link instanceof List<?>) {
            List<?> rawList = (List<?>) link;
            if (!rawList.isEmpty() && rawList.getFirst() instanceof LinkedHashMap) {
                // Convert manually
                ObjectMapper mapper = new ObjectMapper();
                List<Link> result = new ArrayList<>();
                for (Object item : rawList) {
                    Link linkObj = mapper.convertValue(item, Link.class);
                    result.add(linkObj);
                }
                return result;
            } else if (!rawList.isEmpty() && rawList.getFirst() instanceof Link) {
                return (List<Link>) rawList;
            }
        }else if(link instanceof LinkedHashMap){
            List<Link> result = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();
            Link linkObj = mapper.convertValue(link, Link.class);
            result.add(linkObj);
            return result;
        }
        return null;
    }

    @Override
    public String toString() {
        return "Links{" +
                "link=" + link +
                '}';
    }
}

// Link class
@JsonInclude(JsonInclude.Include.NON_NULL)
class Link {
    @JsonProperty("id")
    private String id;

    @JsonProperty("transport")
    private String transport;

    @JsonProperty("bound")
    private String bound;

    public Link() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public String getBound() {
        return bound;
    }

    public void setBound(String bound) {
        this.bound = bound;
    }

    @Override
    public String toString() {
        return "Link{" +
                "id='" + id + '\'' +
                ", transport='" + transport + '\'' +
                ", bound='" + bound + '\'' +
                '}';
    }
}