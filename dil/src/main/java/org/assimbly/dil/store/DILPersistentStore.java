package org.assimbly.dil.store;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;

public class DILPersistentStore implements DILStore {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final DB db;
    private final ConcurrentMap<String, TreeMap<String, String>> flowsMap;
    private final ConcurrentMap<String, String> collectorsMap;

    @SuppressWarnings("unchecked")
    public DILPersistentStore(File dbFile) {

        db = DBMaker.fileDB(dbFile)
                .transactionEnable() // Enable crash safety
                .fileMmapEnable()
                .make();

        collectorsMap = db.hashMap("collectorsMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.STRING)
                .createOrOpen();

        // TreeMap is not a primitive, so use Java general-purpose serializer
        flowsMap = db.hashMap("flowsMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .createOrOpen();

    }

    @Override
    public ConcurrentMap<String, TreeMap<String, String>> getFlowsMap() {
        return flowsMap;
    }

    @Override
    public void putFlow(String flowId, TreeMap<String, String> configuration) {
        flowsMap.put(flowId, configuration);
        db.commit();
    }

    @Override
    public TreeMap<String, String> getFlow(String flowId) {
        return flowsMap.get(flowId);
    }

    @Override
    public void removeFlow(String flowId) {
        flowsMap.remove(flowId);
        db.commit();
    }

    @Override
    public Collection<TreeMap<String, String>> getAllFlows() {
        return flowsMap.values();
    }

    @Override
    public void putCollector(String collectorId, String configuration) {
        collectorsMap.put(collectorId, configuration);
        db.commit();
    }

    @Override
    public String getCollector(String collectorId) {
        return collectorsMap.get(collectorId);
    }

    @Override
    public void removeCollector(String collectorId) {
        collectorsMap.remove(collectorId);
        db.commit();
    }

    @Override
    public void close() {
        if (!db.isClosed()) {
            db.commit();
            db.close();
        }
    }

}