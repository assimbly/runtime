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

    public DILPersistentStore(File dbFile) {
        db = DBMaker.fileDB(dbFile)
                .transactionEnable()
                .fileMmapEnable()
                .fileMmapPreclearDisable()   // faster startup
                .cleanerHackEnable()         // helps reclaim mmap memory on close
                .closeOnJvmShutdown()        // safety net
                .make();
    }

    private ConcurrentMap<String, TreeMap<String, String>>  flowsMap() {
        return db.hashMap("flowsMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.JAVA)
                .createOrOpen();
    }

    private ConcurrentMap<String, String> collectorsMap() {
        return db.hashMap("collectorsMap")
                .keySerializer(Serializer.STRING)
                .valueSerializer(Serializer.STRING)
                .createOrOpen();
    }

    @Override
    public void putFlow(String flowId, TreeMap<String, String> configuration) {
        flowsMap().put(flowId, configuration);
        db.commit();
    }

    @Override
    public TreeMap<String, String> getFlow(String flowId) {
        return flowsMap().get(flowId);
    }
    @Override
    public void removeFlow(String flowId) {
        flowsMap().remove(flowId);
        db.commit();
    }

    @Override
    public void clearAllFlows() {
        log.debug("Clearing all flows from the persistent store");
        try {
            flowsMap().clear();
            db.commit();
        } catch (Exception e) {
            db.rollback(); // Rollback if something goes wrong to maintain DB integrity
            log.error("Failed to clear flows map", e);
            throw e;
        }
    }

    @Override
    public Collection<TreeMap<String, String>> getAllFlows() {
        return flowsMap().values();
    }

    @Override
    public ConcurrentMap<String, TreeMap<String, String>> getFlowsMap() {
        return flowsMap();
    }

    @Override
    public void putCollector(String collectorId, String configuration) {
        collectorsMap().put(collectorId, configuration);
        db.commit();
    }

    @Override
    public String getCollector(String collectorId) {
        return collectorsMap().get(collectorId);
    }

    @Override
    public void removeCollector(String collectorId) {
        collectorsMap().remove(collectorId);
        db.commit();
    }

    @Override
    public void clearAllCollectors() {
        log.debug("Clearing all collectors from the persistent store");
        try {
            collectorsMap().clear();
            db.commit();
        } catch (Exception e) {
            db.rollback(); // Rollback if something goes wrong to maintain DB integrity
            log.error("Failed to clear collectors map", e);
            throw e;
        }
    }

    @Override
    public ConcurrentMap<String, String> getCollectorsMap() {
        return collectorsMap();
    }

    @Override
    public void close() {
        if (!db.isClosed()) {
            db.commit();
            db.close();
        }
    }

}