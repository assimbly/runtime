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
 *   {"flowId":"68c7b0cc1e33920007000082","version":"12","tenant":"integrations"}
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
        Map<String, FlowEntry> entries = readAll();
        entries.put(flowId, new FlowEntry(flowId, version, tenant));
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

            if (flowId != null && version != null && tenant != null) {
                result.put(flowId, new FlowEntry(flowId, version, tenant));
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

    /**
     * Simple data class for a flow entry.
     */
    public static class FlowEntry {
        private final String flowId;
        private final String version;
        private final String tenant;

        public FlowEntry(String flowId, String version, String tenant) {
            this.flowId = flowId;
            this.version = version;
            this.tenant = tenant;
        }

        public String getFlowId() { return flowId; }
        public String getVersion() { return version; }
        public String getTenant() { return tenant; }
    }
}