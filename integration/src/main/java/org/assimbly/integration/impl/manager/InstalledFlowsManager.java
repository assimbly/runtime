package org.assimbly.integration.impl.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.util.BaseDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Maintains a lightweight, crash-safe index of installed flows on disk.
 * Stored independently of DILStore so it survives cache wipes and
 * store incompatibilities.
 *
 * File format: JSON Lines, one object per line:
 *   {"flowId":"...","version":"12","tenant":"integrations","status":"started"}
 *
 * {@code status} is {@link FlowEntry#STATUS_STARTED} or {@link FlowEntry#STATUS_PAUSED}.
 * Lines without status are treated as started (backward compatible).
 */
public class InstalledFlowsManager {

    private static final Logger log = LoggerFactory.getLogger(InstalledFlowsManager.class);
    private static final String FILE_NAME = "installed-flows.index";

    private final Path indexFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public InstalledFlowsManager() {
        Path cacheDir = Path.of(BaseDirectory.getInstance().getBaseDirectory(), "cache");
        this.indexFile = Paths.get(cacheDir.toString(), FILE_NAME);
    }

    public synchronized void register(String flowId, String version, String tenant) {
        register(flowId, version, tenant, FlowEntry.STATUS_STARTED);
    }

    public synchronized void register(String flowId, String version, String tenant, String status) {
        Map<String, FlowEntry> entries = readAll();
        entries.put(flowId, new FlowEntry(flowId, version, tenant, normalizeStatus(status)));
        writeAll(entries);
    }

    public synchronized void unregister(String flowId) {
        Map<String, FlowEntry> entries = readAll();
        if (entries.remove(flowId) != null) {
            writeAll(entries);
        }
    }

    public synchronized Map<String, FlowEntry> getAll() {
        return Collections.unmodifiableMap(readAll());
    }

    public synchronized FlowEntry get(String flowId) {
        return readAll().get(flowId);
    }

    public synchronized Map<String, FlowEntry> getByTenant(String tenant) {
        Map<String, FlowEntry> result = new LinkedHashMap<>();
        readAll().forEach((flowId, entry) -> {
            if (tenant.equals(entry.getTenant())) {
                result.put(flowId, entry);
            }
        });
        return result;
    }

    private Map<String, FlowEntry> readAll() {
        Map<String, FlowEntry> result = new LinkedHashMap<>();

        if (!Files.exists(indexFile)) {
            return result;
        }

        try (BufferedReader reader = Files.newBufferedReader(indexFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                parseLine(line, result);
            }
        } catch (IOException e) {
            log.error("Failed to read installed-flows index", e);
        }

        return result;
    }

    private void parseLine(String line, Map<String, FlowEntry> result) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> entry = mapper.readValue(line, Map.class);
            String flowId  = entry.get("flowId");
            String version = entry.get("version");
            String tenant  = entry.get("tenant");
            String status  = normalizeStatus(entry.get("status"));

            if (flowId != null && version != null && tenant != null) {
                result.put(flowId, new FlowEntry(flowId, version, tenant, status));
            }
        } catch (Exception _) {
            log.warn("Skipping malformed line in installed-flows index: {}", line);
        }
    }

    private void writeAll(Map<String, FlowEntry> entries) {
        Path tmp = indexFile.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(indexFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                for (FlowEntry entry : entries.values()) {
                    Map<String, String> recordMap = new LinkedHashMap<>();
                    recordMap.put("flowId", entry.getFlowId());
                    recordMap.put("version", entry.getVersion());
                    recordMap.put("tenant", entry.getTenant());
                    recordMap.put("status", entry.getStatus());
                    writer.write(mapper.writeValueAsString(recordMap));
                    writer.newLine();
                }
            }
            // Atomic replace — prevents corruption if the process crashes mid-write
            Files.move(tmp, indexFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Failed to write installed-flows index", e);
        }
    }

    private static String normalizeStatus(String status) {
        if (FlowEntry.STATUS_PAUSED.equalsIgnoreCase(status)) {
            return FlowEntry.STATUS_PAUSED;
        }
        return FlowEntry.STATUS_STARTED;
    }

    /**
     * Simple data class for a flow entry.
     */
    public static class FlowEntry {
        public static final String STATUS_STARTED = "started";
        public static final String STATUS_PAUSED = "paused";

        private final String flowId;
        private final String version;
        private final String tenant;
        private final String status;

        public FlowEntry(String flowId, String version, String tenant) {
            this(flowId, version, tenant, STATUS_STARTED);
        }

        public FlowEntry(String flowId, String version, String tenant, String status) {
            this.flowId = flowId;
            this.version = version;
            this.tenant = tenant;
            this.status = status != null ? status : STATUS_STARTED;
        }

        public String getFlowId() { return flowId; }
        public String getVersion() { return version; }
        public String getTenant() { return tenant; }
        public String getStatus() { return status; }

        public boolean isPaused() {
            return STATUS_PAUSED.equals(status);
        }
    }
}
