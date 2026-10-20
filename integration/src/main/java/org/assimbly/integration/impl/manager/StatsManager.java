package org.assimbly.integration.impl.manager;

import javax.management.*;
import java.util.*;

import com.codahale.metrics.MetricRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedPerformanceCounterMBean;
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
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.util.concurrent.ConcurrentMap;

public class StatsManager {

    protected static final Logger log = LoggerFactory.getLogger(StatsManager.class);

    // Jackson nodes keep insertion order (org.json does not), so stats fields are returned in the order they are put
    private static final JsonMapper mapper = JsonMapper.builder().build();

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

    public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        return buildFlowStats(flowId, fullStats, includeMetaData, includeSteps, flowsMap).toString();
    }

    private ObjectNode buildFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        ObjectNode json = mapper.createObjectNode();
        ObjectNode flow = createBasicFlowJson(flowId);

        // Calculate basic statistics
        FlowStatistics stats = calculateFlowStatistics(flowId, fullStats);

        // Populate basic stats
        populateBasicStats(flow, stats);

        // Add additional stats if requested
        if (fullStats) {
            populateDetailedStats(flow, stats);
        }

        // Add metadata if requested
        if (includeMetaData) {
            populateMetadata(flow, flowId, flowsMap);
        }

        // Build final response
        json.set("flow", flow);

        // Add steps if requested
        if (fullStats && includeSteps) {
            json.set("steps", collectStepStatistics(flowId));
        }

        return json;
    }

    public void resetStats() {

        Set<String> flowsIds = context.getRouteGroupIds();

        for (String flowId : flowsIds) {
            resetFlowStatistics(flowId);
        }

    }    

    public void resetFlowStats(String flowId) {
        resetFlowStatistics(flowId);
    }

    private ObjectNode createBasicFlowJson(String flowId) {
        ObjectNode flow = mapper.createObjectNode();
        flow.put("id", flowId);
        return flow;
    }

    private FlowStatistics calculateFlowStatistics(String flowId, boolean fullStats) {

        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);

        FlowStatistics stats = new FlowStatistics();
        stats.status = flowManager.getFlowStatus(flowId);

        if(managedRouteGroup==null){
            return stats;
        }

        // Transactions count each message once (route group), exchanges count it once per step (routes)
        stats.transactions = Counts.of(managedRouteGroup);
        stats.exchanges = sumOfRoutes(context.getRoutesByGroup(flowId));

        if (fullStats) {
            stats.uptimeMillis = managedRouteGroup.getUptimeMillis();
            stats.uptime = managedRouteGroup.getUptime();
            // failed counts both unhandled and handled failures, so take the latest of both
            stats.lastFailed = Math.max(toMillis(managedRouteGroup.getLastExchangeFailureTimestamp()), toMillis(managedRouteGroup.getLastExchangeFailureHandledTimestamp()));
            stats.lastCompleted = toMillis(managedRouteGroup.getLastExchangeCompletedTimestamp());
            stats.meanProcessingTime = managedRouteGroup.getMeanProcessingTime();
            stats.maxProcessingTime = managedRouteGroup.getMaxProcessingTime();
            stats.lastProcessingTime = managedRouteGroup.getLastProcessingTime();
        }

        return stats;
    }

    private Counts sumOfRoutes(List<Route> routes) {
        Counts sum = Counts.NONE;
        for (Route route : routes) {
            ManagedRouteMBean managedRoute = managedContext.getManagedRoute(route.getId());
            if (managedRoute != null) {
                sum = sum.plus(Counts.of(managedRoute));
            }
        }
        return sum;
    }

    private static long toMillis(Date date) {
        return date != null ? date.getTime() : 0L;
    }

    // Exchange counters of Camel, with handled failures counted as failed instead of completed
    private record Counts(long total, long completed, long failed, long pending) {

        static final Counts NONE = new Counts(0, 0, 0, 0);

        static Counts of(ManagedPerformanceCounterMBean counter) {
            return new Counts(
                    counter.getExchangesTotal(),
                    counter.getExchangesCompleted() - counter.getFailuresHandled(),
                    counter.getExchangesFailed() + counter.getFailuresHandled(),
                    counter.getExchangesInflight());
        }

        Counts plus(Counts other) {
            return new Counts(total + other.total, completed + other.completed, failed + other.failed, pending + other.pending);
        }

        // without prefix the keys are total, completed, ... and with prefix "exchanges" they are exchangesTotal, ...
        void putInto(ObjectNode json, String prefix) {
            json.put(key(prefix, "total"), total);
            json.put(key(prefix, "completed"), completed);
            json.put(key(prefix, "failed"), failed);
            json.put(key(prefix, "pending"), pending);
        }

        private static String key(String prefix, String name) {
            return prefix.isEmpty() ? name : prefix + StringUtils.capitalize(name);
        }
    }

    private void resetFlowStatistics(String flowId) {

        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);

        if(managedRouteGroup!=null){

            managedRouteGroup.reset();
            List<Route> routes = context.getRoutesByGroup(flowId);

            for (Route route : routes) {
                ManagedRouteMBean managedRoute = managedContext.getManagedRoute(route.getId());
                managedRoute.reset();
            }

        }
    }

    private void populateBasicStats(ObjectNode flow, FlowStatistics stats) {
        stats.exchanges.putInto(flow, "exchanges");
        stats.transactions.putInto(flow, "transactions");
    }

    private void populateDetailedStats(ObjectNode flow, FlowStatistics stats) {
        flow.put("status", stats.status);
        flow.put("timeout", getTimeout(context));
        flow.put("uptime", stats.uptime);
        flow.put("uptimeMillis", stats.uptimeMillis);
        flow.put("lastFailed", stats.lastFailed);
        flow.put("lastCompleted", stats.lastCompleted);
        flow.put("meanProcessingTime", stats.meanProcessingTime);
        flow.put("maxProcessingTime", stats.maxProcessingTime);
        flow.put("lastProcessingTime", stats.lastProcessingTime);
    }

    private void populateMetadata(ObjectNode flow, String flowId, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
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

    private ArrayNode collectStepStatistics(String flowId) {
        ArrayNode steps = mapper.createArrayNode();
        List<Route> routes = context.getRoutesByGroup(flowId);
        for (Route route : routes) {
            String routeId = route.getId();
            ObjectNode step = getStepStats(routeId, true);
            steps.add(step);
        }
        return steps;
    }

    // Helper class to store statistics
    private static final class FlowStatistics {
        Counts exchanges = Counts.NONE;
        Counts transactions = Counts.NONE;
        long uptimeMillis;
        long lastFailed;
        long lastCompleted;
        long meanProcessingTime;
        long maxProcessingTime;
        long lastProcessingTime;
        String uptime;
        String status;
    }

    private long getTimeout(CamelContext context) {
        try {
            String managementName = context.getManagementNameStrategy().getName();
            ObjectName objectName = context.getManagementStrategy().getManagementObjectNameStrategy().getObjectNameForCamelContext(managementName, context.getName());

            ManagedCamelContextMBean managedCamelContextMBean = JMX.newMBeanProxy(ManagementFactory.getPlatformMBeanServer(), objectName, ManagedCamelContextMBean.class);
            return managedCamelContextMBean.getTimeout();
        } catch (Exception _) {
            return 0L;
        }
    }

    public String getFlowStepStats(String flowId, String stepid, boolean fullStats) {

        String routeid = flowId + "-" + stepid;

        ObjectNode json = getStepStats(routeid, fullStats);
        return toPrettyString(json);

    }

    private ObjectNode getStepStats(String routeid, boolean fullStats) {

        ObjectNode json = mapper.createObjectNode();
        ObjectNode step = mapper.createObjectNode();

        ManagedRouteMBean route = managedContext.getManagedRoute(routeid);

        step.put("id", routeid);
        if(route == null) {
            step.put("status", "unconfigured");
        }else {

            String status = route.getState();
            step.put("status", status);

            if (status.equalsIgnoreCase("started")) {

                if (fullStats) {
                    String stepUptime = route.getUptime();
                    String stepUptimeMilliseconds = Long.toString(route.getUptimeMillis());

                    step.put("uptime", stepUptime);
                    step.put("uptimeMilliseconds", stepUptimeMilliseconds);

                }

                String statsAsJson = route.dumpStatsAsJSon(true);

                step.set("stats", mapper.readTree(statsAsJson));
            }
        }

        json.set("step", step);

        return json;

    }

    public String getFlowExchanges(String flowId, boolean includeSteps, String mediaType) {

        ObjectNode json = mapper.createObjectNode();
        ObjectNode flow = createBasicFlowJson(flowId);

        calculateFlowStatistics(flowId, false).exchanges.putInto(flow, "");

        if (includeSteps) {
            ArrayNode steps = flow.putArray("steps");

            for (ManagedRouteMBean route : managedContext.getManagedRoutesByGroup(flowId)) {
                ObjectNode step = steps.addObject();
                step.put("id", StringUtils.substringAfter(route.getRouteId(), flowId + "-"));
                Counts.of(route).putInto(step, "");
            }
        }

        json.set("flow", flow);

        return applyMediaType(toPrettyString(json), "json", mediaType);

    }

    public String getFlowTransactions(String flowId, String mediaType) {

        ObjectNode json = mapper.createObjectNode();
        ObjectNode flow = createBasicFlowJson(flowId);

        calculateFlowStatistics(flowId, false).transactions.putInto(flow, "");

        json.set("flow", flow);

        return applyMediaType(toPrettyString(json), "json", mediaType);

    }

    public String getFlowTotalExchanges(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).exchanges.total());
    }

    public String getFlowCompletedExchanges(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).exchanges.completed());
    }

    public String getFlowFailedExchanges(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).exchanges.failed());
    }

    public String getFlowPendingExchanges(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).exchanges.pending());
    }

    public String getFlowTotalTransactions(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).transactions.total());
    }

    public String getFlowCompletedTransactions(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).transactions.completed());
    }

    public String getFlowFailedTransactions(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).transactions.failed());
    }

    public String getFlowPendingTransactions(String flowId) {
        return Long.toString(calculateFlowStatistics(flowId, false).transactions.pending());
    }

    public String getStepExchanges(String flowId, String stepId, String mediaType) {

        ManagedRouteMBean route = managedContext.getManagedRoute(flowId + "-" + stepId);

        ObjectNode json = mapper.createObjectNode();
        ObjectNode step = json.putObject("step");

        step.put("id", stepId);
        (route != null ? Counts.of(route) : Counts.NONE).putInto(step, "");

        return applyMediaType(toPrettyString(json), "json", mediaType);

    }

    public String getHealth(String type, String mediaType) {

        Set<String> flowIds = flowManager.getListOfFlowIds(null);

        String result = getHealthFromList(flowIds, type);

        return applyMediaType(result, "json", mediaType);
    }

    public String getHealthByFlowIds(String flowIds, String type, String mediaType) {

        String[] values = flowIds.split(",");

        Set<String> flowSet = new HashSet<>(Arrays.asList(values));

        String result = getHealthFromList(flowSet, type);

        return applyMediaType(result, "json", mediaType);

    }


    private String getHealthFromList(Set<String> flowIds, String type) {

        JSONArray flows = new JSONArray();

        for (String flowId : flowIds) {
            JSONObject flow = getFlowHealthObject(flowId, type, false, false, false);

            flows.put(flow);
        }

        return flows.toString();

    }

    public String getFlowHealth(String flowId, String type, boolean includeSteps, boolean includeError, boolean includeDetails, String mediaType) {

        JSONObject json = getFlowHealthObject(flowId, type, includeSteps, includeError, includeDetails);

        String flowStats = json.toString(4);

        return applyMediaType(flowStats, "json", mediaType);

    }

    private JSONObject getFlowHealthObject(String flowId, String type, boolean includeSteps, boolean includeError, boolean includeDetails){
        JSONObject json = new JSONObject();
        JSONObject flow = new JSONObject();
        JSONArray steps = new JSONArray();

        String state = "UNKNOWN";

        List<Route> routes = context.getRoutesByGroup(flowId);

        for (Route r : routes) {

            String routeId = r.getId();
            String healthCheckId = type + ":" + routeId;
            JSONObject step = getStepHealth(routeId, healthCheckId, includeError, includeDetails);

            String stepState = step.getJSONObject("step").getString("status");
            if (!state.equalsIgnoreCase("DOWN")) {
                state = stepState;
            }
            steps.put(step);

        }

        flow.put("id", flowId);
        flow.put("status", state);

        if (includeSteps) {
            flow.put("steps", steps);
        }
        json.put("flow", flow);

        return json;
    }

    public String getFlowStepHealth(String flowId, String stepId, String type, boolean includeError, boolean includeDetails, String mediaType) {

        String routeid = flowId + "-" + stepId;
        String healthCheckId = type + ":" + routeid;

        JSONObject json = getStepHealth(routeid, healthCheckId, includeError, includeDetails);

        String result = json.toString(4);

        return applyMediaType(result, "json", mediaType);
    }

    private JSONObject getStepHealth(String routeid, String healthCheckId, boolean includeError, boolean includeDetails) {

        JSONObject json = new JSONObject();
        JSONObject step = new JSONObject();

        step.put("id", routeid);

        HealthCheck healthCheck = HealthCheckHelper.getHealthCheck(context, healthCheckId);

        if (healthCheck != null && healthCheck.isReadiness()) {

            HealthCheck.Result result = healthCheck.callReadiness();
            step.put("status", result.getState().toString());

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
            step.put("status", "UNKNOWN");
        }


        json.put("step", step);

        return json;
    }

    public String getStats(String mediaType) {

        ObjectNode json = mapper.createObjectNode();

        ManagedCamelContextMBean managedCamelContext = managedContext.getManagedCamelContext();

        json.put("camelId", managedCamelContext.getCamelId());
        json.put("camelVersion", managedCamelContext.getCamelVersion());
        json.put("status", managedCamelContext.getState());
        json.put("uptime", managedCamelContext.getUptime());
        json.put("uptimeMillis", managedCamelContext.getUptimeMillis());
        json.put("startedFlows", countFlows("started"));
        json.put("startedSteps", managedCamelContext.getStartedRoutes());
        // Transactions count each message once (context), exchanges count it once per step (routes)
        sumOfRoutes(context.getRoutes()).putInto(json, "exchanges");
        Counts.of(managedCamelContext).putInto(json, "transactions");
        json.put("cpuLoadLastMinute", managedCamelContext.getLoad01());
        json.put("cpuLoadLast5Minutes", managedCamelContext.getLoad05());
        json.put("cpuLoadLast15Minutes", managedCamelContext.getLoad15());
        json.put("memoryUsage", getMemoryUsage());
        json.put("totalThreads", ManagementFactory.getThreadMXBean().getThreadCount());

        String stats = toPrettyString(json);

        return applyMediaType(stats, "json", mediaType);

    }

    public String getThreads(String mediaType, String filter, int topEntries) {

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
                jsonObjectList = jsonObjectList.subList(0, jsonObjectList.size());
            }else{
                jsonObjectList = jsonObjectList.subList(0, topEntries);
            }
        }

        // Sort by cpuTime
        List<JSONObject> sortedList = jsonObjectList.stream()
                .sorted(Comparator.comparingInt((JSONObject o) -> o.getInt("cpuTime")).reversed())
                .toList();

        // Rebuild the JSONArray from the sorted and filtered list
        JSONArray jsonArray = new JSONArray(sortedList);
        String result = jsonArray.toString();

        return applyMediaType(result, "json", mediaType);

    }

    private double getMemoryUsage() {

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();

        long maxMemory = heapMemoryUsage.getMax(); // Maximum available memory
        long usedMemory = heapMemoryUsage.getUsed(); // Currently used memory

        double memoryUsagePercentage = ((double) usedMemory / maxMemory) * 100;

        // round to two decimals without DecimalFormat, which uses a locale-dependent decimal separator
        return Math.round(memoryUsagePercentage * 100.0) / 100.0;

    }

    public String getStepsStats(String mediaType) throws Exception {

        ManagedCamelContextMBean managedCamelContext = managedContext.getManagedCamelContext();

        String result = managedCamelContext.dumpRoutesStatsAsXml(true, false);

        return applyMediaType(result, "xml", mediaType);

    }

    public String getFlowsStats(boolean fullStats, String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        Set<String> flowIds = context.getRouteGroupIds();

        String result = getStatsFromList(flowIds, fullStats, flowsMap);

        return applyMediaType(result, "json", mediaType);

    }

    public String getStatsByFlowIds(String flowIds) {

        String[] values = flowIds.split(",");

        Set<String> flowSet = new HashSet<>(Arrays.asList(values));

        ArrayNode flows = mapper.createArrayNode();

        for (String flowId : flowSet) {
            ObjectNode flow = createBasicFlowJson(flowId);

            FlowStatistics stats = calculateFlowStatistics(flowId, true);

            // Populate basic stats
            populateBasicStats(flow, stats);
            populateDetailedStats(flow, stats);

            // Build final response
            flows.addObject().set("flow", flow);
        }

        return flows.toString();

    }

    private String getStatsFromList(Set<String> flowIds, boolean fullStats, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        ArrayNode flows = mapper.createArrayNode();

        for (String flowId : flowIds) {
            flows.add(buildFlowStats(flowId, fullStats, false, false, flowsMap));
        }

        return flows.toString();

    }

    public String getMetrics(String mediaType) {

        String integrationStats = "0";
        MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);

        if (metricsService != null) {
            integrationStats = metricsService.dumpStatisticsAsJson();
            if (mediaType.contains("xml")) {
                integrationStats = DocConverter.jsonToXml(integrationStats);
            }
        }

        return integrationStats;

    }


    public String getHistoryMetrics(String mediaType) {

        String integrationStats = "0";

        MetricsMessageHistoryService historyService = context.hasService(MetricsMessageHistoryService.class);

        if (historyService != null) {
            integrationStats = historyService.dumpStatisticsAsJson();
            if (mediaType.contains("xml")) {
                integrationStats = DocConverter.jsonToXml(integrationStats);
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

        String result = json.toString(4);

        return applyMediaType(result, "json", mediaType);

    }

    public int countFlows(String filter) {

        Set<String> flowIds = flowManager.getListOfFlowIds(filter);

        return flowIds.size();

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

    private static String toPrettyString(ObjectNode json) {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    private String applyMediaType(String content, String origMediaType, String destMediaType) {

        if (origMediaType == null || destMediaType == null) {
            return content;
        }

        String from = normalizeMediaType(origMediaType);
        String to = normalizeMediaType(destMediaType);

        if (from.isEmpty() || to.isEmpty() || from.equalsIgnoreCase(to)) {
            return content;
        }

        try {
            return convertMediaType(content, from, to);
        } catch (Exception e) {
            log.warn("Failed to convert from {} to {}, returning original content", from, to, e);
            return content;
        }
    }

    private String normalizeMediaType(String mediaType) {
        if (mediaType == null) return "";

        mediaType = mediaType.toLowerCase().trim();

        // strip optional parameters like ";charset=utf-8"
        int semicolon = mediaType.indexOf(';');
        if (semicolon != -1) {
            mediaType = mediaType.substring(0, semicolon);
        }

        // strip "application/" or "text/"
        if (mediaType.startsWith("application/")) {
            return mediaType.substring("application/".length());
        }
        if (mediaType.startsWith("text/")) {
            return mediaType.substring("text/".length());
        }

        return mediaType;
    }

    private String convertMediaType(String content, String from, String to) {
        return switch (from + "->" + to) {
            case "json->xml" -> DocConverter.jsonToXml(content);
            case "xml->json" -> DocConverter.xmlToJson(content);
            default -> content;
        };
    }

}
