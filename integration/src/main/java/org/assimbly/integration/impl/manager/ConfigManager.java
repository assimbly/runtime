package org.assimbly.integration.impl.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.builder.ThreadPoolProfileBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.jetty12.JettyHttpComponent12;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.apache.camel.spi.PropertyConfigurer;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.spi.Tracer;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.SimpleRegistry;
import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.assimbly.cookies.CookieStore;
import org.assimbly.dil.blocks.beans.*;
import org.assimbly.dil.blocks.beans.enrich.EnrichStrategy;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;
import org.assimbly.dil.blocks.processors.*;
import org.assimbly.dil.event.EventConfigurer;
import org.assimbly.dil.event.domain.Collection;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.mail.component.mail.AttachmentAttacher;
import org.assimbly.mail.component.mail.MailComponent;
import org.assimbly.mail.dataformat.mime.multipart.MimeMultipartDataFormat;
import org.assimbly.multipart.processor.MultipartProcessor;
import org.assimbly.util.BaseDirectory;
import org.assimbly.util.mail.ExtendedHeaderFilterStrategy;
import org.assimbly.xmltojson.CustomXmlJsonDataFormat;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ConfigManager {

    protected static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private final CamelContext context;
    private final SimpleRegistry registry;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();

    public ConfigManager(CamelContext context, SimpleRegistry registry) {
        this.context = context;
        this.registry = registry;
    }


    public void setDebugging(boolean debugging) {
        context.setDebugging(debugging);
    }

    public void setTracing(boolean tracing, String traceType) {

        if (traceType.equalsIgnoreCase("backlog")) {
            context.setBacklogTracing(true);
        } else if (traceType.equalsIgnoreCase("default")) {
            Tracer tracer = context.getTracer();
            tracer.setEnabled(tracing);
        }

    }


    public void setStreamCaching(boolean streamCaching) {
        context.setStreamCaching(streamCaching);
        context.getStreamCachingStrategy().setBufferSize(32 * 1024);
    }

    public void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout) {
        context.getShutdownStrategy().setSuppressLoggingOnTimeout(suppressLoggingOnTimeout);
        context.getShutdownStrategy().setTimeUnit(TimeUnit.MILLISECONDS);
    }

    public void setCertificateStore(boolean certificateStore, SSLManager sslManager) throws Exception {
        if (certificateStore) {
            sslManager.setSSLContext(context, registry);
        }
    }

    public void setMetrics(boolean metrics, StatsManager statsManager) {
        if (metrics) {
            statsManager.setMetrics();
        }
    }

    public void setHistoryMetrics(boolean setHistoryMetrics, StatsManager statsManager) {
        if (setHistoryMetrics) {
            statsManager.setHistoryMetrics();
        }
    }

    public void setHealthChecks(boolean enable, StatsManager statsManager) {
        statsManager.setHealthChecks(enable);
    }

    public void setDefaultBlocks() throws Exception {

        //Add services
        context.addService(new MailComponent());
        context.addService(new MimeMultipartDataFormat());
        context.addService(new CustomXmlJsonDataFormat());

        DirectComponent directComponent = new DirectComponent();
        directComponent.setTimeout(300000);

        //Add components to a custom name
        context.addComponent("sync", directComponent);
        context.addComponent("async", new SedaComponent());

        KameletComponent kameletComponent = new KameletComponent();
        context.addComponent("function", kameletComponent);

        JettyHttpComponent12 jettyHttpComponent12 = new JettyHttpComponent12();
        jettyHttpComponent12.setRequestHeaderSize(80000);
        jettyHttpComponent12.setResponseHeaderSize(80000);

        context.addComponent("jetty-nossl", jettyHttpComponent12);
        context.addComponent("jetty", jettyHttpComponent12);
        context.addComponent("rabbitmq", new SpringRabbitMQComponent());
        context.addComponent("activemq", new SjmsComponent());

        // Add bean/processors and other custom classes to the registry
        registry.bind("AggregateStrategy", new AggregateStrategy());
        registry.bind("AttachmentAttacher", new AttachmentAttacher());
        registry.bind("CurrentAggregateStrategy", new AggregateStrategy());
        registry.bind("CurrentEnrichStrategy", new EnrichStrategy());
        registry.bind("CustomHttpHeaderFilterStrategy", new CustomHttpHeaderFilterStrategy());
        registry.bind("CustomHttpBinding", new CustomHttpBinding());
        registry.bind("flowCookieStore", new CookieStore());
        registry.bind("InputStreamToStringProcessor", new InputStreamToStringProcessor());
        registry.bind("JsonAggregateStrategy", new JsonAggregateStrategy());
        registry.bind("ManageFlowProcessor", new ManageFlowProcessor());
        registry.bind("multipartProcessor", new MultipartProcessor());
        registry.bind("RoutingRulesProcessor", new RoutingRulesProcessor());
        registry.bind("SetOriginalMessageProcessor", new SetOriginalMessageProcessor());
        registry.bind("SetBodyProcessor", new SetBodyProcessor());
        registry.bind("SetHeadersProcessor", new SetHeadersProcessor());
        registry.bind("SetPatternProcessor", new SetPatternProcessor());
        registry.bind("Unzip", new UnzipProcessor());
        registry.bind("uuid-function", new UuidExtensionFunction());
        registry.bind("XmlAggregateStrategy", new XmlAggregateStrategy());
        registry.bind("FlowLogger", new FlowLogger());
        registry.bind("exceptionAsJson", new ExceptionAsJsonProcessor());

    }

    public void setDefaultThreadProfile(int poolSize, int maxPoolSize, int maxQueueSize) {
        context.getExecutorServiceManager().getDefaultThreadPoolProfile().setPoolSize(poolSize);
        context.getExecutorServiceManager().getDefaultThreadPoolProfile().setMaxPoolSize(maxPoolSize);
        context.getExecutorServiceManager().getDefaultThreadPoolProfile().setMaxQueueSize(maxQueueSize);
    }

    public void setThreadProfile(String name, int poolSize, int maxPoolSize, int maxQueueSize) {
        ThreadPoolProfileBuilder builder = new ThreadPoolProfileBuilder(name);
        builder.poolSize(poolSize).maxPoolSize(maxPoolSize).maxQueueSize(maxQueueSize).rejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns).keepAliveTime(10L);
        context.getExecutorServiceManager().registerThreadPoolProfile(builder.build());
    }

    public void setGlobalOptions() {

        //enable breadcrumb for tracing
        context.setUseBreadcrumb(true);

        //enable performance stats
        context.getManagementStrategy().getManagementAgent().setLoadStatisticsEnabled(true);

        //enable timestamp in the eventNotifier (log, route and step collectors)
        context.getManagementStrategy().getEventFactory().setTimestampEnabled(true);

        String[] componentNames = {"ftp", "ftps", "sftp", "file", "sql", "scheduler", "timer", "quartz", "smtp", "pop3", "imap", "smtps", "pop3s", "imaps"};
        for (String componentName : componentNames) {
            Component component = context.getComponent(componentName);
            if (component != null) {
                PropertyConfigurer propertyConfigurer = component.getComponentPropertyConfigurer();
                if (propertyConfigurer != null) {
                    propertyConfigurer.configure(context, component, "bridgeErrorHandler", "true", true);
                }
            }
        }
    }

    //loads templates in the template package
    public void setRouteTemplates() throws Exception {

        //load kamelets into Camel Context

        RoutesLoader loader = PluginHelper.getRoutesLoader(context);

        List<String> resourceNames = getKamelets();

        //Set to use the list globally
        CustomKameletCatalog.addAllNames(resourceNames);

        for (String resourceName : resourceNames) {

            if (resourceName.equals("kamelets/resolve-pojo-schema-action.kamelet.yaml")
                    || resourceName.equals("kamelets/djl-image-to-text-action.kamelet.yaml")) {
                continue;
            }

            URL url;
            if (resourceName.startsWith("file:")) {
                url = URI.create(resourceName).toURL();
            } else {
                url = Resources.getResource(resourceName);
            }

            String resourceAsString = Resources.toString(url, StandardCharsets.UTF_8);

            registry.bind(StringUtils.substringBetween(resourceName, "kamelets/", ".kamelet.yaml"), resourceAsString);

            Resource resource = convertKameletToStep(resourceName, resourceAsString);

            try {
                loader.loadRoutes(resource);
            } catch (Exception e) {
                log.warn("could not load: {}. Reason: {}", resourceName, e.getMessage());
            }

        }

    }

    private List<String> getKamelets() throws IOException {

        List<String> kamelets = new ArrayList<>();

        // Add resource paths from classpath (/kamelets under resources)
        List<String> classpathNames;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("kamelets").scan()) {
            classpathNames = scanResult.getAllResources().getPaths();
        }

        if (classpathNames != null && !classpathNames.isEmpty()) {
            kamelets.addAll(classpathNames);
        }

        // Add resource paths from filepath (Kamelets .assimbly/kamelets directory)
        List<String> filepathNames = new ArrayList<>();

        File kameletDir = new File(baseDir + "/kamelets");
        if (!kameletDir.exists()) {
            FileUtils.forceMkdir(kameletDir);
        } else {

            try (Stream<Path> paths = Files.walk(Paths.get(baseDir + "/kamelets"))) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith("kamelet.yaml"))
                        .forEach(path -> filepathNames.add("file:///" + path.toString().replace("\\", "/")));
            } catch (IOException e) {
                log.error("Can't update kamelets in directory: {}", kameletDir, e);
            }

            kamelets.addAll(filepathNames);

        }

        return kamelets;

    }

    private Resource convertKameletToStep(String resourceName, String resourceAsString) {

        String properties = """
                    properties:
                      in:
                          title: Source endpoint
                          description: The Camel uri of the source endpoint.
                          type: string
                          default: kamelet:source
                      out:
                          title: Sink endpoint
                          description: The Camel uri of the sink endpoint.
                          type: string
                          default: kamelet:sink
                      routeId:
                          title: Route ID
                          description: The Camel route ID.
                          type: string
                      routeConfigurationId:
                          title: RouteConfiguration ID
                          description: The Camel routeconfiguration ID.
                          type: string
                          default: 0
                """;

        //replace values
        if (resourceName.contains("action") && !resourceAsString.contains("kamelet:sink")) {
            resourceAsString = resourceAsString + "      - to:\n" +
                    "          uri: \"kamelet:sink\"";
        }

        if (resourceAsString.contains("route:")) {

            resourceAsString = Strings.CS.replaceOnce(resourceAsString, "route:", "route:\n" +
                    "      routeConfigurationId: \"{{routeConfigurationId}}\"");

        }

        resourceAsString = Strings.CS.replaceOnce(resourceAsString, """
                template:
                  from:""", """
                template:
                  route:
                    routeConfigurationId: "{{routeConfigurationId}}"
                  from:""");

        resourceAsString = Strings.CS.replace(resourceAsString, "\"kamelet:source\"", "\"{{in}}\"");
        resourceAsString = Strings.CS.replace(resourceAsString, "\"kamelet:sink\"", "\"{{out}}\"");
        resourceAsString = Strings.CS.replace(resourceAsString, "kamelet:source", "\"{{in}}\"");
        resourceAsString = Strings.CS.replace(resourceAsString, "kamelet:sink", "\"{{out}}\"");
        resourceAsString = Strings.CS.replace(resourceAsString, "    properties:", properties, 1);
        resourceName = StringUtils.substringAfter(resourceName, "kamelets/");

        return ResourceHelper.fromString(resourceName, resourceAsString);

    }

    public String getListOfStepTemplates() throws IOException {

        List<String> kamelets = getKamelets();

        for (int i = 0; i < kamelets.size(); i++) {
            String kamelet = kamelets.get(i);
            if (kamelet.endsWith(".kamelet.yaml")) {
                kamelets.set(i, kamelet.substring(9, kamelet.length() - 13));
            }
        }

        Collections.sort(kamelets);

        JSONArray jsonArray = new JSONArray(kamelets);

        return jsonArray.toString();

    }

    public String getStepTemplate(String mediaType, String stepName) throws Exception {

        String resourceName = "kamelets/" + stepName + ".kamelet.yaml";
        URL url = Resources.getResource(resourceName);
        String resourceAsString = Resources.toString(url, StandardCharsets.UTF_8);

        if (mediaType.contains("xml")) {
            resourceAsString = DocConverter.convertYamlToXml(resourceAsString);
        } else if (mediaType.contains("json")) {
            resourceAsString = DocConverter.convertYamlToJson(resourceAsString);
        }

        return resourceAsString;

    }


    public String addCollectorsConfiguration(String mediaType, String configuration) throws Exception {

        String result = "unconfigured";

        if (mediaType.contains("xml")) {
            configuration = DocConverter.convertXmlToJson(configuration);
        } else if (mediaType.contains("yaml")) {
            configuration = DocConverter.convertYamlToJson(configuration);
        }

        ObjectMapper mapper = new ObjectMapper();
        Collection[] collections = mapper.readValue(configuration, Collection[].class);

        for (Collection collection : collections) {

            EventConfigurer eventConfigurer = new EventConfigurer(collection.getId(), context);

            result = eventConfigurer.add(collection);

            if (!result.equalsIgnoreCase("configured")) {
                break;
            }
        }

        return result;

    }

    public String serialize(String json) {
        Gson gson = new Gson();
        String g = gson.toJson(json);
        return StringEscapeUtils.escapeEcmaScript(g);
    }

    public String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception {

        if (mediaType.contains("xml")) {
            configuration = DocConverter.convertXmlToJson(configuration);
        } else if (mediaType.contains("yaml")) {
            configuration = DocConverter.convertYamlToJson(configuration);
        }

        EventConfigurer eventConfigurer = new EventConfigurer(collectorId, context);

        return eventConfigurer.add(configuration);

    }

    public String removeCollectorConfiguration(String collectorId) {
        EventConfigurer eventConfigurer = new EventConfigurer(collectorId, context);
        return eventConfigurer.remove(collectorId);
    }

    public String convertDefaultYAMLToConfiguration(String fileName, String configuration) {

        StringBuilder xmlRoute = new StringBuilder();
        Yaml yaml = new Yaml();

        if (configuration.startsWith("- from:") || configuration.contains("kind: Integration") || configuration.contains("kind: Kamelet")) {
            if (configuration.startsWith("- from:")) {
                configuration = Strings.CS.replace(configuration, "\n", "\n  ");
            }

            configuration = StringUtils.substringAfter(configuration, "from:");

            configuration = "- route:\n" +
                    "    id: " + fileName + "-1\n" +
                    "    from:" + configuration;
        }

        String[] routes = StringUtils.splitByWholeSeparator(configuration, "- route:\n");

        for (String route : routes) {

            route = "- route:\n" + route;

            List<Map<String, Object>> yamlRoutes = yaml.load(route);

            int index = 0;
            for (Map<String, Object> yamlRoute : yamlRoutes) {

                Map<String, String> routeMap = getRouteMap(yamlRoute);

                String id = routeMap.get("id");
                if (id == null || id.isEmpty()) {
                    id = fileName + "-" + index++;
                }

                //put yaml route into xml route
                xmlRoute.append("<route id=\"").append(id).append("\"><yamldsl><![CDATA[").append(route).append("]]></yamldsl></route>");

            }

        }

        return "<routes id=\"" + fileName + "\" xmlns=\"http://camel.apache.org/schema/spring\">" + xmlRoute + "</routes>";

    }

    private static Map<String, String> getRouteMap(Map<String, Object> yamlRoute) {
        Map<String, String> routeMap = new HashMap<>();
        Object value = yamlRoute.get("route");

        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String keyStr && entry.getValue() instanceof String valueStr) {
                    routeMap.put(keyStr, valueStr);
                }
            }
        }

        return routeMap;
    }

}
