package org.assimbly.integration.impl.manager;

import org.apache.camel.*;
import java.util.*;

import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteGroupMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.api.management.mbean.RouteError;
import org.apache.camel.component.mail.MailAuthenticator;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.beans.OAuth2MailAuthenticator;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.blocks.processors.AS2KeyProcessor;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.dil.loader.RouteLoader;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.docconverter.StringConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.EncryptionUtil;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FlowManager {

    protected static final Logger log = LoggerFactory.getLogger(FlowManager.class);

    private static final String ASSIMBLY_ENCRYPTION_SECRET = System.getenv("ASSIMBLY_ENCRYPTION_SECRET");
    private final EncryptionUtil encryptionUtil = new EncryptionUtil(ASSIMBLY_ENCRYPTION_SECRET);

    private ServiceStatus status;

    private final CamelContext context;
    private final ManagedCamelContext managedContext;
    private final InstalledFlowsManager installedFlowsManager;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private static final long STOP_TIMEOUT = 300;

    public static final String PROPERTY_FLOW_ENVIRONMENT = "flow.environment";
    public static final String PROPERTY_FLOW_NAME = "flow.name";
    public static final String PROPERTY_FLOW_TENANT = "flow.tenant";
    public static final String PROPERTY_FLOW_VERSION = "flow.version";
    public static final String PROPERTY_ID = "id";

    public FlowManager(CamelContext context) {
        this(context, null);
    }

    public FlowManager(CamelContext context, InstalledFlowsManager installedFlowsManager) {
        this.context = context;
        this.managedContext = context.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
        this.installedFlowsManager = installedFlowsManager;
    }

    public FlowLoaderReport loadFlow(String flowId, TreeMap<String, String> properties) {
        return loadFlow(flowId, properties, true);
    }

    /**
     * Loads a flow into the Camel context.
     *
     * @param autoStart when false, routes are added without becoming active (used when restoring a paused
     *                  flow after restart). Cache restore runs <em>before</em> {@code CamelContext.start()},
     *                  so route definitions must be marked {@code autoStartup=false} or they start with the
     *                  context. Templated routes have no XML autoStartup attribute.
     */
    public FlowLoaderReport loadFlow(String flowId, TreeMap<String, String> properties, boolean autoStart) {

        String version = setProperty(properties,PROPERTY_FLOW_VERSION,"0");

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId, version);

        try {

            //initialize security
            initializeSecurity(properties);

            //create connections & install dependencies if needed
            createConnections(properties);

            // When the context is already started, disable context autoStartup so newly added
            // templated routes do not activate consumers during addRoutesToCamelContext.
            Boolean previousContextAutoStartup = null;
            if (!autoStart && context.getStatus().isStarted()) {
                previousContextAutoStartup = context.isAutoStartup();
                context.setAutoStartup(false);
            }

            try {
                FlowLoader flow = new FlowLoader(properties, report, encryptionUtil, autoStart);

                flow.addRoutesToCamelContext(context);

                if(flow.isFlowLoaded()){
                    if (!autoStart) {
                        // Critical: setCaching runs before CamelContext.start(). Mark definitions so
                        // context.start() does not activate paused flows (templated routes default to true).
                        markFlowRoutesAutoStartupFalse(flowId);
                        if (context.getStatus().isStarted()) {
                            ensureFlowRoutesStopped(flowId);
                        }
                    }
                    String event = autoStart ? "start" : "pause";
                    String message = autoStart ? "Started flow successfully" : "Loaded paused flow successfully";
                    return finishReport(report, flowId, event, message, "info","success");
                }else{
                    stopFlow(flowId, STOP_TIMEOUT);
                    return finishReport(report, flowId, "start", "Start flow failed", "error","failed");
                }
            } finally {
                if (previousContextAutoStartup != null) {
                    context.setAutoStartup(previousContextAutoStartup);
                }
            }

        } catch (Exception e) {
            log.error("Load flow failed: ", e);
            return finishReport(report, flowId, "start", e.getMessage(), "error","failed");
        }

    }

    /**
     * Marks route definitions for a flow as {@code autoStartup=false} so a later
     * {@link CamelContext#start()} leaves them stopped.
     * <p>
     * Must use {@link ModelCamelContext#getRouteDefinitions()} — after loading templated
     * routes but before the context is started, {@link CamelContext#getRoutes()} /
     * {@code getRoutesByGroup} are often still empty, so iterating live routes is a no-op.
     */
    private void markFlowRoutesAutoStartupFalse(String flowId) {
        ModelCamelContext modelContext = (ModelCamelContext) context;
        List<RouteDefinition> definitions = modelContext.getRouteDefinitions().stream()
                .filter(definition -> belongsToFlow(definition, flowId))
                .toList();

        if (definitions.isEmpty()) {
            log.warn("No route definitions found to mark autoStartup=false | flowid={}", flowId);
            return;
        }

        for (RouteDefinition definition : definitions) {
            try {
                definition.setAutoStartup("false");
                log.info("Marked restored paused route autoStartup=false | flowid={} | routeid={}",
                        flowId, definition.getId());
            } catch (Exception e) {
                log.warn("Failed to mark restored paused route autoStartup=false | flowid={} | routeid={}",
                        flowId, definition.getId(), e);
            }
        }
    }

    private static boolean belongsToFlow(RouteDefinition definition, String flowId) {
        if (definition == null || flowId == null) {
            return false;
        }
        if (flowId.equals(definition.getGroup())) {
            return true;
        }
        String routeId = definition.getId();
        return routeId != null && (routeId.equals(flowId) || routeId.startsWith(flowId + "-"));
    }

    /**
     * After {@link CamelContext#start()}, stop any flow whose desired state is paused.
     * Covers cases where route definitions were not marked in time (or consumers still started).
     */
    public void enforceDesiredPausedFlows() {
        if (installedFlowsManager == null) {
            return;
        }
        installedFlowsManager.getAll().forEach((flowId, entry) -> {
            if (entry != null && entry.isPaused()) {
                log.info("Enforcing paused state after context start | flowid={}", flowId);
                ensureFlowRoutesStopped(flowId);
            }
        });
    }

    /**
     * Safety net when the Camel context is already started: stop any route that still became active.
     */
    private void ensureFlowRoutesStopped(String flowId) {
        RouteController routeController = context.getRouteController();
        List<Route> routes = getRoutesByFlowId(flowId);
        if (routes.isEmpty()) {
            log.warn("No live routes to stop for paused flow | flowid={}", flowId);
            return;
        }
        for (Route route : routes) {
            String routeId = route.getId();
            try {
                ServiceStatus routeStatus = routeController.getRouteStatus(routeId);
                if (routeStatus != null && !routeStatus.isStopped()) {
                    routeController.stopRoute(routeId, STOP_TIMEOUT, TimeUnit.MILLISECONDS);
                    log.info("Stopped restored paused route | flowid={} | routeid={}", flowId, routeId);
                }
            } catch (Exception e) {
                log.warn("Failed to stop restored paused route | flowid={} | routeid={}", flowId, routeId, e);
            }
        }
    }

    private void initializeSecurity(TreeMap<String, String> properties) throws Exception {
        if (properties.containsKey("security.as2")) {
            log.info("Initialize AS2 Inbound security");
            initializeAs2InboundSecurity(properties);
        }
        if (properties.containsKey("security.email")) {
            log.info("Initialize oauth2 email authenticator");
            initializeOAuth2EmailAuthenticator(properties);
        }
        if (properties.containsKey("security.mutualtls")) {
            log.info("Initialize Mutual TLS");
            initializeMutualTLS(properties);
        }
    }

    public boolean hasFlow(String flowId) {

        List<Route> routes = context.getRoutesByGroup(flowId);

        return !routes.isEmpty();

    }

    /**
     * Restores flows from cache. Started entries are loaded and activated; paused entries are loaded
     * with {@code autoStartup=false} (never start-then-pause). Cache entries missing from the index
     * are treated as legacy paused flows and re-registered as paused when {@code installedFlowsManager} is set.
     */
    public void startAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap,
                              Map<String, InstalledFlowsManager.FlowEntry> installedFlowsIndexMap,
                              InstalledFlowsManager installedFlowsManager) {

        log.info("Starting all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                InstalledFlowsManager.FlowEntry entry = installedFlowsIndexMap.get(flowId);
                if (entry != null) {
                    boolean autoStart = !entry.isPaused();
                    loadFlow(flowId, flowProps, autoStart);
                    log.info(autoStart ? "Started flow: {}" : "Restored paused flow: {}", flowId);
                } else if (installedFlowsManager != null) {
                    // Legacy encoding: paused flows were unregistered from the index but left in DIL cache
                    loadFlow(flowId, flowProps, false);
                    String version = flowProps.getOrDefault(PROPERTY_FLOW_VERSION, "0");
                    String tenant = flowProps.getOrDefault(PROPERTY_FLOW_TENANT, "0");
                    installedFlowsManager.register(flowId, version, tenant, InstalledFlowsManager.FlowEntry.STATUS_PAUSED);
                    log.info("Restored legacy paused flow: {}", flowId);
                } else {
                    log.info("Skipping flow not in installed index: {}", flowId);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public String restartAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        log.info("Restarting all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                loadFlow(flowId, flowProps);
                log.info("Restarted flow: {}", flowId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return "restarted";
    }

    public String stopAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Stopping all flows");

        flowsMap.forEach((flowId, _) -> {
            try {
                stopFlow(flowId, 250, false);
                log.info("Stopped flow: {}", flowId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return FlowStatus.STOPPED.toString();
    }

    public String installRoute(String routeId, String route) {

        FlowLoaderReport report = new FlowLoaderReport(routeId, routeId, "0");

        try {
            String routeXml = route.startsWith("<route")
                    ? route
                    : new XMLFileConfiguration().getRouteConfiguration(route);

            RouteLoader routeLoader = new RouteLoader(routeId, routeXml, report);

            routeLoader.addRoutesToCamelContext(context);

            String result = routeLoader.getReport();

            return finishReport(report, routeId, "start", result, "info", "success").getReport();

        } catch (Exception e) {
            return finishReport(report, routeId, "start", "Route install failed | error=" + e.getMessage(), "error", "failed").getReport();
        }
    }

    public FlowLoaderReport startFlow(String flowId, TreeMap<String, String> flowProperties, long timeout) {

        if (hasFlow(flowId)) {
            stopFlow(flowId, timeout, false);
        }

        return loadFlow(flowId, flowProperties);

    }

    public FlowLoaderReport restartFlow(String flowId, TreeMap<String, String> flowProperties, long timeout) {
        return startFlow(flowId, flowProperties, timeout);
    }

    public String stopFlow(String flowid, long timeout) {
        return stopFlow(flowid, timeout, true);
    }

    public String stopFlow(String flowId, long timeout, boolean enableReport) {

        if (!hasFlow(flowId)) {
            FlowLoaderReport report = new FlowLoaderReport(flowId, flowId,"0");
            String errorMessage = "Flow is not installed";
            return  finishReport(report, flowId, "stop", errorMessage, "error","failed").getReport();
        }

        try {
            // gracefully shutdown routes using startup order
            List<RouteStartupOrder> routeStartupOrders = getRoutesStartupOrderByFlowId(flowId);
            context.getShutdownStrategy().shutdown(context, routeStartupOrders, timeout, TimeUnit.MILLISECONDS);
            for (RouteStartupOrder routeStartupOrder : routeStartupOrders) {
                context.removeRoute(routeStartupOrder.getRoute().getId());
            }

            // remove leftover routes
            List<String> leftoverRoutes = getAllRoutesByFlowId(flowId);
            if (!leftoverRoutes.isEmpty()) {
                for (String routeId : leftoverRoutes) {
                    removeRoute(routeId);
                }
            }

            if (enableReport) {
                FlowLoaderReport report = new FlowLoaderReport(flowId, flowId, "0");
                return finishReport(report, flowId, "stop", "Stopped flow successfully", "info" ,"success").getReport();
            }

        } catch (Exception e) {

            log.error("Stop flow failed. | flowid={}", flowId, e);

            if (enableReport) {
                FlowLoaderReport report = new FlowLoaderReport(flowId, flowId,"0");
                return finishReport(report, flowId, "stop", "Stop flow failed | error=" + e.getMessage(), "error","failed").getReport();
            }

        }

        return FlowStatus.STOPPED.toString();

    }

    private void removeRoute(String routeId) {
        try {
            if (context.getRoute(routeId) != null) {
                context.getRouteController().stopRoute(routeId, STOP_TIMEOUT, TimeUnit.MILLISECONDS);
                context.removeRoute(routeId);
            }
        } catch (Exception e) {
            log.error("Error removing route: {} Error message: {}", routeId, e.getMessage());
        }
    }

    public FlowLoaderReport pauseFlow(String flowId) {

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId,"0");

        if (!hasFlow(flowId)) {
            String errorMessage = "Flow is not installed";
            return  finishReport(report, flowId, "pause", errorMessage, "error","failed");
        }

        RouteController routeController = context.getRouteController();
        List<Route> routeList = getRoutesByFlowId(flowId);
        status = routeController.getRouteStatus(routeList.getFirst().getId());

        for (Route route : routeList) {
            if (!routeController.getRouteStatus(route.getId()).isSuspendable()) {
                return finishReport(report, flowId, "pause", "Flow isn't suspendable (Step " + route.getId() + ")", "error","failed");
            }
        }

        try {

            for (Route route : routeList) {
                String routeId = route.getId();
                routeController.suspendRoute(routeId);
            }

            return finishReport(report, flowId, "pause", "Paused flow successfully", "info","success");

        } catch (Exception e) {
            log.error("Pause flow failed. | flowid={}", flowId, e);
            stopFlow(flowId, STOP_TIMEOUT); //Stop flow if one of the routes cannot be paused.
            return finishReport(report, flowId, "pause", e.getMessage(), "error","failed");
        }

    }

    public FlowLoaderReport resumeFlow(String flowId, TreeMap<String, String> flowProperties) {

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId, "0");

        RouteController routeController = context.getRouteController();

        if (!hasFlow(flowId)) {
            String errorMessage = "Flow is not installed";
            return  finishReport(report, flowId, "resume", errorMessage, "error","failed");
        }

        try {

            List<Route> routeList = getRoutesByFlowId(flowId);
            boolean hasStopped = false;
            boolean hasSuspended = false;
            for (Route route : routeList) {
                ServiceStatus routeStatus = routeController.getRouteStatus(route.getId());
                if (routeStatus.isStopped()) {
                    hasStopped = true;
                }
                if (routeStatus.isSuspended()) {
                    hasSuspended = true;
                }
            }

            // Restored paused flows (autoStartup=false) are Stopped — start the whole flow once
            if (hasStopped && !hasSuspended) {
                FlowLoaderReport startReport = startFlow(flowId, flowProperties, STOP_TIMEOUT);
                if (startReport.isStatusSuccess()) {
                    return finishReport(report, flowId, "resume", "Resumed flow successfully", "info","success");
                }
                return startReport;
            }

            for (Route route : routeList) {
                String routeId = route.getId();
                status = routeController.getRouteStatus(routeId);

                if (status.isSuspended()) {
                    routeController.resumeRoute(routeId);
                    log.info("Resumed flow  | flowid={} | stepid={}", flowId, routeId);
                } else if (status.isStopped()) {
                    log.info("Starting route as route {} is currently stopped (not suspended)", flowId);
                    startFlow(flowId, flowProperties, STOP_TIMEOUT);
                    break;
                }

            }

            return finishReport(report, flowId, "resume", "Resumed flow successfully", "info","success");

        } catch (Exception e) {
            log.error("Resume flow {} failed.", flowId, e);
            return finishReport(report, flowId, "resume", e.getMessage(), "error", "failed");
        }

    }

    public String testFlow(String flowId) {

        JSONObject json = new JSONObject();
        JSONObject test = new JSONObject();

        try(FluentProducerTemplate template = context.createFluentProducerTemplate()) {

            Message message = template
                    .to("sync:" + flowId)
                    .request(Message.class);

            JSONObject headers = new JSONObject();
            Map<String, Object> headersMap = message.getHeaders();
            for (Map.Entry<String, Object> header : headersMap.entrySet()) {
                headers.put(header.getKey(),header.getValue());
            }

            test.put("body",message.getBody(String.class));
            test.put("headers",headers);
            test.put("passed ",true);


        } catch (Exception e) {
            test.put("passed ",false);
            test.put("message ",e.getMessage());
            log.error("Test flow failed. | flowid={}", flowId, e);
        }

        // Build final response
        json.put("test", test);

        return json.toString(2);

    }

    public FlowLoaderReport finishReport(FlowLoaderReport report, String flowid, String event, String message, String messageType, String status) {

        String eventCapitalized = StringUtils.capitalize(event);

        if (messageType.equalsIgnoreCase("error")) {
            log.error("{} flow failed | flowid={} message={}", eventCapitalized, flowid, message);
        } else if (messageType.equalsIgnoreCase("warning")) {
            log.warn("{} flow failed | flowid={} message={}", eventCapitalized, flowid, message);
        } else {
            log.info("{} | flowid={}", message, flowid);
        }

        report.finishReport(event, message, status);

        return report;

    }

    public boolean isFlowStarted(String flowid) {

        RouteController routeController = context.getRouteController();

        if (hasFlow(flowid)) {
            ServiceStatus serviceStatus = null;
            List<Route> routes = getRoutesByFlowId(flowid);

            for (Route route : routes) {
                serviceStatus = routeController.getRouteStatus(route.getId());
                if (!serviceStatus.isStarted()) {
                    return false;
                }
            }
            return serviceStatus != null;
        } else {
            return false;
        }

    }

    public String getFlowStatus(String id) {

        RouteController routeController = context.getRouteController();

        String flowStatus;
        if (hasFlow(id)) {
            try {
                List<Route> routesList = getRoutesByFlowId(id);
                if (routesList.isEmpty()) {
                    flowStatus = FlowStatus.UNCONFIGURED.toString();
                } else {
                    String flowId = routesList.getFirst().getId();
                    ServiceStatus serviceStatus = routeController.getRouteStatus(flowId);
                    flowStatus = serviceStatus.toString().toLowerCase();
                    // Restored paused flows are Camel Stopped (autoStartup=false); report as suspended for UI
                    if ("stopped".equals(flowStatus) && isDesiredPaused(id)) {
                        flowStatus = "suspended";
                    }
                }
            } catch (Exception e) {
                log.error("Get status flow {} failed.", id, e);

                flowStatus = FlowStatus.ERROR.toString();
            }

        } else {
            flowStatus = FlowStatus.UNCONFIGURED.toString();
        }

        return flowStatus;

    }

    private boolean isDesiredPaused(String flowId) {
        if (installedFlowsManager == null) {
            return false;
        }
        InstalledFlowsManager.FlowEntry entry = installedFlowsManager.get(flowId);
        return entry != null && entry.isPaused();
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

    public String getFlowLastError(String id) {

        List<Route> routeList = getRoutesByFlowId(id);
        StringBuilder sb = new StringBuilder();

        for (Route r : routeList) {

            String routeId = r.getId();
            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);

            if (route != null) {
                RouteError lastError = route.getLastError();
                if (lastError != null) {
                    sb.append("RouteID: ")
                    .append(routeId)
                    .append("Error: ")
                    .append(lastError)
                    .append(';');
                }
            }
        }
        String flowInfo;
        if (sb.isEmpty()) {
            flowInfo = "0";
        } else {
            flowInfo = sb.toString();
        }

        return flowInfo;
    }

    public String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception {

        Date date = new Date();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);
        File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

        if (file.exists()) {
            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            if (numberOfEntries != null && numberOfEntries < lines.size()) {
                lines = lines.subList(lines.size() - numberOfEntries, lines.size());
            }
            return StringUtils.join(lines, ',');
        } else {
            return "0";
        }
    }

    public TreeMap<String, String> getIntegrationAlertsCount(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        TreeMap<String, String> numberOfEntriesList = new TreeMap<>();

        flowsMap.forEach((flowId, _) -> {
            try {
                long numberOfEntries = getFlowAlertsCount(flowId);
                numberOfEntriesList.put(flowId, Long.toString(numberOfEntries));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return numberOfEntriesList;

    }

    public long getFlowAlertsCount(String flowId) {

        ManagedRouteGroupMBean managedRouteGroup = managedContext.getManagedRouteGroup(flowId);

        if (managedRouteGroup == null){
            return 0;
        }

        return managedRouteGroup.getExchangesFailed() + managedRouteGroup.getFailuresHandled();

    }

    public String getFlowEventsLog(String id, Integer numberOfEntries) throws Exception {

        Date date = new Date();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);
        File file = new File(baseDir + "/events/" + id + "/" + today + "_events.log");

        if (file.exists()) {
            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            if (numberOfEntries != null && numberOfEntries < lines.size()) {
                lines = lines.subList(lines.size() - numberOfEntries, lines.size());
            }
            return StringUtils.join(lines, ',');
        } else {
            return "0";
        }
    }

    public Set<String> getListOfFlowIds(String filter) {
        return context.getRoutes().stream()
                .map(route -> {
                    String flowId = route.getGroup();
                    if (flowId == null || flowId.isEmpty()) {
                        return StringUtils.substringBefore(route.getId(), "-");
                    }
                    return flowId;
                })
                .filter(flowId -> flowId != null && !flowId.isEmpty())
                .distinct()
                .filter(flowId -> filter == null || filter.isEmpty() || getFlowStatus(flowId).equalsIgnoreCase(filter))
                .collect(Collectors.toSet());
    }

    public String getListOfFlows(String filter, String mediaType) {

        Set<String> flowIds = getListOfFlowIds(filter);

        JSONArray flowsArray = new JSONArray();

        for (String flowId : flowIds) {
            JSONObject flowObject = new JSONObject();
            flowObject.put("id", flowId);
            flowsArray.put(flowObject);
        }

        String result = flowsArray.toString();

        if (mediaType.contains("xml")) {
            JSONObject flowsObject = new JSONObject();
            JSONObject flowObject = new JSONObject();
            flowObject.put("flow", flowsArray);
            flowsObject.put("flows", flowObject);
            result = DocConverter.jsonToXml(flowsObject.toString());
        }

        return result;

    }

    public String getFlowInfo(String flowId, String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        TreeMap<String, String> flowProperties = flowsMap.get(flowId);

        JSONObject json = new JSONObject();
        JSONObject flow = new JSONObject();

        if (flowProperties != null) {
            flow.put("id", flowProperties.get(PROPERTY_ID));
            flow.put("name", flowProperties.get(PROPERTY_FLOW_NAME));
            flow.put("isRunning", isFlowStarted(flowId));
            flow.put("status", getFlowStatus(flowId));
            flow.put("version", setProperty(flowProperties,PROPERTY_FLOW_VERSION,"0"));
            flow.put("environment", setProperty(flowProperties,PROPERTY_FLOW_ENVIRONMENT,null));
            flow.put("tenant", setProperty(flowProperties,PROPERTY_FLOW_TENANT,null));
            flow.put("uptime", getFlowUptime(flowId));
        } else {
            flow.put("id", flowId);
            flow.put("isRunning", false);
            flow.put("status", getFlowStatus(flowId));
        }

        json.put("flow", flow);

        String integrationInfo = json.toString(4);
        if (mediaType.contains("xml")) {
            integrationInfo = DocConverter.jsonToXml(integrationInfo);
        }

        return integrationInfo;

    }

    public String getListOfFlowsDetails(String filter, String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        Set<String> flowIds = getListOfFlowIds(filter);

        JSONArray flowsArray = new JSONArray();

        for (String flowId : flowIds) {
            JSONObject flowObject = new JSONObject(getFlowInfo(flowId, "application/json", flowsMap));
            flowsArray.put(flowObject);
        }

        String result = flowsArray.toString();

        if (mediaType.contains("xml")) {
            JSONObject flowsObject = new JSONObject();
            JSONObject flowObject = new JSONObject();
            flowObject.put("flow", flowsArray);
            flowsObject.put("flows", flowObject);
            result = DocConverter.jsonToXml(flowsObject.toString());
        }

        return result;

    }

    public String getErrors(int maxNumberOfEntries, String mediaType) {

        ErrorRegistry errorRegistry = context.getErrorRegistry();

        Collection<BacklogErrorEventMessage> errorEventMessages = errorRegistry.browse(maxNumberOfEntries);

        String result = errorEventMessageToJson(errorEventMessages);

        if (mediaType.contains("xml")) {
            result = DocConverter.jsonToXml(result);
        }

        return result;

    }

    public String getFlowErrors(String flowId, int maxNumberOfEntries, String mediaType) {

        ErrorRegistry errorRegistry = context.getErrorRegistry();

        Collection<BacklogErrorEventMessage> errorEventMessages = errorRegistry.browse();

        String result = errorEventMessageToJson(
                errorEventMessages.stream()
                        .filter(msg -> flowId.equals(msg.getRouteGroup()))
                        .limit(maxNumberOfEntries)
                        .toList()
        );

        if (mediaType.contains("xml")) {
            result = DocConverter.jsonToXml(result);
        }

        return result;

    }

    public String getStepErrors(String flowId, String stepId, int maxNumberOfEntries, String mediaType) {

        ErrorRegistry errorRegistry = context.getErrorRegistry();
        ErrorRegistryView errorRegistryView = errorRegistry.forRoute(flowId + "-" + stepId);

        Collection<BacklogErrorEventMessage> errorEventMessages = errorRegistryView.browse(maxNumberOfEntries);

        String result = errorEventMessageToJson(errorEventMessages);

        if (mediaType.contains("xml")) {
            result = DocConverter.jsonToXml(result);
        }

        return result;

    }

    public String getErrorByUid(String flowId, String stepId, long uid, String mediaType) {

        ErrorRegistry errorRegistry = context.getErrorRegistry();

        ErrorRegistryView errorRegistryView = errorRegistry.forRoute(flowId + "-" + stepId);
        
        Collection<BacklogErrorEventMessage> errorEventMessages = errorRegistryView.browse();

        Optional<BacklogErrorEventMessage> matchingMessage = errorEventMessages.stream()
                .filter(msg -> uid == msg.getUid())
                .findFirst();

        String result = matchingMessage
                .map(msg -> msg.toJSon(4))
                .orElse("{}");

        if (mediaType.contains("xml")) {
            result = DocConverter.jsonToXml(result);
        }

        return result;

    }

    private String errorEventMessageToJson(Collection<BacklogErrorEventMessage> errorEventMessages) {

        JsonMapper mapper = JsonMapper.builder().build();
        ArrayNode arrayNode = mapper.createArrayNode();

        for (BacklogErrorEventMessage errorEventMessage : errorEventMessages) {

            ObjectNode node = mapper.createObjectNode();
            node.put("uid", errorEventMessage.getUid());
            node.put("flowId", errorEventMessage.getRouteGroup());
            node.put("stepId", StringUtils.substringAfter(errorEventMessage.getRouteId(),errorEventMessage.getRouteGroup() + "-"));
            node.put("tenant", context.getVariable("group:" + errorEventMessage.getRouteGroup() + ":MetaData.TenantName",String.class));
            node.put("timestamp", errorEventMessage.getTimestamp());
            node.put("exceptionMessage", errorEventMessage.getExceptionMessage());
            node.put("exceptionType", errorEventMessage.getExceptionType());

            arrayNode.add(node);
        }

        return mapper.writeValueAsString(arrayNode);
    }


    public String setFlowId(String filename, String configuration) throws Exception {

        String configurationUTF8 = new String(configuration.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        String flowId;
        if (IntegrationUtil.isXML(configurationUTF8)) {
            flowId = getFlowId(filename, configurationUTF8);
        } else {
            flowId = filename;
        }

        return flowId;

    }

    public String getFlowId(String filename, String configurationUTF8) throws Exception {

        String flowId = "";
        Document doc = StringConverter.stringToDoc(configurationUTF8);
        XPath xPath = XPathFactory.newInstance().newXPath();

        String root = doc.getDocumentElement().getTagName();

        switch (root) {
            case "dil", "integrations", "flows", "flow" -> {
                flowId = xPath.evaluate("//flows/flow[id='" + filename + "']/id", doc);
                if (flowId == null || flowId.isEmpty()) {
                    flowId = xPath.evaluate("//flow[1]/id", doc);
                }
            }
            case "camelContext" -> flowId = xPath.evaluate("/camelContext/@id", doc);
            case "routes" -> flowId = xPath.evaluate("/routes/@id", doc);
            default ->
                    log.error("Unknown configuration. Either a DIL file (starting with a <dil> element) or Camel file (starting with <routes> element) is expected");
        }

        return flowId;

    }

    public boolean removeFlow(String flowId) throws Exception {

        List<Route> routes = getRoutesByFlowId(flowId);

        if (routes == null || routes.isEmpty()) {
            return false;
        }

        for (Route route : routes) {
            context.removeRoute(route.getId()); // ← use actual route ID
        }

        return true;
    }

    public List<Route> getRoutesByFlowId(String id) {

        List<Route> routes = context.getRoutesByGroup(id);

        if(!routes.isEmpty()){
            return routes;
        }

        return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).toList();

    }

    private List<RouteStartupOrder> getRoutesStartupOrderByFlowId(String id) {
        List<RouteStartupOrder> routeStartupOrder = context.getCamelContextExtension().getRouteStartupOrder();
        return routeStartupOrder.stream().filter(r -> r.getRoute().getId().startsWith(id)).toList();
    }

    private List<String> getAllRoutesByFlowId(String id) {

        List<Route> routes = context.getRoutesByGroup(id);

        if(routes != null && !routes.isEmpty()){
            return routes
                    .stream()
                    .map(Route::getId)
                    .toList();
        }

        return context.getRoutes().stream()
                .map(Route::getId)
                .filter(routeId -> routeId.startsWith(id))
                .toList();
    }

    public Component getComponent(List<Class<?>> classes, String scheme) {

        Component component = null;
        for (Class<?> classToLoad : classes) {
            String className = classToLoad.getName().toLowerCase();
            if (className.endsWith(scheme + "component")) {
                try {
                    component = (Component) classToLoad.getDeclaredConstructor().newInstance();
                    break; // Exit loop once a match is found
                } catch (InstantiationException | IllegalAccessException |
                         InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException("Failed to instantiate component: " + className, e);
                }
            }
        }

        return component;
    }

    // Dynamically initializes and registers route-specific SSL contexts for Mutual TLS client authentication.
    private void initializeMutualTLS(TreeMap<String, String> properties) throws Exception {

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (key.startsWith("route") && value.contains("<setProperty name=\"httpMutualSSL\">") &&
                        value.contains("<constant>true</constant>")) {

                    String routeId = extractRouteIdFromKey(key);

                    // Parse XML snippet in value to extract resource and authPassword
                    String keystoreResource = extractPropertyValue(value, "resource");
                    String keystorePassword = extractPropertyValue(value, "authPassword");

                    if (keystoreResource != null && !keystoreResource.isEmpty() &&
                            keystorePassword != null && !keystorePassword.isEmpty()) {

                        String contextId = "mutualSslContext_" + routeId;

                        SSLManager sslManager = new SSLManager();
                        sslManager.setMutualSsl(keystoreResource, keystorePassword, contextId, this.context.getRegistry());
                    }
                }

        }
    }

    // Simple method to extract <setProperty name="X"><constant>VALUE</constant></setProperty>
    private String extractPropertyValue(String xmlSnippet, String propertyName) {
        String startTag = "<setProperty name=\"" + propertyName + "\">";
        int startIdx = xmlSnippet.indexOf(startTag);
        if (startIdx == -1) return null;
        int constStart = xmlSnippet.indexOf("<constant>", startIdx);
        int constEnd = xmlSnippet.indexOf("</constant>", constStart);
        if (constStart == -1 || constEnd == -1) return null;
        return xmlSnippet.substring(constStart + "<constant>".length(), constEnd).trim();
    }

    // Extracts the route ID by taking the segment immediately following 'route.' from a key string.
    private String extractRouteIdFromKey(String key) {
        if (key == null || !key.startsWith("route.")) {
            return null;
        }
        String[] parts = key.split("\\.");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }

    private void initializeOAuth2EmailAuthenticator(TreeMap<String, String> properties) {

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!entry.getKey().endsWith(".routetemplate") ||
                    (!entry.getValue().contains("routeTemplateRef=\"smtp-action\"") &&
                    !entry.getValue().contains("routeTemplateRef=\"imaps-source\""))
            ) {
                continue;
            }

            boolean isConsumer = entry.getValue().contains("routeTemplateRef=\"imaps-source\"");

            try {
                // ---------- Parse XML ----------
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document doc = db.parse(new InputSource(new StringReader(entry.getValue())));

                Element root = doc.getDocumentElement();
                Element templatedRoute = (Element)
                        root.getElementsByTagNameNS("*", "templatedRoute").item(0);

                if (templatedRoute == null) {
                    continue;
                }

                // ---------- Collect parameter elements ----------
                Map<String, Element> paramElements = new LinkedHashMap<>();
                NodeList params = templatedRoute.getElementsByTagNameNS("*", "parameter");

                for (int i = 0; i < params.getLength(); i++) {
                    Element p = (Element) params.item(i);
                    paramElements.put(p.getAttribute("name"), p);
                }

                Map<String, String> allParams = new LinkedHashMap<>();
                for (Map.Entry<String, Element> e : paramElements.entrySet()) {
                    String value = e.getValue().getAttribute("value").replace("&amp;", "&").trim();
                    allParams.put(e.getKey(), value);
                }

                String username = cleanRaw(allParams.get("username"));
                String tenantDbName = cleanRaw(allParams.get("tenantDbName"));

                String accessToken = allParams.get("accessToken");
                if (accessToken != null && accessToken.startsWith("RAW(") && accessToken.endsWith(")")) {
                    accessToken = accessToken.substring(4, accessToken.length() - 1);
                }
                accessToken = decryptAccessToken(accessToken);

                if (StringUtils.isAnyEmpty(username, accessToken, tenantDbName)) {
                    log.warn("Skipped OAuth2 email authenticator, missing username/accessToken/tenantDbName | key={}", entry.getKey());
                    continue;
                }

                // ---------- Build & bind the Authenticator ----------
                // Token is resolved lazily (per connection attempt) so refresh works without
                // rebuilding the bean - see OAuth2MailAuthenticator below.
                String beanId = "mailAuth-" + UUID.randomUUID();

                MailAuthenticator authenticator = new OAuth2MailAuthenticator(username, accessToken, tenantDbName, isConsumer);
                context.getRegistry().bind(beanId, authenticator);

                // ---------- Wire it in as a real Kamelet property, not into "options" ----------
                setOrCreateParameter(doc, templatedRoute, paramElements, "authenticator", "#" + beanId);
                setOrCreateParameter(doc, templatedRoute, paramElements, "authMechanisms", "XOAUTH2");
                setOrCreateParameter(doc, templatedRoute, paramElements, "authEnabled", "true");

                // ---------- Serialize XML back ----------
                Transformer tf = TransformerFactory.newInstance().newTransformer();
                tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

                StringWriter sw = new StringWriter();
                tf.transform(new DOMSource(doc), new StreamResult(sw));

                entry.setValue(sw.toString());

            } catch (Exception e) {
                log.error("initializeOAuth2EmailAuthenticator failed.", e);
            }
        }
    }

    private void setOrCreateParameter(Document doc, Element templatedRoute,
                                      Map<String, Element> paramElements,
                                      String name, String value) {
        if (paramElements.containsKey(name)) {
            paramElements.get(name).setAttribute("value", value);
        } else {
            Element param = doc.createElementNS(templatedRoute.getNamespaceURI(), "parameter");
            param.setAttribute("name", name);
            param.setAttribute("value", value);
            templatedRoute.appendChild(param);
            paramElements.put(name, param);
        }
    }

    private @Nullable String decryptAccessToken(String accessToken) {
        try {
            accessToken = encryptionUtil.decrypt(accessToken);
        } catch (Exception e) {
            log.error("ERROR to decrypt accessToken - "+accessToken, e);
        }
        return accessToken;
    }

    private void initializeAs2InboundSecurity(TreeMap<String, String> properties) {

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (!entry.getKey().endsWith(".routetemplate") || !entry.getValue().contains("routeTemplateRef=\"as2-source\"")) {
                continue;
            }

            try {
                // ---------- Parse XML ----------
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                DocumentBuilder db = dbf.newDocumentBuilder();

                Document doc = db.parse(
                        new InputSource(new StringReader(entry.getValue()))
                );

                Element root = doc.getDocumentElement();
                Element templatedRoute = (Element)
                        root.getElementsByTagNameNS("*", "templatedRoute").item(0);

                if (templatedRoute == null) {
                    continue;
                }

                // ---------- Collect parameter elements ----------
                Map<String, Element> paramElements = new LinkedHashMap<>();
                NodeList params = templatedRoute.getElementsByTagNameNS("*", "parameter");

                for (int i = 0; i < params.getLength(); i++) {
                    Element p = (Element) params.item(i);
                    paramElements.put(p.getAttribute("name"), p);
                }

                // ---------- Extract parameter values ----------
                Map<String, String> allParams = new LinkedHashMap<>();
                for (Map.Entry<String, Element> e : paramElements.entrySet()) {
                    String value = e.getValue()
                            .getAttribute("value")
                            .replace("&amp;", "&")
                            .trim();
                    allParams.put(e.getKey(), value);
                }

                // ---------- Extract password & alias ----------
                String password = cleanRaw(allParams.get("password"));
                String alias = cleanRaw(allParams.get("alias"));

                // ---------- Bind keys and rebuild params ----------
                Set<String> securityParams = Set.of(
                        "signingPrivateKey",
                        "decryptingPrivateKey",
                        "signingCertificateChain",
                        "validateSigningCertificateChain"
                );

                String uniqueId = UUID.randomUUID().toString();
                Map<String, String> boundParams = new LinkedHashMap<>();

                for (Map.Entry<String, String> e : allParams.entrySet()) {
                    String key = e.getKey();
                    String value = e.getValue();

                    if (securityParams.contains(key)) {

                        String beanId = key + "-" + uniqueId;
                        String cleanedValue = cleanRaw(value);
                        Object keyObject;

                        switch (key) {
                            case "signingCertificateChain":
                                keyObject = AS2KeyProcessor.getSigningCertificateChain(
                                        new URI(cleanedValue), password, alias);
                                boundParams.put(
                                        "signingAlgorithm",
                                        AS2KeyProcessor.getSigningAlgorithm(
                                                (java.security.cert.Certificate[]) keyObject));
                                break;

                            case "signingPrivateKey":
                                keyObject = AS2KeyProcessor.getSigningPrivateKey(
                                        new URI(cleanedValue), password, alias);
                                break;

                            case "decryptingPrivateKey":
                                keyObject = AS2KeyProcessor.getDecryptingPrivateKey(
                                        new URI(cleanedValue), password, alias);
                                break;

                            default:
                                keyObject = AS2KeyProcessor.getValidateSigningCertificateChain(
                                        new URI(cleanedValue));
                        }

                        context.getRegistry().bind(beanId, keyObject);
                        boundParams.put(key, "#" + beanId);

                    } else {
                        boundParams.put(key, value);
                    }
                }

                // ---------- Rebuild query ----------
                StringBuilder newQuery = new StringBuilder();
                boolean first = true;

                for (Map.Entry<String, String> e : boundParams.entrySet()) {
                    String key = e.getKey();

                    if (key.equals("password") || key.equals("alias")) {
                        continue;
                    }

                    if (!first) newQuery.append('&');
                    newQuery.append(key).append('=').append(e.getValue());
                    first = false;
                }

                String escapedQuery = newQuery.toString().replace("&", "&amp;");

                // ---------- Write back to XML ----------
                if (paramElements.containsKey("uri")) {
                    paramElements.get("uri")
                            .setAttribute("value", "as2?" + escapedQuery);
                }

                if (paramElements.containsKey("options")) {
                    paramElements.get("options")
                            .setAttribute("value", escapedQuery);
                }

                for (String sec : securityParams) {
                    if (paramElements.containsKey(sec)) {
                        paramElements.get(sec)
                                .setAttribute("value",
                                        boundParams.get(sec).replace("&", "&amp;"));
                    }
                }

                // Remove sensitive parameters
                if (paramElements.containsKey("password")) {
                    templatedRoute.removeChild(paramElements.get("password"));
                }
                if (paramElements.containsKey("alias")) {
                    templatedRoute.removeChild(paramElements.get("alias"));
                }

                // ---------- Serialize XML ----------
                Transformer tf = TransformerFactory.newInstance().newTransformer();
                tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

                StringWriter sw = new StringWriter();
                tf.transform(new DOMSource(doc), new StreamResult(sw));

                entry.setValue(sw.toString());

            } catch (Exception e) {
                log.error("initializeAs2InboundSecurity failed.", e);
            }
        }
    }

    private String cleanRaw(String value) {
        if (value == null) return "";
        if (value.startsWith("RAW(") && value.endsWith(")")) {
            value = value.substring(4, value.length() - 1);
        }
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    public void setConnection(TreeMap<String, String> props, String key) throws Exception {
        new Connection(context, props, key).start();
    }

    public void createConnections(TreeMap<String, String> properties) throws Exception {

        for (Map.Entry<String, String> entry : properties.entrySet()) {

            String key = entry.getKey();

            if (key.endsWith("connection.id")) {
                setConnection(properties, key);
            }

        }

    }

    private String setProperty(TreeMap<String, String> properties,String propertyKey, String defaultValue){

        if(properties.containsKey(propertyKey)){
            return properties.get(propertyKey);
        }

        return defaultValue;

    }

    // Add as a nested enum or a separate file
    public enum FlowStatus {
        STARTED("started"),
        STOPPED("stopped"),
        PAUSED("paused"),
        RESUMED("started"),
        RESTARTED("restarted"),
        UNCONFIGURED("unconfigured"),
        ERROR("error");

        private final String value;

        FlowStatus(String value) { this.value = value; }

        @Override
        public String toString() { return value; }
    }

}
