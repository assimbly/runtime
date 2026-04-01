package org.assimbly.dil.store;

import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

public interface DILStore {


    // --- Flows map: String -> TreeMap<String, String> ---
    void putFlow(String flowId, TreeMap<String, String> configuration);
    TreeMap<String, String> getFlow(String flowId);
    void removeFlow(String flowId);
    Collection<TreeMap<String, String>> getAllFlows();
    ConcurrentMap<String, TreeMap<String, String>> getFlowsMap();

    // --- Collectors map: String -> String ---
    void putCollector(String collectorId, String configuration);
    String getCollector(String collectorId);
    void removeCollector(String collectorId);
    ConcurrentMap<String, String> getCollectorsMap();

    // --- Lifecycle ---
    void close();
}