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
 *   {"flowId":"abc","versionId":"3"}
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

    public synchronized void register(String flowId, String versionId) {
        Map<String, String> entries = readAll();
        entries.put(flowId, versionId);
        writeAll(entries);
    }

    public synchronized void unregister(String flowId) {
        Map<String, String> entries = readAll();
        if (entries.remove(flowId) != null) {
            writeAll(entries);
        }
    }

    public synchronized Map<String, String> getAll() {
        return Collections.unmodifiableMap(readAll());
    }

    private Map<String, String> readAll() {
        Map<String, String> result = new LinkedHashMap<>();

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

    private void parseLine(String line, Map<String, String> result) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> entry = mapper.readValue(line, Map.class);
            String flowId  = entry.get("flowId");
            String version = entry.get("versionId");
            if (flowId != null && version != null) {
                result.put(flowId, version);
            }
        } catch (Exception _) {
            log.warn("Skipping malformed line in installed-flows index: {}", line);
        }
    }

    private void writeAll(Map<String, String> entries) {
        Path tmp = indexFile.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(indexFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                for (Map.Entry<String, String> e : entries.entrySet()) {
                    Map<String, String> recordMap = new LinkedHashMap<>();
                    recordMap.put("flowId",    e.getKey());
                    recordMap.put("versionId", e.getValue());
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
}