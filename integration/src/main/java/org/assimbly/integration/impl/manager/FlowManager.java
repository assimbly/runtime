package org.assimbly.integration.impl.manager;

import org.apache.camel.*;
import java.util.*;

import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedRouteGroupMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.api.management.mbean.RouteError;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.RouteStartupOrder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.blocks.processors.AS2KeyProcessor;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.dil.loader.RouteLoader;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.IntegrationUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

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

    private ServiceStatus status;

    private final CamelContext context;
    private final ManagedCamelContext managedContext;
    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    private static final long STOP_TIMEOUT = 300;

    public FlowManager(CamelContext context) {
        this.context = context;
        this.managedContext = context.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);
    }

    public String loadFlow(String flowId, TreeMap<String, String> properties) {

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);

        try {

            //initialize security for AS2
            initializeSecurity(properties);

            //create connections & install dependencies if needed
            createConnections(properties);

            FlowLoader flow = new FlowLoader(properties, report);

            flow.addRoutesToCamelContext(context);

            if(flow.isFlowLoaded()){
                return finishReport(report, flowId, "start", "Started flow successfully", "info","success");
            }else{
                stopFlow(flowId, STOP_TIMEOUT);
                return finishReport(report, flowId, "start", "Start flow failed", "error","failed");
            }

        } catch (Exception e) {
            log.error("Load flow failed: ", e);
            return finishReport(report, flowId, "start", e.getMessage(), "error","failed");
        }

    }

    private void initializeSecurity(TreeMap<String, String> properties) throws Exception {
        if (properties.containsKey("security.as2")) {
            log.info("Initialize AS2 Inbound security");
            initializeAs2InboundSecurity(properties);
        }
        if (properties.containsKey("security.mutualtls")) {
            log.info("Initialize Mutual TLS");
            initializeMutualTLS(properties);
        }
    }

    public boolean hasFlow(String flowId) {

        List<Route> routes = context.getRoutesByGroup(flowId);

        return routes != null && !routes.isEmpty();

    }

    public void startAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {

        log.info("Starting all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                loadFlow(flowId, flowProps);
                log.info("Started flow: {}", flowId);
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

    public String pauseAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Pause all flows");

        flowsMap.forEach((flowId, _) -> {
            try {
                pauseFlow(flowId);
                log.info("Paused flow: {}", flowId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return FlowStatus.STARTED.toString();
    }

    public String resumeAllFlows(ConcurrentMap<String, TreeMap<String, String>> flowsMap) {
        log.info("Resume all flows");

        flowsMap.forEach((flowId, flowProps) -> {
            try {
                resumeFlow(flowId, flowProps);
                log.info("Resumed flow: {}", flowId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        return FlowStatus.RESUMED.toString();
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

        FlowLoaderReport report = new FlowLoaderReport(routeId, routeId);

        try {
            String routeXml = route.startsWith("<route")
                    ? route
                    : new XMLFileConfiguration().getRouteConfiguration(route);

            RouteLoader routeLoader = new RouteLoader(routeId, routeXml, report);

            routeLoader.addRoutesToCamelContext(context);

            String result = routeLoader.getReport();

            return finishReport(report, routeId, "start", result, "info", "success");

        } catch (Exception e) {
            return finishReport(report, routeId, "start", "Route install failed | error=" + e.getMessage(), "error", "failed");
        }
    }

    public String startFlow(String flowId, TreeMap<String, String> flowProperties, long timeout) {

        if (hasFlow(flowId)) {
            stopFlow(flowId, timeout, false);
        }

        return loadFlow(flowId, flowProperties);

    }

    public String restartFlow(String flowId, TreeMap<String, String> flowProperties, long timeout) {
        return startFlow(flowId, flowProperties, timeout);
    }

    public String stopFlow(String flowid, long timeout) {
        return stopFlow(flowid, timeout, true);
    }

    public String stopFlow(String flowId, long timeout, boolean enableReport) {

        if (!hasFlow(flowId)) {
            FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);
            String errorMessage = "Flow is not installed";
            return  finishReport(report, flowId, "stop", errorMessage, "error","failed");
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
                FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);
                return finishReport(report, flowId, "stop", "Stopped flow successfully", "info" ,"success");
            }

        } catch (Exception e) {

            log.error("Stop flow failed. | flowid={}", flowId, e);

            if (enableReport) {
                FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);
                return finishReport(report, flowId, "stop", "Stop flow failed | error=" + e.getMessage(), "error","failed");
            }

        }

        return FlowStatus.STOPPED.toString();

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

    public String pauseFlow(String flowId) {

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);

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

    public String resumeFlow(String flowId, TreeMap<String, String> flowProperties) {

        FlowLoaderReport report = new FlowLoaderReport(flowId, flowId);

        RouteController routeController = context.getRouteController();

        if (!hasFlow(flowId)) {
            String errorMessage = "Flow is not installed";
            return  finishReport(report, flowId, "resume", errorMessage, "error","failed");
        }

        try {

            List<Route> routeList = getRoutesByFlowId(flowId);
            for (Route route : routeList) {
                String routeId = route.getId();
                status = routeController.getRouteStatus(routeId);

                if (status.isSuspended()) {
                    routeController.resumeRoute(routeId);
                    log.info("Resumed flow  | flowid={} | stepid={}", flowId, routeId);
                } else if (status.isStopped()) {
                    log.info("Starting route as route {} is currently stopped (not suspended)", flowId);
                    startFlow(routeId, flowProperties, STOP_TIMEOUT);
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

    public String finishReport(FlowLoaderReport report, String flowid, String event, String message, String messageType, String status) {

        String eventCapitalized = StringUtils.capitalize(event);

        if (messageType.equalsIgnoreCase("error")) {
            log.error("{} flow failed | flowid={} message={}", eventCapitalized, flowid, message);
        } else if (messageType.equalsIgnoreCase("warning")) {
            log.warn("{} flow failed | flowid={} message={}", eventCapitalized, flowid, message);
        } else {
            log.info("{} | flowid={}", message, flowid);
        }

        report.finishReport(event, "0", message, status);

        return report.getReport();

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
