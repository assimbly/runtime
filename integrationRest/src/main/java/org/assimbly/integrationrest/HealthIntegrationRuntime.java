package org.assimbly.integrationrest;

import com.sun.management.UnixOperatingSystemMXBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@Tag(name = "Backend health", description = "Health of the backend itself")
@RestController
@RequestMapping("/health/backend")
public class HealthIntegrationRuntime {

    private static final Logger log = LoggerFactory.getLogger(HealthIntegrationRuntime.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private final Integration integration;

    public HealthIntegrationRuntime(IntegrationRuntime integrationRuntime) {
        this.integration = integrationRuntime.getIntegration();
    }

    @Operation(summary = "Get flows overview")
    @GetMapping(
            path = "/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {
        long connectorId = 1L;

        try {
            String stats = integration.getStats(mediaType);
            boolean isPlainResponse = !stats.startsWith("Error") && !stats.startsWith("Warning");
            return ResponseUtil.createSuccessResponse(connectorId, mediaType, "/health/backend/flows", stats, isPlainResponse);
        } catch (Exception e) {
            log.error("Get flow failed", e);
            return ResponseUtil.createFailureResponse(connectorId, mediaType, "/health/backend/flows", e.getMessage());
        }
    }

    @Operation(summary = "Get JVM statistics")
    @GetMapping(
            path = "/jvm",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getJvmStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) {
        long connectorId = 1L;

        try {
            final BackendResponse backendResponse = new BackendResponse();
            MemoryUsage mem = memoryMXBean.getHeapMemoryUsage();

            long used = mem.getUsed();
            long max = mem.getMax();
            long committed = mem.getCommitted();

            backendResponse.addMemory("current", convertSizeToKb(used));
            backendResponse.addMemory("max", convertSizeToKb(max));
            backendResponse.addMemory("committed", convertSizeToKb(committed));
            backendResponse.addMemory("cached", convertSizeToKb(committed - used));

            if (max > 0) {
                backendResponse.addMemory("currentUsedPercentage", (used * 100L) / max);
            } else {
                backendResponse.addMemory("currentUsedPercentage", 0L);
            }

            backendResponse.addThread("threadCount", threadMXBean.getThreadCount());
            backendResponse.addThread("peakThreadCount", threadMXBean.getPeakThreadCount());

            if (operatingSystemMXBean instanceof UnixOperatingSystemMXBean unixOs) {
                backendResponse.addJvm("openFileDescriptors", unixOs.getOpenFileDescriptorCount());
                backendResponse.addJvm("maxFileDescriptors", unixOs.getMaxFileDescriptorCount());
            } else {
                backendResponse.addJvm("openFileDescriptors", "Unknown");
                backendResponse.addJvm("maxFileDescriptors", "Unknown");
            }

            String jsonOutput = MAPPER.writeValueAsString(backendResponse);
            return ResponseUtil.createSuccessResponse(connectorId, mediaType, "/health/backend/jvm", jsonOutput, true);
        } catch (Exception e) {
            log.error("Get jvm failed", e);
            return ResponseUtil.createFailureResponse(connectorId, mediaType, "/health/backend/jvm", e.getMessage());
        }
    }

    private long convertSizeToKb(long size) {
        return size / 1024L;
    }

    private static final class BackendResponse {

        private Map<String, Object> jvm = new ConcurrentHashMap<>();
        private Map<String, Long> memory = new ConcurrentHashMap<>();
        private Map<String, Integer> threads = new ConcurrentHashMap<>();

        public Map<String, Long> getMemory() {
            return memory;
        }
        public void setMemory(Map<String, Long> memory) {
            this.memory = memory;
        }
        public void addMemory(String key, Long value) {
            this.memory.put(key, value);
        }

        public Map<String, Integer> getThreads() {
            return threads;
        }
        public void setThreads(Map<String, Integer> threads) {
            this.threads = threads;
        }
        public void addThread(String key, Integer value) {
            this.threads.put(key, value);
        }

        public Map<String, Object> getJvm() {
            return jvm;
        }
        public void setJvm(Map<String, Object> jvm) {
            this.jvm = jvm;
        }
        public void addJvm(String key, Object value) {
            this.jvm.put(key, value);
        }
    }

}