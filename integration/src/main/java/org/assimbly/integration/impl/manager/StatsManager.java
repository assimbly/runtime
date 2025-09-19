package org.assimbly.integration.impl.manager;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteGroupMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.support.CamelContextHelper;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.event.collect.MicrometerTimestampRoutePolicyFactory;
import org.assimbly.docconverter.DocConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class StatsManager {

    protected static final Logger log = LoggerFactory.getLogger(StatsManager.class);

    private final CamelContext context;
    private final ManagedCamelContext managedContext;
    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private final FlowManager flowManager;

    public StatsManager(CamelContext context, FlowManager flowManager) {
        this.context = context;
        this.flowManager = flowManager;
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

    public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject flow = createBasicFlowJson(flowId);

        // Calculate basic statistics
        FlowStatistics stats = calculateFlowStatistics(flowId, fullStats);

        // Populate basic stats
        populateBasicStats(flow, stats);

        // Add additional stats if requested
        if (fullStats) {
            populateDetailedStats(flow, stats);

            // Add steps if requested
            if (includeSteps) {
                JSONArray steps = collectStepStatistics(flowId);
                json.put("steps", steps);
            }

        }

        // Add metadata if requested
        if (includeMetaData) {
            populateMetadata(flow, flowId, flowsMap);
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

    private FlowStatistics calculateFlowStatistics(String flowId, boolean fullStats) {

        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);

        FlowStatistics stats = new FlowStatistics();
        if(managedRouteGroup==null){
            return stats;
        }

        stats.status = managedRouteGroup.getState();
        stats.totalMessages = managedRouteGroup.getExchangesTotal();
        stats.completedMessages = managedRouteGroup.getExchangesCompleted();
        stats.failedMessages = managedRouteGroup.getExchangesFailed();
        stats.pendingMessages = managedRouteGroup.getExchangesInflight();

        if (fullStats) {
            stats.uptimeMillis = managedRouteGroup.getUptimeMillis();
            stats.uptime = managedRouteGroup.getUptime();
            stats.lastFailed = managedRouteGroup.getLastExchangeFailureTimestamp() != null ? managedRouteGroup.getLastExchangeFailureTimestamp().getTime() : 0;
            stats.lastCompleted = managedRouteGroup.getLastExchangeCompletedTimestamp() != null ? managedRouteGroup.getLastExchangeCompletedTimestamp().getTime() : 0;
        }

        return stats;
    }

    private void populateBasicStats(JSONObject flow, FlowStatistics stats) {
        flow.put("total", stats.totalMessages);
        flow.put("completed", stats.completedMessages);
        flow.put("failed", stats.failedMessages);
        flow.put("pending", stats.pendingMessages);
    }

    private void populateDetailedStats(JSONObject flow, FlowStatistics stats) {
        flow.put("status", stats.status);
        flow.put("timeout", getTimeout(context));
        flow.put("uptime", stats.uptime);
        flow.put("uptimeMillis", stats.uptimeMillis);
        flow.put("lastFailed", stats.lastFailed);
        flow.put("lastCompleted", stats.lastCompleted);
    }

    private void populateMetadata(JSONObject flow, String flowId, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
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

    private JSONArray collectStepStatistics(String flowId) throws Exception {
        JSONArray steps = new JSONArray();
        List<Route> routes = context.getRoutesByGroup(flowId);
        for (Route route : routes) {
            String routeId = route.getId();
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
        String status = null;
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

    public String getFlowStepStats(String flowId, String stepid, boolean fullStats) throws Exception {

        String routeid = flowId + "-" + stepid;

        JSONObject json = getStepStats(routeid, fullStats);
        return json.toString(4);

    }

    private JSONObject getStepStats(String routeid, boolean fullStats) throws Exception {

        JSONObject json = new JSONObject();
        JSONObject step = new JSONObject();

        ManagedRouteMBean route = managedContext.getManagedRoute(routeid);

        step.put("id", routeid);
        step.put("status", route.getState());

        if (route.getState().equals("started")) {

            if (fullStats) {
                String stepUptime = route.getUptime();
                String stepUptimeMilliseconds = Long.toString(route.getUptimeMillis());

                step.put("uptime", stepUptime);
                step.put("uptimeMilliseconds", stepUptimeMilliseconds);

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

        List<ManagedRouteMBean> routes = managedContext.getManagedRoutesByGroup(flowId);

        for (ManagedRouteMBean route : routes) {

            totalMessages += route.getExchangesTotal();
            completedMessages += route.getExchangesCompleted();
            failedMessages += route.getExchangesFailed();
            pendingMessages += route.getExchangesInflight();

            if (includeSteps) {
                JSONObject step = new JSONObject();
                String routeId = route.getRouteId();
                String stepId = StringUtils.substringAfter(routeId, flowId + "-");
                step.put("id", stepId);
                step.put("total", route.getExchangesTotal());
                step.put("completed", route.getExchangesCompleted());
                step.put("failed", route.getExchangesFailed());
                step.put("pending", route.getExchangesInflight());
                steps.put(step);
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
        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);
        return Long.toString(managedRouteGroup.getExchangesTotal());
    }

    public String getFlowCompletedMessages(String flowId) {
        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);
        return Long.toString(managedRouteGroup.getExchangesCompleted());

    }

    public String getFlowFailedMessages(String flowId) {
        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);
        return Long.toString(managedRouteGroup.getExchangesFailed());
    }

    public String getFlowPendingMessages(String flowId) {
        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);
        return Long.toString(managedRouteGroup.getExchangesInflight());
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

        Set<String> flowIds = flowManager.getListOfFlowIds(null);

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

        List<Route> routes = context.getRoutesByGroup(flowId);

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

    public String getFlowsStats(String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

        Set<String> flowIds = new HashSet<>();

        List<Route> routes = context.getRoutes();

        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                flowIds.add(flowId);
            }
        }

        String result = getStatsFromList(flowIds, true, flowsMap);

        if (mediaType.contains("xml")) {
            result = DocConverter.convertJsonToXml(result);
        }

        return result;

    }

    public String getStatsByFlowIds(String flowIds) {

        String[] values = flowIds.split(",");

        Set<String> flowSet = new HashSet<>(Arrays.asList(values));

        JSONArray flows = new JSONArray();

        for (String flowId : flowSet) {
            JSONObject json = new JSONObject();
            JSONObject flow = createBasicFlowJson(flowId);

            FlowStatistics stats = calculateFlowStatistics(flowId, true);

            // Populate basic stats
            populateBasicStats(flow, stats);
            populateDetailedStats(flow, stats);

            // Build final response
            json.put("flow", flow);

            flows.put(json);
        }

        return flows.toString();

    }

    private String getStatsFromList(Set<String> flowIds, boolean fullStats, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

        JSONArray flows = new JSONArray();

        for (String flowId : flowIds) {
            String flowStats = getFlowStats(flowId, fullStats, false, false, flowsMap);
            JSONObject flow = new JSONObject(flowStats);
            flows.put(flow);
        }

        return flows.toString();

    }

    public String getMessages(String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

        Set<String> flowIds = new HashSet<>();

        List<Route> routes = context.getRoutes();

        for (Route route : routes) {
            String routeId = route.getId();
            String flowId = StringUtils.substringBefore(routeId, "-");
            if (flowId != null && !flowId.isEmpty()) {
                flowIds.add(flowId);
            }
        }

        String result = getStatsFromList(flowIds, false, flowsMap);

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

    public String countFlows(String filter) {

        Set<String> flowIds = flowManager.getListOfFlowIds(filter);

        return Integer.toString(flowIds.size());

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

}