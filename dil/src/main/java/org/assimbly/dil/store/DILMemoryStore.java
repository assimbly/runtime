package org.assimbly.dil.store;

import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DILMemoryStore implements DILStore {

    private final ConcurrentMap<String, TreeMap<String, String>> flowsMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> collectorsMap = new ConcurrentHashMap<>();

    @Override
    public void putFlow(String flowId, TreeMap<String, String> configuration) {
        flowsMap.put(flowId, configuration);
    }

    @Override
    public TreeMap<String, String> getFlow(String flowId) {
        return flowsMap.get(flowId);
    }

    @Override
    public void removeFlow(String flowId) {
        flowsMap.remove(flowId);
    }

    @Override
    public Collection<TreeMap<String, String>> getAllFlows() {
        return flowsMap.values();
    }

    @Override
    public ConcurrentMap<String, TreeMap<String, String>> getFlowsMap() {
        return flowsMap;
    }


    @Override
    public void putCollector(String collectorId, String configuration) {
        collectorsMap.put(collectorId, configuration);
    }

    @Override
    public String getCollector(String collectorId) {
        return collectorsMap.get(collectorId);
    }

    @Override
    public void removeCollector(String collectorId) {
        collectorsMap.remove(collectorId);
    }

    @Override
    public ConcurrentMap<String, String> getCollectorsMap() {
        return collectorsMap;
    }

    @Override
    public void close() {
        // As not persistent nothing to do
    }

}