package org.assimbly.integration.impl.manager;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.api.management.mbean.RouteError;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.component.jms.ClassicJmsHeaderFilterStrategy;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RouteStartupOrder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.dil.loader.RouteLoader;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.DependencyUtil;
import org.assimbly.util.INetUtil;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class FlowManager {

    protected static final Logger log = LoggerFactory.getLogger(FlowManager.class);

    private TreeMap<String, String> props;
    private String loadReport;
    private FlowLoaderReport flowLoaderReport;
    private ServiceStatus status;

    private final CamelContext context;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private static final String RESOURCE_PROP = "resource";
    private static final String AUTH_PASSWORD_PROP = "authPassword";
    private static final String BROKER_HOST = "ASSIMBLY_BROKER_HOST";
    private static final String BROKER_PORT = "ASSIMBLY_BROKER_PORT";
    private static final long STOP_TIMEOUT = 300;

    public FlowManager(CamelContext context) {
        this.context = context;
    }

    public String loadFlow(TreeMap<String, String> properties, SSLManager sslManager) {

        this.props = properties;

        try {

            if (properties.containsKey("frontend") && properties.get("frontend").equals("dovetail")) {

                // add custom connections if needed
                addCustomActiveMQConnection();

                // add custom connections if needed
                addCustomRabbitMQConnection(new TreeMap<>(properties));

            }

            //create connections & install dependencies if needed
            createConnections(properties);

            Map<String, String> mutualSSLInfoMap = sslManager.getMutualSSLInfoFromProps(properties);
            if (mutualSSLInfoMap != null && !mutualSSLInfoMap.isEmpty()) {
                // add certificate on keystore
                sslManager.addCertificateFromUrl(mutualSSLInfoMap.get(RESOURCE_PROP), mutualSSLInfoMap.get(AUTH_PASSWORD_PROP));
            }

            FlowLoader flow = new FlowLoader(properties, flowLoaderReport);

            flow.addRoutesToCamelContext(context);

            loadReport = flow.getReport();

            if (!flow.isFlowLoaded()) {
                return "error";
            }

            return "started";

        } catch (Exception e) {
            log.error("add flow failed: ", e);
            return "error reason: " + e.getMessage();
        }

    }

    public boolean hasFlow(String id) {

        List<Route> routes = context.getRoutesByGroup(id);

        return routes != null && !routes.isEmpty();

    }

    public void startAllFlows(SSLManager sslManager, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Starting all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                flowLoaderReport = new FlowLoaderReport(flowId, flowId);
                loadFlow(flowProps, sslManager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

    }

    public String restartAllFlows(SSLManager sslManager, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Restarting all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                flowLoaderReport = new FlowLoaderReport(flowId, flowId);
                loadFlow(props, sslManager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return "restarted";
    }

    public String pauseAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Pause all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                pauseFlow(flowId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return "paused";
    }

    public String resumeAllFlows(SSLManager sslManager, ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Resume all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                resumeFlow(flowId, flowProps, sslManager);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return "started";
    }

    public String stopAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Stopping all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                stopFlow(flowId, 250, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return "stopped";
    }

    public String installRoute(String routeId, String route) throws Exception {

        initFlowActionReport(routeId);

        if (!route.startsWith("<route")) {
            route = new XMLFileConfiguration().getRouteConfiguration(route);
        }

        try {

            RouteLoader routeLoader = new RouteLoader(routeId, route, flowLoaderReport);

            routeLoader.addRoutesToCamelContext(context);

            loadReport = routeLoader.getReport();

            finishFlowActionReport(routeId, "start", "Started flow successfully", "info");
        } catch (Exception e) {
            finishFlowActionReport(routeId, "error", "Failed starting flow", e.getMessage());
        }

        return loadReport;

    }

    public String startFlow(String flowId, TreeMap<String, String> flowProperties, SSLManager sslManager, long timeout) {

        initFlowActionReport(flowId);

        if (hasFlow(flowId)) {
            stopFlow(flowId, timeout, false);
        }

        try {

            String result = loadFlow(flowProperties, sslManager);

            if (result.equals("started")) {
                finishFlowActionReport(flowId, "start", "Started flow successfully", "info");
            } else {
                stopFlow(flowId, timeout, false);
                finishFlowActionReport(flowId, "error", result, "error");
            }

        } catch (Exception e) {
            stopFlow(flowId, STOP_TIMEOUT, false);
            finishFlowActionReport(flowId, "error", "Start flow failed | error=" + e.getMessage(), "error");
            log.error("Start flow failed. | flowid={}", flowId, e);
        }

        return loadReport;

    }

    public String restartFlow(String flowId, TreeMap<String, String> flowProperties, SSLManager sslManager, long timeout) {

        try {
            startFlow(flowId, flowProperties, sslManager, timeout);
        } catch (Exception e) {
            log.error("Restart flow failed. | flowid={}", flowId, e);
            finishFlowActionReport(flowId, "error", e.getMessage(), "error");
        }

        return loadReport;

    }

    public String stopFlow(String flowid, long timeout) {
        return stopFlow(flowid, timeout, true);
    }

    public String stopFlow(String flowid, long timeout, boolean enableReport) {

        if (enableReport) {
            initFlowActionReport(flowid);
        }

        try {
            // gracefully shutdown routes using startup order
            List<RouteStartupOrder> routeStartupOrders = getRoutesStartupOrderByFlowId(flowid);
            context.getShutdownStrategy().shutdown(context, routeStartupOrders, timeout, TimeUnit.MILLISECONDS);
            for (RouteStartupOrder routeStartupOrder : routeStartupOrders) {
                context.removeRoute(routeStartupOrder.getRoute().getId());
            }

            // remove leftover routes
            List<String> leftoverRoutes = getAllRoutesByFlowId(flowid);
            if (!leftoverRoutes.isEmpty()) {
                for (String routeId : leftoverRoutes) {
                    removeRoute(routeId);
                }
            }

            if (enableReport) {
                finishFlowActionReport(flowid, "stop", "Stopped flow successfully", "info");
            }

        } catch (Exception e) {
            if (enableReport) {
                finishFlowActionReport(flowid, "error", "Stop flow failed | error=" + e.getMessage(), "error");
            }
            log.error("Stop flow failed. | flowid={}", flowid, e);
        }

        return loadReport;

    }

    private void removeRoute(String routeId) {
        try {
            if (context.getRoute(routeId) != null) {
                context.getRouteController().stopRoute(routeId);
                context.removeRoute(routeId);
            }
        } catch (Exception e) {
            log.error("Error removing route: {} Error message: {}", routeId, e.getMessage());
        }
    }


    public String pauseFlow(String flowid) {

        initFlowActionReport(flowid);

        RouteController routeController = context.getRouteController();

        try {

            if (hasFlow(flowid)) {

                List<Route> routeList = getRoutesByFlowId(flowid);
                status = routeController.getRouteStatus(routeList.getFirst().getId());

                for (Route route : routeList) {
                    if (!routeController.getRouteStatus(route.getId()).isSuspendable()) {
                        finishFlowActionReport(flowid, "error", "Flow isn't suspendable (Step " + route.getId() + ")", "error");
                        return loadReport;
                    }
                }

                for (Route route : routeList) {
                    String routeId = route.getId();
                    routeController.suspendRoute(routeId);
                }
                finishFlowActionReport(flowid, "pause", "Paused flow successfully", "info");
            } else {
                String errorMessage = "Configuration is not set (use setConfiguration or setFlowConfiguration)";
                finishFlowActionReport(flowid, "error", errorMessage, "error");
            }
        } catch (Exception e) {
            log.error("Pause flow failed. | flowid={}", flowid, e);
            stopFlow(flowid, STOP_TIMEOUT); //Stop flow if one of the routes cannot be paused.
            finishFlowActionReport(flowid, "error", e.getMessage(), "error");
        }

        return loadReport;

    }

    public String resumeFlow(String flowId, TreeMap<String, String> flowProperties, SSLManager sslManager) {

        initFlowActionReport(flowId);

        RouteController routeController = context.getRouteController();

        try {

            if (hasFlow(flowId)) {

                List<Route> routeList = getRoutesByFlowId(flowId);
                for (Route route : routeList) {
                    String routeId = route.getId();
                    status = routeController.getRouteStatus(routeId);

                    if (status.isSuspended()) {
                        routeController.resumeRoute(routeId);
                        log.info("Resumed flow  | flowid={} | stepid={}", flowId, routeId);
                    } else if (status.isStopped()) {
                        log.info("Starting route as route {} is currently stopped (not suspended)", flowId);
                        startFlow(routeId, flowProperties, sslManager, STOP_TIMEOUT);
                    }

                }
                finishFlowActionReport(flowId, "resume", "Resumed flow successfully", "info");
            } else {
                String errorMessage = "Configuration is not set (use setConfiguration or setFlowConfiguration)";
                finishFlowActionReport(flowId, "error", errorMessage, "error");
            }

        } catch (Exception e) {
            log.error("Resume flow {} failed.", flowId, e);
            finishFlowActionReport(flowId, "error", e.getMessage(), "error");
        }

        return loadReport;

    }

    public void initFlowActionReport(String flowid) {
        flowLoaderReport = new FlowLoaderReport(flowid, flowid);
    }

    public void finishFlowActionReport(String flowid, String event, String message, String messageType) {

        String eventCapitalized = StringUtils.capitalize(event);

        //logs event to
        if (messageType.equalsIgnoreCase("error")) {
            log.error("{} flow {} failed | flowid={} message={}", eventCapitalized, flowid, flowid, message);
        } else if (messageType.equalsIgnoreCase("warning"))
            log.warn("{} flow{} failed | flowid={} message={}", eventCapitalized, flowid, flowid, message);
        else {
            log.info("{} | flowid={}", message, flowid);
        }

        flowLoaderReport.finishReport(event, "0", message);

        loadReport = flowLoaderReport.getReport();

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
        ManagedCamelContext managedContext = context.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);

        for (Route r : routeList) {
            String routeId = r.getId();
            ManagedRouteMBean route = managedContext.getManagedRoute(routeId);

            if (route != null) {
                RouteError lastError = route.getLastError();
                if (lastError != null) {
                    sb.append("RouteID: ");
                    sb.append(routeId);
                    sb.append("Error: ");
                    sb.append(lastError);
                    sb.append(";");
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

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                String numberOfEntries = getFlowAlertsCount(flowId);
                numberOfEntriesList.put(flowId, numberOfEntries);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return numberOfEntriesList;

    }

    public String getFlowAlertsCount(String id) throws Exception {

        Date date = new Date();
        String today = new SimpleDateFormat("yyyyMMdd").format(date);
        File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

        if (file.exists()) {
            List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
            return Integer.toString(lines.size());
        } else {
            return "0";
        }
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
                .filter(flowId -> filter == null || filter.isEmpty() || getFlowStatus(flowId).equalsIgnoreCase(filter))
                .collect(Collectors.toSet());
    }

    public String getListOfFlows(String filter, String mediaType) throws Exception {

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
            result = DocConverter.convertJsonToXml(flowsObject.toString());
        }

        return result;

    }

    public String getFlowInfo(String flowId, String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

        TreeMap<String, String> flowProperties = flowsMap.get(flowId);

        JSONObject json = new JSONObject();
        JSONObject flow = new JSONObject();

        if (flowProperties != null) {
            flow.put("id", flowProperties.get("id"));
            flow.put("name", flowProperties.get("flow.name"));
            flow.put("isRunning", isFlowStarted(flowId));
            flow.put("status", getFlowStatus(flowId));
            flow.put("version", flowProperties.get("flow.version"));
            flow.put("environment", flowProperties.get("environment"));
            flow.put("uptime", getFlowUptime(flowId));
        } else {
            flow.put("id", flowId);
            flow.put("isRunning", false);
            flow.put("status", getFlowStatus(flowId));
        }

        json.put("flow", flow);

        String integrationInfo = json.toString(4);
        if (mediaType.contains("xml")) {
            integrationInfo = DocConverter.convertJsonToXml(integrationInfo);
        }

        return integrationInfo;

    }

    public String getListOfFlowsDetails(String filter, String mediaType, ConcurrentMap<String, TreeMap<String, String>> flowsMap) throws Exception {

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
            result = DocConverter.convertJsonToXml(flowsObject.toString());
        }

        return result;

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
        Document doc = DocConverter.convertStringToDoc(configurationUTF8);
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

    public boolean removeFlow(String id) throws Exception {

        boolean removed = false;
        List<Route> routes = getRoutesByFlowId(id);

        if (routes != null && !routes.isEmpty()) {
            for (Route route : routes) {
                route.getId();
                context.removeRoute(id);
                removed = true;
            }
        }

        return removed;

    }

    private static String getEnvironmentVariable() {
        String environmentVariable = System.getenv(BROKER_HOST);
        String value = "localhost";
        if (environmentVariable != null && !environmentVariable.isEmpty()) {
            value = environmentVariable;
        }
        return value;
    }

    private static int getEnvironmentVariableAsInteger(String envName, int defaultValue) {
        String environmentVariable = System.getenv(envName);
        int value = defaultValue;
        if (environmentVariable != null && !environmentVariable.isEmpty()) {
            try {
                value = Integer.parseInt(environmentVariable);
            } catch (NumberFormatException e) {
                log.error("Invalid value for {}: {}", envName, value);
            }
        }
        return value;
    }

    public List<Route> getRoutesByFlowId(String id) {

        List<Route> routes = context.getRoutesByGroup(id);

        if(routes != null && !routes.isEmpty()){
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

    public String resolveDependency(String scheme) {

        DefaultCamelCatalog catalog = new DefaultCamelCatalog();
        String jsonString = catalog.componentJSonSchema(scheme);

        if (jsonString == null || jsonString.isEmpty()) {
            log.info("Unknown scheme: {}. Error: Could not found scheme in catalog.", scheme);
            return "Unknown scheme: " + scheme + ". Error: Could not found scheme in catalog.";
        }

        JSONObject componentSchema = new JSONObject(jsonString);
        JSONObject component = componentSchema.getJSONObject("component");

        String groupId = component.getString("groupId");
        String artifactId = component.getString("artifactId");
        String version = catalog.getCatalogVersion();

        String dependency = groupId + ":" + artifactId + ":" + version;
        String result;

        try {
            List<Class<?>> classes = resolveMavenDependency(groupId, artifactId, version);
            Component camelComponent = getComponent(classes, scheme);
            context.addComponent(scheme, camelComponent);
            result = "Dependency " + dependency + " resolved";
        } catch (Exception e) {
            log.error("Dependency {} resolved failed.", dependency, e);
            result = "Dependency " + dependency + " resolved failed. Error message: " + e.getMessage();
        }

        return result;

    }

    public List<Class<?>> resolveMavenDependency(String groupId, String artifactId, String version) throws Exception {
        DependencyUtil dependencyUtil = new DependencyUtil();
        List<Path> paths = dependencyUtil.resolveDependency(groupId, artifactId, version);
        return dependencyUtil.loadDependency(paths);
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


    private void addCustomActiveMQConnection() {

        try {

            Component activemqComp = this.context.getComponent("activemq");

            if (activemqComp == null) {

                String brokerHost = getEnvironmentVariable();
                int brokerPort = getEnvironmentVariableAsInteger(BROKER_PORT, 61616);
                String activemqUrl = String.format("tcp://%s:%s", brokerHost, brokerPort);

                log.info("Adding custom ActiveMQ connection. URL: {}", activemqUrl);

                this.context.addComponent("activemq", getJmsComponent(activemqUrl));

            }

        } catch (Exception e) {
            log.error("Error to add custom ActiveMQ connection", e);
        }

    }

    private void addCustomRabbitMQConnection(TreeMap<String, String> properties) {

        for (Map.Entry<String, String> entry : properties.entrySet()) {
            if (entry.getKey().startsWith("route") && entry.getValue().contains("rabbitmqConnectionFactory")) {

                String connection = StringUtils.substringBetween(entry.getValue(), "<rabbitmqConnectionFactory>", "</rabbitmqConnectionFactory>");
                String rabbitMQElement = "<rabbitmqConnectionFactory>" + connection + "</rabbitmqConnectionFactory>";

                if (connection == null) {
                    connection = StringUtils.substringBetween(entry.getValue(), "<rabbitmqConnectionFactory xmlns=\"http://camel.apache.org/schema/blueprint\">", "</rabbitmqConnectionFactory>");
                    rabbitMQElement = "<rabbitmqConnectionFactory xmlns=\"http://camel.apache.org/schema/blueprint\">" + connection + "</rabbitmqConnectionFactory>";
                }

                Map<String, String> connectionMap = stringToMap(connection);
                String connectionId = connectionMap.get("host") + "-" + connectionMap.get("port") + "-" + connectionMap.get("username");

                props.put("sink.1.connection.id", connectionId);
                props.put("connection." + connectionId + ".type", "rabbitmq");
                props.put("connection." + connectionId + ".host", connectionMap.get("host"));
                props.put("connection." + connectionId + ".vhost", connectionMap.get("vhost"));
                props.put("connection." + connectionId + ".port", connectionMap.get("port"));
                props.put("connection." + connectionId + ".username", connectionMap.get("username"));
                props.put("connection." + connectionId + ".password", connectionMap.get("password"));
                props.put(entry.getKey(), Strings.CS.replace(entry.getValue(), rabbitMQElement, ""));


            }
        }

    }

    private Map<String, String> stringToMap(String input) {
        Map<String, String> map = new LinkedHashMap<>();
        String[] pairs = StringUtils.split(input, ',');

        for (String pair : pairs) {
            if (StringUtils.contains(pair, '=')) {
                String key = StringUtils.substringBefore(pair, "=");
                String value = StringUtils.substringAfter(pair, "=");
                if (value.startsWith("RAW")) {
                    value = StringUtils.substringBetween(value, "RAW(", ")");
                }
                map.put(key, value);
            }
        }

        return map;

    }

    private static SjmsComponent getJmsComponent(String activemqUrl) {

        int maxConnections = getEnvironmentVariableAsInteger("AMQ_MAXIMUM_CONNECTIONS", 500);
        int idleTimeout = getEnvironmentVariableAsInteger("AMQ_IDLE_TIMEOUT", 5000);

        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(activemqUrl);

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(activeMQConnectionFactory);
        pooledConnectionFactory.setMaxConnections(maxConnections);
        pooledConnectionFactory.setIdleTimeout(idleTimeout);

        SjmsComponent sjmsComponent = new SjmsComponent();
        sjmsComponent.setConnectionFactory(pooledConnectionFactory);
        sjmsComponent.setHeaderFilterStrategy(new ClassicJmsHeaderFilterStrategy());

        return sjmsComponent;
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

            if (key.equals("flow.dependencies") && properties.get(key) != null) {

                String[] schemes = StringUtils.split(properties.get(key), ",");

                for (String scheme : schemes) {
                    createConnection(scheme);
                }
            }
        }

    }

    private void createConnection(String scheme) throws Exception {
        if (!scheme.equals("null") && context.getComponent(scheme.toLowerCase()) == null && !DependencyUtil.CompiledDependency.hasCompiledDependency(scheme.toLowerCase())) {

            log.warn("Component {} is not supported by Assimbly. Try to resolve dependency dynamically.", scheme);

            if (INetUtil.isHostAvailable("repo1.maven.org")) {
                log.info(resolveDependency(scheme));
            } else {
                log.error("Failed to resolve {}. No available internet is found. Cannot reach http://repo1.maven.org/maven2/", scheme);
            }

        }
    }

    public String getFlowReport() {
        return loadReport;
    }

}
