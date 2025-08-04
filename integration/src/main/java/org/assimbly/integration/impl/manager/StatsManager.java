package org.assimbly.integration.impl.manager;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.spi.RouteController;
import org.apache.camel.support.CamelContextHelper;
import org.apache.camel.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.collect.MicrometerTimestampRoutePolicyFactory;
import org.assimbly.docconverter.DocConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.JMX;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class StatsManager {

    protected static final Logger log = LoggerFactory.getLogger(StatsManager.class);

    private final CamelContext context;
    private final ManagedCamelContext managedContext;
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final ConcurrentMap<String, TreeMap<String, String>> flowsMap;

    public StatsManager(CamelContext context, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        this.context = context;
        this.flowsMap = flowsMap;
        this.managedContext = context.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
    }

    public void setHistoryMetrics() {
        //set history metrics
        MetricsMessageHistoryFactory factory = new MetricsMessageHistoryFactory();
        factory.setPrettyPrint(true);
        factory.setMetricsRegistry(metricRegistry);
        context.setMessageHistoryFactory(factory);
        context.setSourceLocationEnabled(true);
    }

    public void setMetrics() {
        context.addRoutePolicyFactory(new MetricsRoutePolicyFactory());

        //Add custom micrometerpolicyfactor that add support for timestamps
        MicrometerTimestampRoutePolicyFactory micrometerTimestampRoutePolicy = new MicrometerTimestampRoutePolicyFactory(meterRegistry);
        micrometerTimestampRoutePolicy.setMeterRegistry(meterRegistry);
        context.addRoutePolicyFactory(micrometerTimestampRoutePolicy);
    }


    public void setHealthChecks(boolean enable) {

        HealthCheckRepository routesHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "routes");
        if (routesHealthCheckRepository != null) {
            routesHealthCheckRepository.setEnabled(enable);
        }
        HealthCheckRepository consumersHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "consumers");
        if (consumersHealthCheckRepository != null) {
            consumersHealthCheckRepository.setEnabled(enable);
        }

        HealthCheckRepository producersHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "producers");
        if (producersHealthCheckRepository != null) {
            producersHealthCheckRepository.setEnabled(enable);
        }
    }

    public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, String filter) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject flow = createBasicFlowJson(flowId);

        List<Route> routes = getRoutesByFlowId(flowId);
        if (filter != null && !filter.isEmpty()) {
            routes = filterRoutes(routes, filter);
        }

        // Calculate basic statistics
        FlowStatistics stats = calculateFlowStatistics(routes, fullStats);

        // Populate basic stats
        populateBasicStats(flow, stats);

        // Add additional stats if requested
        if (fullStats) {
            populateDetailedStats(flow, stats);

            // Add steps if requested
            if (includeSteps) {
                JSONArray steps = collectStepStatistics(routes);
                json.put("steps", steps);
            }

        }

        // Add metadata if requested
        if (includeMetaData) {
            populateMetadata(flow, flowId);
        }

        // Build final response
        json.put("flow", flow);

        return json.toString();
    }

    private JSONObject createBasicFlowJson(String flowId) {
        JSONObject flow = new JSONObject();
        flow.put("id", flowId);
        return flow;
    }

    private List<Route> filterRoutes(List<Route> routes, String filter) {
        if (filter.isEmpty()) {
            return routes;
        }

        return routes.stream()
                .filter(r -> !r.getId().contains(filter))
                .toList();
    }

    private FlowStatistics calculateFlowStatistics(List<Route> routes, boolean fullStats) {

        FlowStatistics stats = new FlowStatistics();
        long total = 0;
        long completed = 0;
        long failed = 0;
        long pending = 0;
        long lastFailed;
        long lastCompleted;

        List<Long> uptimeList = new ArrayList<>();
        List<Long> lastFailedList = new ArrayList<>();
        List<Long> lastCompletedList = new ArrayList<>();

        for (Route r : routes) {

            String routeId = r.getId();

            total += getCounter("camel.exchanges.total", routeId);
            failed += getCounter("camel.exchanges.failed", routeId);
            completed += getCounter("camel.exchanges.succeeded", routeId);
            pending += getGauge("camel.exchanges.inflight", routeId);

            if (fullStats) {
                if (stats.uptime == null) {
                    uptimeList.add(r.getUptimeMillis());
                }
                lastFailed = getGauge("camel.exchanges.lastfailure.timestamp", routeId);
                if (lastFailed != 0) {
                    lastFailedList.add(lastFailed);
                }

                lastCompleted = getGauge("camel.exchanges.lastcompleted.timestamp", routeId);
                if (lastCompleted != 0) {
                    lastCompletedList.add(lastCompleted);
                }

            }

        }

        stats.totalMessages = total;
        stats.completedMessages = completed;
        stats.failedMessages = failed;
        stats.pendingMessages = pending;

        if (fullStats) {
            stats.uptimeMillis = uptimeList.stream()
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L);
            stats.uptime = TimeUtils.printDuration(stats.uptimeMillis);
            stats.lastFailed = lastFailedList.stream()
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L);
            stats.lastCompleted = lastCompletedList.stream()
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L);
        }

        return stats;
    }

    private long getCounter(String metricName, String routeId) {

        try {
            Counter counter = meterRegistry.get(metricName)
                    .tag("kind", "CamelRoute")
                    .tag("routeId", routeId)
                    .counter();
            return (long) counter.count();
        } catch (MeterNotFoundException meterNotFoundException) {
            return 0L;
        }

    }

    private long getGauge(String metricName, String routeId) {

        try {
            Gauge gauge = meterRegistry.get(metricName)
                    .tag("kind", "CamelRoute")
                    .tag("routeId", routeId)
                    .gauge();
            return (long) gauge.value();
        } catch (MeterNotFoundException meterNotFoundException) {
            return 0L;
        }

    }

    private void populateBasicStats(JSONObject flow, FlowStatistics stats) {
        flow.put("total", stats.totalMessages);
        flow.put("completed", stats.completedMessages);
        flow.put("failed", stats.failedMessages);
        flow.put("pending", stats.pendingMessages);
    }

    private void populateDetailedStats(JSONObject flow, FlowStatistics stats) {
        flow.put("status", getFlowStatus(flow.getString("id")));
        flow.put("timeout", getTimeout(context));
        flow.put("uptime", stats.uptime);
        flow.put("uptimeMillis", stats.uptimeMillis);
        flow.put("lastFailed", stats.lastFailed);
        flow.put("lastCompleted", stats.lastCompleted);
    }

    private void populateMetadata(JSONObject flow, String flowId) {
        TreeMap<String, String> flowProps = flowsMap.get(flowId);
        if (flowProps != null) {
            for (var flowProp : flowProps.entrySet()) {
                if (flowProp.getKey().startsWith("flow") && !flowProp.getKey().endsWith("id")) {
                    String key = StringUtils.substringAfter(flowProp.getKey(), "flow.");
                    flow.put(key, flowProp.getValue());
                }
            }
        }
    }

    private JSONArray collectStepStatistics(List<Route> routes) throws Exception {
        JSONArray steps = new JSONArray();
        for (Route r : routes) {
            String routeId = r.getId();
            JSONObject step = getStepStats(routeId, true);
            steps.put(step);
        }
        return steps;
    }

    // Helper class to store statistics
    private static class FlowStatistics {
        long totalMessages = 0;
        long completedMessages = 0;
        long failedMessages = 0;
        long pendingMessages = 0;
        long uptimeMillis = 0;
        long lastFailed = 0;
        long lastCompleted = 0;
        String uptime = null;

    }

    private long getTimeout(CamelContext context) {
        try {
            String managementName = context.getManagementNameStrategy().getName();
            ObjectName objectName = context.getManagementStrategy().getManagementObjectNameStrategy().getObjectNameForCamelContext(managementName, context.getName());

            ManagedCamelContextMBean managedCamelContextMBean = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), objectName, ManagedCamelContextMBean.class);
            return managedCamelContextMBean.getTimeout();
        } catch (Exception e) {
            return 0L;
        }
    }

    public String getFlowStepStats(String flowId, String stepid, boolean fullStats, String mediaType) throws Exception {

        String routeid = flowId + "-" + stepid;

        JSONObject json = getStepStats(routeid, fullStats);
        String stepStats = json.toString(4);
        if (mediaType.contains("xml")) {
            stepStats = DocConverter.convertJsonToXml(stepStats);
        }

        return stepStats;
    }

    private JSONObject getStepStats(String routeid, boolean fullStats) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject step = new JSONObject();

        ManagedRouteMBean route = managedContext.getManagedRoute(routeid);

        String stepStatus = getFlowStatus(routeid);

        step.put("id", routeid);
        step.put("status", stepStatus);

        if (route != null && stepStatus.equals("started")) {

            if (fullStats) {
                String stepUptime = getFlowUptime(routeid);
                String stepUptimeMilliseconds = Long.toString(route.getUptimeMillis());

                step.put("uptime", stepUptime);
                step.put("uptimeMilliseconds", stepUptimeMilliseconds);

                JSONObject load = new JSONObject();

                String stepLoad01 = route.getLoad01();
                String stepLoad05 = route.getLoad05();
                String stepLoad15 = route.getLoad15();

                load.put("cpuLoadLastMinute", stepLoad01);
                load.put("cpuLoadLast5Minutes", stepLoad05);
                load.put("cpuLoadLast15Minutes", stepLoad15);

                step.put("load", load);
            }

            String statsAsXml = route.dumpStatsAsXml(fullStats);
            String statsAsJson = DocConverter.convertXmlToJson(statsAsXml);
            JSONObject stepStatsObject = new JSONObject(statsAsJson);
            step.put("stats", stepStatsObject.get("stats"));
        }

        json.put("step", step);

        return json;

    }


    public String getFlowMessages(String flowId, boolean includeSteps, String mediaType) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject flow = new JSONObject();
        JSONArray steps = new JSONArray();

        long totalMessages = 0;
        long completedMessages = 0;
        long failedMessages = 0;
        long pendingMessages = 0;

        List<Route> routes = getRoutesByFlowId(flowId);

        for (Route r : routes) {
            String routeId = r.getId();

            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);
            if (route != null) {
                totalMessages += route.getExchangesTotal();
                completedMessages += route.getExchangesCompleted();
                failedMessages += route.getExchangesFailed();
                pendingMessages += route.getExchangesInflight();
                if (includeSteps) {
                    JSONObject step = new JSONObject();
                    String stepId = StringUtils.substringAfter(routeId, flowId + "-");
                    step.put("id", stepId);
                    step.put("total", route.getExchangesTotal());
                    step.put("completed", route.getExchangesCompleted());
                    step.put("failed", route.getExchangesFailed());
                    step.put("pending", route.getExchangesInflight());
                    steps.put(step);
                }
            }
        }

        flow.put("id", flowId);
        flow.put("total", totalMessages);
        flow.put("completed", completedMessages);
        flow.put("failed", failedMessages);
        flow.put("pending", pendingMessages);
        if (includeSteps) {
            flow.put("steps", steps);
        }
        json.put("flow", flow);

        String flowStats = json.toString(4);
        if (mediaType.contains("xml")) {
            flowStats = DocConverter.convertJsonToXml(flowStats);
        }

        return flowStats;

    }

    public String getFlowTotalMessages(String flowId) {

        List<Route> routeList = getRoutesByFlowId(flowId);

        long totalMessages = 0;

        for (Route r : routeList) {
            String routeId = r.getId();
            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);

            if (route != null) {
                totalMessages += route.getExchangesTotal();
            }
        }

        return Long.toString(totalMessages);

    }

    public String getFlowCompletedMessages(String flowId) {

        List<Route> routeList = getRoutesByFlowId(flowId);
        long completedMessages = 0;

        for (Route r : routeList) {
            String routeId = r.getId();

            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);
            if (route != null) {
                completedMessages += route.getExchangesCompleted();
            }
        }

        return Long.toString(completedMessages);

    }

    public String getFlowFailedMessages(String flowId) {

        List<Route> routeList = getRoutesByFlowId(flowId);
        long failedMessages = 0;

        for (Route r : routeList) {
            String routeId = r.getId();

            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);
            if (route != null) {
                failedMessages += route.getExchangesFailed();
            }
        }

        return Long.toString(failedMessages);

    }

    public String getFlowPendingMessages(String flowId) {

        List<Route> routeList = getRoutesByFlowId(flowId);
        long pendingMessages = 0;

        for (Route r : routeList) {
            String routeId = r.getId();

            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);
            if (route != null) {
                pendingMessages += route.getExchangesInflight();
            }
        }

        return Long.toString(pendingMessages);

    }

    public String getStepMessages(String flowId, String stepId, String mediaType) throws Exception {

        long totalMessages = 0;
        long completedMessages = 0;
        long failedMessages = 0;
        long pendingMessages = 0;

        String routeId = flowId + "-" + stepId;

        ManagedRouteMBean route = managedContext.getManagedRoute(routeId);
        if (route != null) {
            totalMessages += route.getExchangesTotal();
            completedMessages += route.getExchangesCompleted();
            failedMessages += route.getExchangesFailed();
            pendingMessages += route.getExchangesInflight();
        }

        JSONObject json = new JSONObject();
        JSONObject step = new JSONObject();

        step.put("id", flowId);
        step.put("total", totalMessages);
        step.put("completed", completedMessages);
        step.put("failed", failedMessages);
        step.put("pending", pendingMessages);
        json.put("step", step);

        String flowStats = json.toString(4);
        if (mediaType.contains("xml")) {
            flowStats = DocConverter.convertJsonToXml(flowStats);
        }

        return flowStats;

    }


    public String getHealth(String type, String mediaType) throws Exception {

        Set<String> flowIds = new HashSet<>();

        List<Route> routes = context.getRoutes();

        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                flowIds.add(flowId);
            }
        }

        String result = getHealthFromList(flowIds, type);

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;
    }

    public String getHealthByFlowIds(String flowIds, String type, String mediaType) throws Exception {

        String[] values = flowIds.split(",");

        Set<String> flowSet = new HashSet<>(Arrays.asList(values));

        String result = getHealthFromList(flowSet, type);

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;

    }


    private String getHealthFromList(Set<String> flowIds, String type) throws Exception {

        JSONArray flows = new JSONArray();

        for (String flowId : flowIds) {
            String flowHealth = getFlowHealth(flowId, type, false, false, false, "application/json");
            JSONObject flow = new JSONObject(flowHealth);
            flows.put(flow);
        }

        return flows.toString();

    }

    public String getFlowHealth(String flowId, String type, boolean includeSteps, boolean includeError, boolean includeDetails, String mediaType) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject flow = new JSONObject();
        JSONArray steps = new JSONArray();

        String state = "UNKNOWN";

        List<Route> routes = getRoutesByFlowId(flowId);

        for (Route r : routes) {

            String routeId = r.getId();
            String healthCheckId = type + ":" + routeId;
            JSONObject step = getStepHealth(routeId, healthCheckId, includeError, includeDetails);

            String stepState = step.getJSONObject("step").getString("state");
            if (!state.equalsIgnoreCase("DOWN")) {
                state = stepState;
            }
            steps.put(step);

        }

        flow.put("id", flowId);
        flow.put("state", state);

        if (includeSteps) {
            flow.put("steps", steps);
        }
        json.put("flow", flow);

        String flowStats = json.toString(4);

        if (mediaType.contains("xml")) {
            flowStats = DocConverter.convertJsonToXml(flowStats);
        }

        return flowStats;

    }

    public String getFlowStepHealth(String flowId, String stepId, String type, boolean includeError, boolean includeDetails, String mediaType) throws Exception {

        String routeid = flowId + "-" + stepId;
        String healthCheckId = type + ":" + routeid;

        JSONObject json = getStepHealth(routeid, healthCheckId, includeError, includeDetails);
        String stepHealth = json.toString(4);
        if (mediaType.contains("xml")) {
            stepHealth = DocConverter.convertJsonToXml(stepHealth);
        }

        return stepHealth;
    }

    private JSONObject getStepHealth(String routeid, String healthCheckId, boolean includeError, boolean includeDetails) {

        JSONObject json = new JSONObject();
        JSONObject step = new JSONObject();

        step.put("id", routeid);

        HealthCheck healthCheck = HealthCheckHelper.getHealthCheck(context, healthCheckId);

        if (healthCheck != null && healthCheck.isReadiness()) {

            HealthCheck.Result result = healthCheck.callReadiness();
            step.put("state", result.getState().toString());

            if (includeError) {
                JSONObject error = new JSONObject();
                Optional<Throwable> errorResultOptional = result.getError();
                if (errorResultOptional.isPresent()) {
                    Throwable errorResult = errorResultOptional.get();
                    error.put("message", errorResult.getMessage());
                    error.put("class", errorResult.getClass().getName());
                }
                step.put("error", error);
            }

            if (includeDetails) {
                JSONObject details = new JSONObject();

                for (Map.Entry<String, Object> entry : result.getDetails().entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    details.put(key, value);
                }

                step.put("details", details);
            }

        } else {
            step.put("state", "UNKNOWN");
        }


        json.put("step", step);

        return json;
    }

    public String getStats(String mediaType) throws Exception {

        JSONObject json = new JSONObject();

        ManagedCamelContextMBean managedCamelContext = managedContext.getManagedCamelContext();

        json.put("camelId", managedCamelContext.getCamelId());
        json.put("camelVersion", managedCamelContext.getCamelVersion());
        json.put("status", managedCamelContext.getState());
        json.put("uptime", managedCamelContext.getUptime());
        json.put("uptimeMillis", managedCamelContext.getUptimeMillis());
        json.put("startedFlows", countFlows("started"));
        json.put("startedSteps", managedCamelContext.getStartedRoutes());
        json.put("exchangesTotal", managedCamelContext.getExchangesTotal());
        json.put("exchangesCompleted", managedCamelContext.getExchangesCompleted());
        json.put("exchangesInflight", managedCamelContext.getExchangesInflight());
        json.put("exchangesFailed", managedCamelContext.getExchangesFailed());
        json.put("cpuLoadLastMinute", managedCamelContext.getLoad01());
        json.put("cpuLoadLast5Minutes", managedCamelContext.getLoad05());
        json.put("cpuLoadLast15Minutes", managedCamelContext.getLoad15());
        json.put("memoryUsage", getMemoryUsage());
        json.put("totalThreads", ManagementFactory.getThreadMXBean().getThreadCount());

        String stats = json.toString(4);
        if (mediaType.contains("xml")) {
            stats = DocConverter.convertJsonToXml(stats);
        }

        return stats;

    }

    public String getThreads(String mediaType, String filter, int topEntries) throws Exception {

        List<JSONObject> jsonObjectList = new ArrayList<>();
        ThreadInfo[] threadInfoArray = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true, 1);

        for (ThreadInfo threadInfo : threadInfoArray) {
            JSONObject thread = new JSONObject();
            thread.put("id", threadInfo.getThreadId());
            thread.put("name", threadInfo.getThreadName());
            thread.put("status", threadInfo.getThreadState().name());
            thread.put("cpuTime", ManagementFactory.getThreadMXBean().getThreadCpuTime(threadInfo.getThreadId()));
            jsonObjectList.add(thread);
        }

        // Filter by name
        if (!filter.isEmpty()) {

            jsonObjectList = jsonObjectList.stream()
                    .filter(obj -> obj.getString("name").contains(filter))
                    .toList();
        }


        // Filter by top entries
        if (topEntries >= 1) {
            if (topEntries > jsonObjectList.size()) {
                topEntries = jsonObjectList.size();
            }
            jsonObjectList = jsonObjectList.subList(0, topEntries);
        }

        // Sort by cpuTime
        List<JSONObject> sortedList = jsonObjectList.stream()
                .sorted(Comparator.comparingInt((JSONObject o) -> o.getInt("cpuTime")).reversed())
                .toList();

        // Rebuild the JSONArray from the sorted and filtered list
        JSONArray jsonArray = new JSONArray(sortedList);
        String result = jsonArray.toString();

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;

    }

    private double getMemoryUsage() {

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();

        long maxMemory = heapMemoryUsage.getMax(); // Maximum available memory
        long usedMemory = heapMemoryUsage.getUsed(); // Currently used memory

        double memoryUsagePercentage = ((double) usedMemory / maxMemory) * 100;

        DecimalFormat df = new DecimalFormat("#.##");
        memoryUsagePercentage = Double.parseDouble(df.format(memoryUsagePercentage));

        return memoryUsagePercentage;

    }

    public String getStepsStats(String mediaType) throws Exception {

        ManagedCamelContextMBean managedCamelContext = managedContext.getManagedCamelContext();

        String result = managedCamelContext.dumpRoutesStatsAsXml(true, false);

        if (mediaType.contains("json")) {
            result = DocConverter.convertXmlToJson(result);
        }

        return result;

    }

    public String getFlowsStats(String mediaType) throws Exception {

        Set<String> flowIds = new HashSet<>();

        List<Route> routes = context.getRoutes();

        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                flowIds.add(flowId);
            }
        }

        String result = getStatsFromList(flowIds, true);

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;

    }

    public String getStatsByFlowIds(String flowIds, String filter) {

        String[] values = flowIds.split(",");

        Set<String> flowSet = new HashSet<>(Arrays.asList(values));

        JSONArray flows = new JSONArray();

        for (String flowId : flowSet) {
            JSONObject json = new JSONObject();
            JSONObject flow = createBasicFlowJson(flowId);

            List<Route> routes = getRoutesByFlowId(flowId);

            if (filter != null && !filter.isEmpty()) {
                routes = filterRoutes(routes, filter);
            }

            // Calculate basic statistics
            FlowStatistics stats = calculateFlowStatistics(routes, true);

            // Populate basic stats
            populateBasicStats(flow, stats);
            populateDetailedStats(flow, stats);

            // Build final response
            json.put("flow", flow);

            flows.put(json);
        }

        return flows.toString();

    }

    private String getStatsFromList(Set<String> flowIds, boolean fullStats) throws Exception {

        JSONArray flows = new JSONArray();

        for (String flowId : flowIds) {
            String flowStats = getFlowStats(flowId, fullStats, false, false, "");
            JSONObject flow = new JSONObject(flowStats);
            flows.put(flow);
        }

        return flows.toString();

    }

    public String getMessages(String mediaType) throws Exception {

        Set<String> flowIds = new HashSet<>();

        List<Route> routes = context.getRoutes();

        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                flowIds.add(flowId);
            }
        }

        String result = getStatsFromList(flowIds, false);

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;

    }

    public String getMetrics(String mediaType) throws Exception {

        String integrationStats = "0";
        MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);

        if (metricsService != null) {
            integrationStats = metricsService.dumpStatisticsAsJson();
            if (mediaType.contains("xml")) {
                integrationStats = DocConverter.convertJsonToXml(integrationStats);
            }
        }

        return integrationStats;

    }


    public String getHistoryMetrics(String mediaType) throws Exception {

        String integrationStats = "0";

        MetricsMessageHistoryService historyService = context.hasService(MetricsMessageHistoryService.class);

        if (historyService != null) {
            integrationStats = historyService.dumpStatisticsAsJson();
            if (mediaType.contains("xml")) {
                integrationStats = DocConverter.convertJsonToXml(integrationStats);
            }
        }

        return integrationStats;

    }

    public String info(String mediaType) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject info = new JSONObject();

        info.put("name", context.getName());
        info.put("version", context.getVersion());
        info.put("startDate", CamelContextHelper.getStartDate(context));
        info.put("startupType", context.getStartupSummaryLevel());
        info.put("uptime", context.getUptime());
        info.put("uptimeMiliseconds", context.getUptime().toMillis());
        info.put("numberOfRunningSteps", context.getRoutesSize());

        json.put("info", info);

        String integrationInfo = json.toString(4);
        if (mediaType.contains("xml")) {
            integrationInfo = DocConverter.convertJsonToXml(integrationInfo);
        }

        return integrationInfo;

    }

    private List<Route> getRoutesByFlowId(String id) {
        return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).toList();
    }

    public String countFlows(String filter) {

        Set<String> flowIds = getListOfFlowIds(filter);

        return Integer.toString(flowIds.size());

    }

    private Set<String> getListOfFlowIds(String filter) {

        //get all routes
        List<Route> routes = context.getRoutes();

        Set<String> flowIds = new HashSet<>();

        //filter flows from routes
        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                if (filter != null && !filter.isEmpty()) {
                    String serviceStatus = getFlowStatus(flowId);
                    if (serviceStatus.equalsIgnoreCase(filter)) {
                        flowIds.add(flowId);
                    }
                } else {
                    flowIds.add(flowId);
                }
            }
        }

        return flowIds;

    }

    public String countSteps(String filter) {

        List<Route> routes = context.getRoutes();

        Set<String> stepIds = new HashSet<>();

        for (Route route : routes) {
            String routeId = route.getId();
            ManagedRouteMBean managedRoute = managedContext.getManagedRoute(routeId);

            if (filter != null && !filter.isEmpty()) {
                String serviceStatus = managedRoute.getState();
                if (serviceStatus.equalsIgnoreCase(filter)) {
                    stepIds.add(routeId);
                }
            } else {
                stepIds.add(routeId);
            }
        }

        return Integer.toString(stepIds.size());

    }


    public static BigDecimal parseBigDecimal(String value) {
        if (value == null || value.isEmpty()) {
            return BigDecimal.ZERO;  // or handle as needed
        }
        return new BigDecimal(value);
    }

    public String getFlowStatus(String id) {

        RouteController routeController = context.getRouteController();

        String flowStatus;
        if (hasFlow(id)) {
            String updatedId;
            if (id.contains("-")) {
                updatedId = id;
            } else {
                updatedId = id + "-";
            }

            try {
                List<Route> routesList = getRoutesByFlowId(updatedId);
                if (routesList.isEmpty()) {
                    flowStatus = "unconfigured";
                } else {
                    String flowId = routesList.getFirst().getId();
                    ServiceStatus serviceStatus = routeController.getRouteStatus(flowId);
                    flowStatus = serviceStatus.toString().toLowerCase();
                }
            } catch (Exception e) {
                log.error("Get status flow {} failed.", id, e);

                flowStatus = "error: " + e.getMessage();
            }

        } else {
            flowStatus = "unconfigured";
        }

        return flowStatus;

    }

    public boolean hasFlow(String id) {

        boolean routeFound = false;

        if (context != null) {
            for (Route route : context.getRoutes()) {
                if (route.getId().startsWith(id)) {
                    routeFound = true;
                }
            }
        }
        return routeFound;
    }


    public String getFlowUptime(String flowId) {

        String flowUptime;
        if (hasFlow(flowId)) {
            Route route = getRoutesByFlowId(flowId).getFirst();
            flowUptime = route.getUptime();
        } else {
            flowUptime = "0";
        }

        return flowUptime;
    }

}
