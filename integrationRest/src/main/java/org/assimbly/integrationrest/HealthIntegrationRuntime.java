package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import org.assimbly.integration.Integration;
import org.assimbly.util.rest.ResponseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Resource to return information about the currently running Spring profiles.
 */
@ControllerAdvice
@RestController
@RequestMapping("/health/backend")
public class HealthIntegrationRuntime {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

    @Autowired
    private IntegrationRuntime integrationRuntime;

    private boolean plainResponse;

    private Integration integration;

    @GetMapping(
            path = "/flows",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getFlowStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

        plainResponse = true;
        long connectorId = 1;
        integration = integrationRuntime.getIntegration();

        try {
            String stats = integration.getStats(mediaType);
            if(stats.startsWith("Error")||stats.startsWith("Warning")) {plainResponse = false;}
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(connectorId, mediaType, "/health/flow", stats, plainResponse);
        } catch (Exception e) {
            log.error("Get flow failed",e);
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/health/flow",e.getMessage());
        }
    }

    @GetMapping(
            path = "/jvm",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public ResponseEntity<String> getJvmStats(@Parameter(hidden = true) @RequestHeader(value = "Accept") String mediaType) throws Exception {

        plainResponse = true;
        long connectorId = 1;

        try {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            final ObjectMapper mapper = new ObjectMapper();
            final BackendResponse backendResponse = new BackendResponse();
            final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

            MemoryUsage mem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

            backendResponse.addMemory("current", convertSizeToKb(mem.getUsed()));
            backendResponse.addMemory("max", convertSizeToKb(mem.getMax()));
            backendResponse.addMemory("committed", convertSizeToKb(mem.getCommitted()));
            backendResponse.addMemory("cached", convertSizeToKb(mem.getCommitted() - mem.getUsed()));
            backendResponse.addMemory("currentUsedPercentage", (mem.getUsed() * 100 / mem.getMax()));

            backendResponse.addThread("threadCount", threadMXBean.getThreadCount());
            backendResponse.addThread("peakThreadCount", threadMXBean.getPeakThreadCount());

            backendResponse.addJvm("openFileDescriptors", invokeMethod("getOpenFileDescriptorCount"));
            backendResponse.addJvm("maxFileDescriptors", invokeMethod("getMaxFileDescriptorCount"));

            mapper.writeValue(out, backendResponse);
            return org.assimbly.util.rest.ResponseUtil.createSuccessResponse(connectorId, mediaType, "/health/jvm", out.toString(StandardCharsets.UTF_8), plainResponse);
        } catch (Exception e) {
            log.error("Get jvm failed",e);
            return ResponseUtil.createFailureResponse(connectorId, mediaType,"/health/jvm",e.getMessage());
        }
    }

    private long convertSizeToKb(double size) {
        return (long) (size / 1024);
    }

    private Object invokeMethod(String methodName) {
        try {
            Class<?> unixOS = Class.forName("com.sun.management.UnixOperatingSystemMXBean");

            if (unixOS.isInstance(operatingSystemMXBean))
                return unixOS.getMethod(methodName).invoke(operatingSystemMXBean);

        } catch (Throwable ignored) { }

        return "Unknown";
    }

    private class BackendResponse {

        private Map<String, Object> jvm = new HashMap<>();
        private Map<String, Long> memory = new HashMap<>();
        private Map<String, Integer> threads = new HashMap<>();

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
