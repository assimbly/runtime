package org.assimbly.integration.impl.manager;

import org.assimbly.docconverter.DocConverter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The installed flows that couldn't be restored from the cache, with the reason. A flow stays in the
 * installed-flows index, so the next start of the runtime tries it again; installing or uninstalling it
 * removes it from here.
 */
public class FailedFlows {

    public record FailedFlow(String flowId, String name, String version, String tenant, String message, Instant failedAt) {
    }

    private final Map<String, FailedFlow> flows = new ConcurrentHashMap<>();

    public void add(FailedFlow flow) {
        flows.put(flow.flowId(), flow);
    }

    public void remove(String flowId) {
        flows.remove(flowId);
    }

    public boolean contains(String flowId) {
        return flows.containsKey(flowId);
    }

    public List<FailedFlow> getAll() {
        return flows.values().stream().sorted(Comparator.comparing(FailedFlow::failedAt)).toList();
    }

    public String toString(String mediaType) {
        JSONArray array = new JSONArray();
        for (FailedFlow flow : getAll()) {
            JSONObject json = new JSONObject();
            json.put("id", flow.flowId());
            json.put("name", flow.name());
            json.put("version", flow.version());
            json.put("tenant", flow.tenant());
            json.put("message", flow.message());
            json.put("failedAt", flow.failedAt().toString());
            array.put(json);
        }

        if (mediaType != null && mediaType.contains("xml")) {
            JSONObject flowObject = new JSONObject();
            flowObject.put("flow", array);
            JSONObject flowsObject = new JSONObject();
            flowsObject.put("flows", flowObject);
            return DocConverter.jsonToXml(flowsObject.toString());
        }
        return array.toString();
    }
}
