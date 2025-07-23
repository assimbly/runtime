package org.assimbly.integration.impl;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.jms.pool.PooledConnectionFactory;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.api.management.mbean.RouteError;
import org.apache.camel.builder.ThreadPoolProfileBuilder;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.component.jetty12.JettyHttpComponent12;
import org.apache.camel.component.jms.ClassicJmsHeaderFilterStrategy;
import org.apache.camel.component.kamelet.KameletComponent;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.component.seda.SedaComponent;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.springrabbit.SpringRabbitMQComponent;
import org.apache.camel.health.HealthCheck;
import org.apache.camel.health.HealthCheckHelper;
import org.apache.camel.health.HealthCheckRepository;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.engine.DefaultCamelContextNameStrategy;
import org.apache.camel.impl.engine.ExplicitCamelContextNameStrategy;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.*;
import org.apache.camel.support.*;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.TimeUtils;
import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.assimbly.cookies.CookieStore;
import org.assimbly.dil.blocks.beans.*;
import org.assimbly.dil.blocks.beans.enrich.EnrichStrategy;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.blocks.processors.*;
import org.assimbly.dil.event.EventConfigurer;
import org.assimbly.dil.event.domain.Collection;
import org.assimbly.dil.loader.FastFlowLoader;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.loader.FlowLoaderReport;
import org.assimbly.dil.loader.RouteLoader;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.dil.validation.*;
import org.assimbly.dil.validation.beans.FtpSettings;
import org.assimbly.dil.validation.beans.Regex;
import org.assimbly.dil.validation.beans.script.EvaluationRequest;
import org.assimbly.dil.validation.beans.script.EvaluationResponse;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.mail.component.mail.AttachmentAttacher;
import org.assimbly.mail.component.mail.MailComponent;
import org.assimbly.mail.dataformat.mime.multipart.MimeMultipartDataFormat;
import org.assimbly.multipart.processor.MultipartProcessor;
import org.assimbly.util.*;
import org.assimbly.util.error.ValidationErrorMessage;
import org.assimbly.util.file.DirectoryWatcher;
import org.assimbly.util.helper.JsonHelper;
import org.assimbly.util.mail.ExtendedHeaderFilterStrategy;
import org.assimbly.xmltojson.CustomXmlJsonDataFormat;
import org.jasypt.properties.EncryptableProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

import javax.management.JMX;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class CamelIntegration extends BaseIntegration {

	protected static final Logger log = LoggerFactory.getLogger(CamelIntegration.class);

	private final CamelContext context;
	private boolean started;
	private static final String BROKER_HOST = "ASSIMBLY_BROKER_HOST";
	private static final String BROKER_PORT = "ASSIMBLY_BROKER_PORT";
	private static final long STOP_TIMEOUT = 300;
	private ServiceStatus status;
	private String flowStatus;
	private final MetricRegistry metricRegistry = new MetricRegistry();
	private final SimpleRegistry registry = new SimpleRegistry();
	private String flowInfo;
	private RouteController routeController;
	private ManagedCamelContext managed;
	private Properties encryptionProperties;
	private boolean watchDeployDirectoryInitialized;
	private TreeMap<String, String> props;
	private final TreeMap<String, String> confFiles = new TreeMap<>();
	private String loadReport;
	private FlowLoaderReport flowLoaderReport;

	private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private static final String SEP = "/";
	private static final String SECURITY_PATH = "security";
	private static final String TRUSTSTORE_FILE = "truststore.jks";
	private static final String KEYSTORE_FILE = "keystore.jks";
	private static final String KEYSTORE_PWD = "KEYSTORE_PWD";

	private static final String HTTP_MUTUAL_SSL_PROP = "httpMutualSSL";
	private static final String RESOURCE_PROP = "resource";
	private static final String AUTH_PASSWORD_PROP = "authPassword";
	private long timeCreated;
	private Path pathCreated;
	private boolean cacheEnabled = false;

	public CamelIntegration() throws Exception {
		super();
		context = new DefaultCamelContext(registry);
	}

	public CamelIntegration(boolean useDefaultSettings) throws Exception {
		super();
		context = new DefaultCamelContext(registry);
		init(useDefaultSettings);
	}

	public final void init(boolean useDefaultSettings) throws Exception {

		//set the name of the runtime
		context.setNameStrategy(new ExplicitCamelContextNameStrategy("assimbly"));
		context.setManagementName("assimbly");

		//setting tracing standby to true, so it can be enabled during runtime
		context.setTracingStandby(true);

		//load settings into a separate thread
		if(useDefaultSettings){
			setDefaultSettings();
		}

		//set management tasks
		routeController = context.getRouteController();
		managed = context.getCamelContextExtension().getContextPlugin(ManagedCamelContext.class);

	}

	public void setDefaultSettings() throws Exception {

		setRouteTemplates();

		setGlobalOptions();

		setDefaultBlocks();

		setDefaultThreadProfile(5,50,5000);

		setThreadProfile("wiretapProfile", 0,10,2500);

		setCertificateStore(true);

		setDebugging(false);

		setSuppressLoggingOnTimeout(true);

		setStreamCaching(true);

		setMetrics(true);

		setHistoryMetrics(true);

		setHealthChecks(true);

		setCache(false);

	}

	public void setTracing(boolean tracing, String traceType) {

		if(traceType.equalsIgnoreCase("backlog")){
			context.setBacklogTracing(true);
		}else if (traceType.equalsIgnoreCase("default")) {
			Tracer tracer = context.getTracer();
			tracer.setEnabled(tracing);
		}

	}

	public void setHealthChecks(boolean enable) {

		HealthCheckRepository routesHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "routes");
		if(routesHealthCheckRepository!=null) {
			routesHealthCheckRepository.setEnabled(enable);
		}
		HealthCheckRepository consumersHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "consumers");
		if(consumersHealthCheckRepository!=null) {
			consumersHealthCheckRepository.setEnabled(enable);
		}

		HealthCheckRepository producersHealthCheckRepository = HealthCheckHelper.getHealthCheckRepository(context, "producers");
		if(producersHealthCheckRepository!=null) {
			producersHealthCheckRepository.setEnabled(enable);
		}

	}

	public void setCache(boolean cache) throws Exception {

		if(cache){
			cacheEnabled = true;

			initFlowDB();
			startAllFlows();
		}else{
			cacheEnabled = false;
			initFlowMap();
		}

	}

	public void setDebugging(boolean debugging) {
		context.setDebugging(debugging);
	}

	public void setStreamCaching(boolean streamCaching) {
		context.setStreamCaching(streamCaching);
		context.getStreamCachingStrategy().setBufferSize(32 * 1024);

	}

	public void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout) {
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(suppressLoggingOnTimeout);
		context.getShutdownStrategy().setTimeUnit(TimeUnit.MILLISECONDS);
	}

	public void setCertificateStore(boolean certificateStore) throws Exception {
		if(certificateStore){
			setSSLContext();
		}
	}

	public void setMetrics(boolean metrics){
		if(metrics){
			context.addRoutePolicyFactory(new MetricsRoutePolicyFactory());
		}
	}

	public void setHistoryMetrics(boolean setHistoryMetrics){
		//set history metrics
		MetricsMessageHistoryFactory factory = new MetricsMessageHistoryFactory();
		factory.setPrettyPrint(true);
		factory.setMetricsRegistry(metricRegistry);
		context.setMessageHistoryFactory(factory);
		context.setSourceLocationEnabled(true);
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
		jettyHttpComponent12.setBridgeErrorHandler(true);
		context.addComponent("jetty-nossl", jettyHttpComponent12);
		context.addComponent("jetty", jettyHttpComponent12);
		context.addComponent("rabbitmq", new SpringRabbitMQComponent());

		// Add bean/processors and other custom classes to the registry
		registry.bind("AggregateStrategy", new AggregateStrategy());
		registry.bind("AttachmentAttacher",new AttachmentAttacher());
		registry.bind("CurrentAggregateStrategy", new AggregateStrategy());
		registry.bind("CurrentEnrichStrategy", new EnrichStrategy());
		registry.bind("CustomHttpHeaderFilterStrategy",new CustomHttpHeaderFilterStrategy());
		registry.bind("customHttpBinding", new CustomHttpBinding());
		registry.bind("ExtendedHeaderFilterStrategy", new ExtendedHeaderFilterStrategy());
		registry.bind("flowCookieStore", new CookieStore());
		registry.bind("InputStreamToStringProcessor", new InputStreamToStringProcessor());
		registry.bind("JsonAggregateStrategy", new JsonAggregateStrategy());
		registry.bind("ManageFlowProcessor", new ManageFlowProcessor());
		registry.bind("multipartProcessor",new MultipartProcessor());
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

	public void setGlobalOptions(){

		//enable breadcrumb for tracing
		context.setUseBreadcrumb(true);

		//enable performance stats
		context.getManagementStrategy().getManagementAgent().setLoadStatisticsEnabled(true);

		//enable timestamp in the eventNotifier (log, route and step collectors)
		context.getManagementStrategy().getEventFactory().setTimestampEnabled(true);

		String[] componentNames = {"ftp", "ftps", "sftp", "file", "sql", "scheduler", "timer","quartz","smtp","pop3","imap","smtps","pop3s","imaps"};
		for (String componentName : componentNames) {
			Component component = context.getComponent(componentName);
			if(component!=null) {
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
		CustomKameletCatalog.names.addAll(resourceNames);

		for(String resourceName: resourceNames){

			if(resourceName.equals("kamelets/resolve-pojo-schema-action.kamelet.yaml")
					|| resourceName.equals("kamelets/djl-image-to-text-action.kamelet.yaml")){
				continue;
			}

			URL url;
			if(resourceName.startsWith("file:")){
				url= URI.create(resourceName).toURL();
			}else{
				url = Resources.getResource(resourceName);
			}

			String resourceAsString = Resources.toString(url, StandardCharsets.UTF_8);

			registry.bind(StringUtils.substringBetween(resourceName, "kamelets/",".kamelet.yaml"),resourceAsString);

			Resource resource = convertKameletToStep(resourceName, resourceAsString);

			try{
				loader.loadRoutes(resource);
			}catch (Exception e){
				log.warn("could not load: " + resourceName + ". Reason: " + e.getMessage());
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

		if(classpathNames != null && !classpathNames.isEmpty()){
			kamelets.addAll(classpathNames);
		}

		// Add resource paths from filepath (Kamelets .assimbly/kamelets directory)
		List<String> filepathNames = new ArrayList<>();

		File kameletDir = new File(baseDir + "/kamelets");
		if(!kameletDir.exists()){
			FileUtils.forceMkdir(kameletDir);
		}else{

			try (Stream<Path> paths = Files.walk(Paths.get(baseDir + "/kamelets"))) {
				paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith("kamelet.yaml"))
						.forEach(path -> filepathNames.add("file:///" + path.toString().replace("\\", "/")));
			} catch (IOException e) {
				// Handle exception appropriately
				e.printStackTrace();
			}

			kamelets.addAll(filepathNames);

		}

		return kamelets;

	}

	private Resource convertKameletToStep(String resourceName, String resourceAsString){

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
		if(resourceName.contains("action") && !resourceAsString.contains("kamelet:sink") ){
			resourceAsString = resourceAsString + "      - to:\n" +
					"          uri: \"kamelet:sink\"";
		}

		if(resourceAsString.contains("route:")){

			resourceAsString = StringUtils.replaceOnce(resourceAsString,"route:","route:\n" +
					"      routeConfigurationId: \"{{routeConfigurationId}}\"");

		}

		resourceAsString = StringUtils.replaceOnce(resourceAsString, """
  template:
    from:""", """
  template:
    route:
      routeConfigurationId: "{{routeConfigurationId}}"
    from:""");

		resourceAsString = StringUtils.replace(resourceAsString,"\"kamelet:source\"", "\"{{in}}\"");
		resourceAsString = StringUtils.replace(resourceAsString,"\"kamelet:sink\"", "\"{{out}}\"");
		resourceAsString = StringUtils.replace(resourceAsString,"kamelet:source", "\"{{in}}\"");
		resourceAsString = StringUtils.replace(resourceAsString,"kamelet:sink", "\"{{out}}\"");
		resourceAsString = StringUtils.replace(resourceAsString,"    properties:", properties,1);
		resourceName= StringUtils.substringAfter(resourceName, "kamelets/");
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
		URL	url = Resources.getResource(resourceName);
		String resourceAsString = Resources.toString(url, StandardCharsets.UTF_8);

		if(mediaType.contains("xml")){
			resourceAsString = DocConverter.convertYamlToXml(resourceAsString);
		}else if(mediaType.contains("json")){
			resourceAsString = DocConverter.convertYamlToJson(resourceAsString);
		}

		return resourceAsString;

	}

	public String addCollectorsConfiguration(String mediaType, String configuration) throws Exception{

		String result = "unconfigured";

		if(mediaType.contains("xml")){
			configuration = DocConverter.convertXmlToJson(configuration);
		}else if(mediaType.contains("yaml")){
			configuration = DocConverter.convertYamlToJson(configuration);
		}

		ObjectMapper mapper = new ObjectMapper();
		Collection[] collections = mapper.readValue(configuration, Collection[].class);

		for(Collection collection: collections){

			EventConfigurer eventConfigurer = new EventConfigurer(collection.getId(), context);

			result = eventConfigurer.add(collection);

			if(!result.equalsIgnoreCase("configured")){
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

	public String addCollectorConfiguration(String collectorId, String mediaType, String configuration) throws Exception{

		if(mediaType.contains("xml")){
			configuration = DocConverter.convertXmlToJson(configuration);
		}else if(mediaType.contains("yaml")){
			configuration = DocConverter.convertYamlToJson(configuration);
		}

		EventConfigurer eventConfigurer = new EventConfigurer(collectorId, context);

		return eventConfigurer.add(configuration);

	}

	public String removeCollectorConfiguration(String collectorId) throws Exception{

		EventConfigurer eventConfigurer = new EventConfigurer(collectorId, context);

		return eventConfigurer.remove(collectorId);

	}


	public void setDeployDirectory(boolean deployOnStart, boolean deployOnEvent) throws Exception {

		Path path = Paths.get(baseDir + "/deploy");

		//Create the deploy directory if not exist
		Files.createDirectories(path);

		if(deployOnStart && deployOnEvent){
			checkDeployDirectory(path);
			watchDeployDirectory(path);
		}else if (deployOnStart){
			//Check & Start files found in the deploy directory
			checkDeployDirectory(path);
		}else if(deployOnEvent){
			//Monitor files in the deploy directory after start
			watchDeployDirectory(path);
		}

	}

	private void checkDeployDirectory(Path path) {
		try (Stream<Path> paths = Files.walk(path)) {
			paths.filter(fPath -> fPath.toString().endsWith(".xml")
							|| fPath.toString().endsWith(".json")
							|| fPath.toString().endsWith(".yaml"))
					.forEach(fPath -> {
						try {
							fileInstall(fPath);
						} catch (Exception e) {
							log.error("Check deploy directory " + path + " failed", e);
						}
					});
		} catch (IOException e) {
			log.error("Error while walking the file tree for path: " + path, e);
		}

	}

	private void watchDeployDirectory(Path path) throws Exception {

		if(watchDeployDirectoryInitialized){
			return;
		}

		log.info("Deploy folder | Init watching for changes: {}", path);
		watchDeployDirectoryInitialized = true;

		DirectoryWatcher watcher = new DirectoryWatcher.Builder()
				.addDirectories(path)
				.setPreExistingAsCreated(true)
				.build((event, path1) -> {

					switch (event) {
						case ENTRY_CREATE:
							createDeployDirectory(path1);
							break;
						case ENTRY_MODIFY:
							modifyDeployDirectory(path1);
							break;
						case ENTRY_DELETE:
							deleteDeployDirectory(path1);
							break;
					}
				});

		watcher.start();

	}

	private void createDeployDirectory(Path path){
		log.info("Deploy folder | File created: " + path);

		try {
			timeCreated = System.currentTimeMillis();
			long lastModified = FileUtils.lastModifiedFileTime(path.toFile()).toMillis();
			long diff = timeCreated - lastModified;

			log.info("time modified: " + diff);

			if(diff < 5000){
				fileInstall(path);
			}

			pathCreated = path;

		} catch (Exception e) {
			log.error("FileInstall for created {} failed", path.toString(), e);
		}
	}

	private void modifyDeployDirectory(Path path){
		long timeModified = System.currentTimeMillis();
		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);

		long diff = timeModified - timeCreated;

		if(diff > 5000){
			log.info("Deploy folder | File modified: " + path);
			try {
				if (path.equals(pathCreated) && confFiles.get(fileName)!= null){
					fileReinstall(path);
				}else if (confFiles.get(fileName)!= null){
					fileReinstall(path);
				}else{
					fileInstall(path);
				}
			} catch (Exception e) {
				log.error("FileInstall for modified {} failed", path, e);
			}
		}
	}

	private void deleteDeployDirectory(Path path){
        log.info("Deploy folder | File deleted: {}", path);
		try {
			fileUninstall(path);
		} catch (Exception e) {
            log.error("FileUnInstall for deleted {} failed", path.toString(), e);
		}
	}

	public void fileInstall(Path path) throws Exception {

		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);
		String configuration = FileUtils.readFileToString(new File(pathAsString), StandardCharsets.UTF_8);

		confFiles.put(fileName,configuration);

		if(mediaType.contains("json")){
			configuration = DocConverter.convertJsonToXml(configuration);
			mediaType = "xml";

		}else if(mediaType.contains("yaml")){
			if(configuration.contains("dil")){
				configuration = DocConverter.convertYamlToXml(configuration);
			}else{
				configuration = convertDefaultYAMLToConfiguration(fileName, configuration);
			}

			mediaType = "xml";
		}

		String flowId = setFlowId(fileName, configuration);

		if(flowId!=null){
			log.info("File install flowid=" + flowId + " | path=" + pathAsString);
			loadReport = installFlow(flowId, STOP_TIMEOUT, mediaType, configuration);

			if(loadReport.contains("\"event\": \"error\"")||loadReport.contains("\"event\": \"failed\"") || loadReport.contains("message\": \"Failed to load flow\"")){
				log.error(loadReport);
			}else{
				log.info(loadReport);
			}
		}else{
			log.error("File install for " + pathAsString + " failed. Invalid configuration file.");
		}

	}

	public String convertDefaultYAMLToConfiguration(String fileName, String configuration) {

		StringBuilder xmlRoute = new StringBuilder();
		Yaml yaml = new Yaml();

		if (configuration.startsWith("- from:") || configuration.contains("kind: Integration") || configuration.contains("kind: Kamelet")) {
			if (configuration.startsWith("- from:")){
				configuration= StringUtils.replace(configuration,"\n","\n  ");
			}

			configuration = StringUtils.substringAfter(configuration, "from:");

			configuration = "- route:\n" +
					"    id: " + fileName + "-1\n" +
					"    from:" + configuration;
		}

		String[] routes = StringUtils.splitByWholeSeparator(configuration,"- route:\n");

		for(String route: routes){

			route = "- route:\n" + route;

			List<Map<String,Object>> yamlRoutes = yaml.load(route);

			int index = 0;
			for(Map<String,Object> yamlRoute: yamlRoutes){

				Map<String,String> routeMap = (Map<String, String>) yamlRoute.get("route");

				String id = routeMap.get("id");
				if(id == null || id.isEmpty()){
					id = fileName + "-" + index++;
				}

				//put yaml route into xml route
				xmlRoute.append("<route id=\"").append(id).append("\"><yamldsl><![CDATA[").append(route).append("]]></yamldsl></route>");

			}

		}

		return "<routes id=\"" + fileName + "\" xmlns=\"http://camel.apache.org/schema/spring\">" + xmlRoute + "</routes>";

	}

	public void fileReinstall(Path path) throws Exception {

		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);
		String oldConfiguration = confFiles.get(fileName);

		if(mediaType.contains("json")){
			oldConfiguration = DocConverter.convertJsonToXml(oldConfiguration);
		}else if(mediaType.contains("yaml")){
			oldConfiguration = DocConverter.convertYamlToXml(oldConfiguration);
		}

		String flowId = setFlowId(fileName, oldConfiguration);

		stopFlow(flowId, STOP_TIMEOUT);

		fileInstall(path);

	}


	public void fileUninstall(Path path) throws Exception {

		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);
		String configuration = confFiles.get(fileName);

		confFiles.remove(fileName);

		if(mediaType.contains("json")){
			configuration = DocConverter.convertJsonToXml(configuration);
		}else if(mediaType.contains("yaml")){
			configuration = DocConverter.convertYamlToXml(configuration);
		}

		String flowId = setFlowId(fileName, configuration);

		if(flowId!=null){
			log.info("File uninstall flowid=" + flowId + " | path=" + pathAsString);
			stopFlow(flowId, STOP_TIMEOUT);
		}else{
			log.error("File uninstall for " + pathAsString + " failed. FlowId is null.");
		}

	}

	public String setFlowId(String filename, String configuration) throws Exception {

		String configurationUTF8 = new String(configuration.getBytes(StandardCharsets.UTF_8),StandardCharsets.UTF_8);

		String flowId;
		if(IntegrationUtil.isXML(configurationUTF8)) {
			flowId = getFlowId(filename, configurationUTF8);
		}else{
			flowId = filename;
		}

		return flowId;

	}

	public String getFlowId(String filename, String configurationUTF8) throws Exception {

		String flowId = "";
		Document doc = DocConverter.convertStringToDoc(configurationUTF8);
		XPath xPath = XPathFactory.newInstance().newXPath();

		String root = doc.getDocumentElement().getTagName();

		if (root.equals("dil") || root.equals("integrations") || root.equals("flows") || root.equals("flow")) {
			flowId = xPath.evaluate("//flows/flow[id='" + filename + "']/id", doc);
			if (flowId == null || flowId.isEmpty()) {
				flowId = xPath.evaluate("//flow[1]/id", doc);
			}
		} else if (root.equals("camelContext")) {
			flowId = xPath.evaluate("/camelContext/@id", doc);
		} else if (root.equals("routes")) {
			flowId = xPath.evaluate("/routes/@id", doc);
		} else {
			log.error("Unknown configuration. Either a DIL file (starting with a <dil> element) or Camel file (starting with <routes> element) is expected");
		}

		return flowId;

	}

	//Manage integration

	public void start() throws Exception {

		// start Camel context
		if(!started){

			context.start();
			started = true;

			log.info("Runtime started");

		}

	}

	public void stop() throws Exception {
		super.getFlowConfigurations().clear();
		if (context != null){
			for (Route route : context.getRoutes()) {
				routeController.stopRoute(route.getId());
				context.removeRoute(route.getId());
			}

			context.stop();
			started = false;
			log.info("Integration stopped");

		}
	}

	public boolean isStarted() {
		return started;
	}



	//Manage flows

	public String loadFlow(TreeMap<String, String> properties)  {

		this.props = properties;

		try{

			if(properties.containsKey("frontend") && properties.get("frontend").equals("dovetail")) {

				// add custom connections if needed
				addCustomActiveMQConnection();

				// add custom connections if needed
				addCustomRabbitMQConnection(new TreeMap<>(properties));

			}

			//create connections & install dependencies if needed
			createConnections(properties);

			Map<String, String> mutualSSLInfoMap = getMutualSSLInfoFromProps(properties);
			if(mutualSSLInfoMap!=null && !mutualSSLInfoMap.isEmpty()) {
				// add certificate on keystore
				addCertificateFromUrl(mutualSSLInfoMap.get(RESOURCE_PROP), mutualSSLInfoMap.get(AUTH_PASSWORD_PROP));
			}

			FlowLoader flow = new FlowLoader(properties, flowLoaderReport);

			flow.addRoutesToCamelContext(context);

			loadReport = flow.getReport();

			if(!flow.isFlowLoaded()){
				return "error";
			}

			return "started";

		}catch (Exception e){
			log.error("add flow failed: ", e);
			return "error reason: " + e.getMessage();
		}

	}

	// add certificate from url on the keystore
	private void addCertificateFromUrl(String url, String authPassword) {
		try {
			byte[] fileContent;

			URL urlObject = URI.create(url).toURL();
			try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(IOUtils.toByteArray(urlObject))) {
				fileContent = IOUtils.toByteArray(byteArrayInputStream);
			}
			String encodedResourceContent = Base64.getEncoder().encodeToString(fileContent);

			CertificatesUtil util = new CertificatesUtil();
			String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + KEYSTORE_FILE;
			util.importP12Certificate(keystorePath, getKeystorePassword(), encodedResourceContent, authPassword);

		} catch (Exception e) {
			log.error("Error to add certificate", e);
		}
	}

	// get mutual ssl info from props
	private Map<String, String> getMutualSSLInfoFromProps(TreeMap<String, String> props) {
		for (Map.Entry<String, String> entry : props.entrySet()) {
			String xml = entry.getValue();
			if(!xml.startsWith("<")) {
				// skip prop if it's not an xml
				continue;
			}
			return getMutualSSLInfoFromXml(xml);
		}
		return Collections.emptyMap();
	}

	// get mutual ssl info from xml
	private HashMap<String, String> getMutualSSLInfoFromXml(String xml) {
		HashMap<String, String> map = new HashMap<>();

		String httpMutualSSL = getPropertyValue(xml, HTTP_MUTUAL_SSL_PROP);
		if(httpMutualSSL!=null && httpMutualSSL.equals("true")) {
			String authPassword = getPropertyValue(xml, AUTH_PASSWORD_PROP);
			String resource = getPropertyValue(xml, RESOURCE_PROP);

			map.put(AUTH_PASSWORD_PROP, authPassword);
			map.put(RESOURCE_PROP, resource);

		}

		return map;
	}

	// get property value by property name
	private String getPropertyValue(String xml, String propName) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		XPathFactory xpathFactory = XPathFactory.newInstance();
		XPath xpath = xpathFactory.newXPath();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xml)));
			String expression = String.format("//setProperty[@name='%s']/constant/text()", propName);
			return xpath.evaluate(expression, doc);
		} catch (Exception e) {
			return null;
		}

	}

	private void addCustomActiveMQConnection() {

		try {

			Component activemqComp = this.context.getComponent("activemq");

			if (activemqComp == null) {

				String brokerHost = getEnvironmentVariable(BROKER_HOST,"localhost");
				int brokerPort = getEnvironmentVariableAsInteger(BROKER_PORT,61616);
				String activemqUrl = String.format("tcp://%s:%s", brokerHost, brokerPort);

				log.info("Adding custom ActiveMQ connection. URL: " + activemqUrl);

				this.context.addComponent("activemq", getJmsComponent(activemqUrl));

			}

		} catch (Exception e) {
			log.error("Error to add custom ActiveMQ connection", e);
		}

	}

	private void addCustomRabbitMQConnection(TreeMap<String, String> properties) {

		for (Map.Entry<String, String> entry : properties.entrySet()) {
			if(entry.getKey().startsWith("route") && entry.getValue().contains("rabbitmqConnectionFactory")) {

				String connection = StringUtils.substringBetween(entry.getValue(),"<rabbitmqConnectionFactory>","</rabbitmqConnectionFactory>");
				String rabbitMQElement = "<rabbitmqConnectionFactory>" + connection + "</rabbitmqConnectionFactory>";

				if(connection == null) {
					connection = StringUtils.substringBetween(entry.getValue(),"<rabbitmqConnectionFactory xmlns=\"http://camel.apache.org/schema/blueprint\">","</rabbitmqConnectionFactory>");
					rabbitMQElement = "<rabbitmqConnectionFactory xmlns=\"http://camel.apache.org/schema/blueprint\">" + connection + "</rabbitmqConnectionFactory>";
				}

				Map<String, String> connectionMap = stringToMap(connection);
				String connectionId = connectionMap.get("host") + "-" + connectionMap.get("port") + "-" + connectionMap.get("username");

				props.put("sink.1.connection.id",connectionId);
				props.put("connection." + connectionId + ".type","rabbitmq");
				props.put("connection." + connectionId + ".host",connectionMap.get("host"));
				props.put("connection." + connectionId + ".vhost",connectionMap.get("vhost"));
				props.put("connection." + connectionId + ".port",connectionMap.get("port"));
				props.put("connection." + connectionId + ".username",connectionMap.get("username"));
				props.put("connection." + connectionId + ".password",connectionMap.get("password"));
				props.put(entry.getKey(),StringUtils.replace(entry.getValue(),rabbitMQElement,""));

			}
		}

	}

	private Map<String, String> stringToMap(String input){
		Map<String, String> map = new LinkedHashMap<>();
		String[] pairs = StringUtils.split(input, ',');

		for (String pair : pairs) {
			if (StringUtils.contains(pair, '=')) {
				String key = StringUtils.substringBefore(pair, "=");
				String value = StringUtils.substringAfter(pair, "=");
				if(value.startsWith("RAW")){
					value = StringUtils.substringBetween(value,"RAW(",")");
				}
				map.put(key, value);
			}
		}

		return map;

	}



	private static SjmsComponent getJmsComponent(String activemqUrl) {

		int maxConnections = getEnvironmentVariableAsInteger("AMQ_MAXIMUM_CONNECTIONS",500);
		int idleTimeout = getEnvironmentVariableAsInteger("AMQ_IDLE_TIMEOUT",5000);

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

	public void createConnections(TreeMap<String, String> properties) throws Exception {

		for (Map.Entry<String, String> entry : properties.entrySet()){

			String key = entry.getKey();

			if (key.endsWith("connection.id")){
				setConnection(properties, key);
			}

			if (key.equals("flow.dependencies") && properties.get(key) != null){

				String[] schemes = StringUtils.split(properties.get(key), ",");

				for (String scheme : schemes) {
					createConnection(scheme);
				}
			}
		}

	}

	private void createConnection(String scheme) throws Exception {
		if(!scheme.equals("null") && context.getComponent(scheme.toLowerCase()) == null && !DependencyUtil.CompiledDependency.hasCompiledDependency(scheme.toLowerCase())) {

			log.warn("Component {} is not supported by Assimbly. Try to resolve dependency dynamically.", scheme);

			if(INetUtil.isHostAvailable("repo1.maven.org")){
				log.info(resolveDependency(scheme));
			}else{
				log.error("Failed to resolve {}. No available internet is found. Cannot reach http://repo1.maven.org/maven2/", scheme);
			}

		}
	}

	public boolean removeFlow(String id) throws Exception {

		boolean removed = false;
		List<Route> routes = getRoutesByFlowId(id);

		if(routes!=null && !routes.isEmpty()){
			for(Route route: routes){
				route.getId();
				context.removeRoute(id);
				removed = true;
			}
		}

		return removed;

	}

	public boolean hasFlow(String id) {

		boolean routeFound = false;

		if (context != null){
			for (Route route : context.getRoutes()) {
				if (route.getId().startsWith(id)) {
					routeFound = true;
				}
			}
		}
		return routeFound;
	}

	public String startAllFlows() throws Exception {
		log.info("Starting all flows");

		flowsMap.forEach((flowId, flowProps) -> {
			try {
				flowLoaderReport = new FlowLoaderReport(flowId, flowId);
				loadFlow(flowProps);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return "started";
	}

	public String restartAllFlows() throws Exception {
		log.info("Restarting all flows");

		flowsMap.forEach((flowId, flowProps) -> {
			try {
				flowLoaderReport = new FlowLoaderReport(flowId, flowId);
				loadFlow(props);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return "restarted";
	}

	public String pauseAllFlows() throws Exception {
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

	public String resumeAllFlows() throws Exception {
		log.info("Resume all flows");

		flowsMap.forEach((flowId, flowProps) -> {
			try {
				resumeFlow(flowId);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return "started";
	}

	public String stopAllFlows() throws Exception {
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

	public String installFlow(String flowId, long timeout, String mediaType, String configuration) {

		try {
			super.setFlowConfiguration(flowId, mediaType, configuration);
		}catch (Exception e){
			log.error("Flow configuration failed for flowId: {} and mediaType: {}", flowId, mediaType, e);
			initFlowActionReport(flowId);
			finishFlowActionReport(flowId, "error",e.getMessage(),"error");
			return loadReport;
		}

		return startFlow(flowId, timeout);

	}

	public String fastInstallFlow(String flowId, String configuration) throws Exception {

		flowLoaderReport = new FlowLoaderReport(flowId, flowId);

		if(hasFlow(flowId)) {
			stopFlow(flowId, 250, false);
		}

		TreeMap<String, String> properties = new XMLFileConfiguration().getFlowConfigurationMinimal(flowId, configuration);

		flowsMap.put(flowId, properties);
		if(cacheEnabled) {
			db.commit();
		}

		return loadingFlow(flowId, properties);

	}

	private String loadingFlow(String flowId, TreeMap<String, String> properties) throws Exception {

		createConnections(properties);

		FastFlowLoader flow = new FastFlowLoader(properties, flowLoaderReport, flowId);

		flow.addRoutesToCamelContext(context);

		if(flow.isFlowLoaded()){
			finishFlowActionReport(flowId, "start","Started flow successfully","info");
		}else{
			stopFlow(flowId, 250, false);
			finishFlowActionReport(flowId, "error","error","error");
		}

		return flow.getReport();
	}


	public String uninstallFlow(String flowId, long timeout) throws Exception {
		return stopFlow(flowId, timeout);
	}

	public String fileInstallFlow(String flowId, String configuration) throws Exception {

		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.writeStringToFile(flowFile, configuration, Charset.defaultCharset());
			return "saved";
		} catch (Exception e) {
            log.error("FileInstall flow {} failed", flowId, e);
			return "Fail to save flow " + flowId + " Error: " + e.getMessage();
		}

	}

	public String fileUninstallFlow(String flowId) throws Exception {

		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.deleteQuietly(flowFile);
			return "deleted";
		} catch (Exception e) {
            log.error("FileUninstall flow {} failed", flowId, e);
			return "failed to delete flow " + e.getMessage();
		}

	}

	public String installRoute(String routeId, String route) throws Exception {

		initFlowActionReport(routeId);

		if(!route.startsWith("<route")){
			route = new XMLFileConfiguration().getRouteConfiguration(route);
		}

		try{

			RouteLoader routeLoader = new RouteLoader(routeId,route,flowLoaderReport);

			routeLoader.addRoutesToCamelContext(context);

			loadReport = routeLoader.getReport();

			finishFlowActionReport(routeId, "start","Started flow successfully","info");
		}catch(Exception e){
			finishFlowActionReport(routeId, "error","Failed starting flow",e.getMessage());
		}

		return loadReport;

	}

	public String startFlow(String flowId, long timeout) {

		initFlowActionReport(flowId);

		if(hasFlow(flowId)) {
			stopFlow(flowId, timeout, false);
		}

		try {

			TreeMap<String, String> flowProperties = getProperties(flowId);
			String result = loadFlow(flowProperties);

			if (result.equals("started")){
				finishFlowActionReport(flowId, "start","Started flow successfully","info");
			}else {
				stopFlow(flowId, timeout, false);
				finishFlowActionReport(flowId, "error",result,"error");
			}

		}catch (Exception e) {
			stopFlow(flowId, STOP_TIMEOUT, false);
			finishFlowActionReport(flowId, "error","Start flow failed | error=" + e.getMessage(),"error");
			log.error("Start flow failed. | flowid={}", flowId, e);
		}

		return loadReport;

	}

	private TreeMap<String, String> getProperties(String flowId) throws Exception {
		return super.getFlowConfigurations().stream()
				.filter(properties -> flowId.equals(properties.get("id")))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Flow not found"));
	}

	public String restartFlow(String id, long timeout) {

		try {
			startFlow(id, timeout);
		}catch (Exception e) {
			log.error("Restart flow failed. | flowid=" + id,e);
			finishFlowActionReport(id, "error", e.getMessage(),"error");
		}

		return loadReport;

	}

	public String stopFlow(String flowid, long timeout) {
		return stopFlow(flowid, timeout, true);
	}

	public String stopFlow(String flowid, long timeout, boolean enableReport) {

		if(enableReport) {
			initFlowActionReport(flowid);
		}

		try {
			// gracefully shutdown routes using startup order
			List<RouteStartupOrder> routeStartupOrders = getRoutesStartupOrderByFlowId(flowid);
			context.getShutdownStrategy().shutdown(context, routeStartupOrders, timeout, TimeUnit.MILLISECONDS);
			for(RouteStartupOrder routeStartupOrder : routeStartupOrders){
				context.removeRoute(routeStartupOrder.getRoute().getId());
			}

			// remove leftover routes
			List<String> leftoverRoutes = getAllRoutesByFlowId(flowid);
			if (!leftoverRoutes.isEmpty()) {
				for (String routeId : leftoverRoutes) {
					removeRoute(routeId);
				}
			}

			if(enableReport) {
				finishFlowActionReport(flowid, "stop", "Stopped flow successfully", "info");
			}

		}catch (Exception e) {
			if(enableReport) {
				finishFlowActionReport(flowid, "error", "Stop flow failed | error=" + e.getMessage(), "error");
			}
			log.error("Stop flow failed. | flowid=" + flowid,e);
		}

		return loadReport;

	}

	private void removeRoute(String routeId){
		try {
			if (context.getRoute(routeId) != null) {
				context.getRouteController().stopRoute(routeId);
				context.removeRoute(routeId);
			}
		} catch (Exception e) {
			log.error("Error removing route: " + routeId + " Error message: " + e.getMessage());
		}
	}


	public String pauseFlow(String flowid) {

		initFlowActionReport(flowid);

		try {

			if(hasFlow(flowid)) {

				List<Route> routeList = getRoutesByFlowId(flowid);
				status = routeController.getRouteStatus(routeList.get(0).getId());

				for(Route route : routeList){
					if(!routeController.getRouteStatus(route.getId()).isSuspendable()){
						finishFlowActionReport(flowid, "error","Flow isn't suspendable (Step " + route.getId() + ")","error");
						return loadReport;
					}
				}

				for(Route route : routeList){
					String routeId = route.getId();
					routeController.suspendRoute(routeId);
				}
				finishFlowActionReport(flowid, "pause","Paused flow successfully","info");
			}else {
				String errorMessage = "Configuration is not set (use setConfiguration or setFlowConfiguration)";
				finishFlowActionReport(flowid, "error",errorMessage,"error");
			}
		}catch (Exception e) {
			log.error("Pause flow failed. | flowid=" + flowid,e);
			stopFlow(flowid, STOP_TIMEOUT); //Stop flow if one of the routes cannot be paused.
			finishFlowActionReport(flowid, "error",e.getMessage(),"error");
		}

		return loadReport;

	}

	public String resumeFlow(String flowid) throws Exception {

		initFlowActionReport(flowid);

		try {

			if(hasFlow(flowid)) {

				List<Route> routeList = getRoutesByFlowId(flowid);
				for(Route route : routeList){
					String routeId = route.getId();
					status = routeController.getRouteStatus(routeId);

					if(status.isSuspended()){
						routeController.resumeRoute(routeId);
						log.info("Resumed flow  | flowid=" + flowid + " | stepid=" + routeId);
					}else if (status.isStopped()){
						log.info("Starting route as route " + flowid + " is currently stopped (not suspended)");
						startFlow(routeId, STOP_TIMEOUT);
					}

				}
				finishFlowActionReport(flowid, "resume","Resumed flow successfully","info");
			}else {
				String errorMessage = "Configuration is not set (use setConfiguration or setFlowConfiguration)";
				finishFlowActionReport(flowid, "error",errorMessage,"error");
			}

		}catch (Exception e) {
			log.error("Resume flow " + flowid + " failed.",e);
			finishFlowActionReport(flowid, "error",e.getMessage(),"error");
		}

		return loadReport;

	}

	private void initFlowActionReport(String flowid) {
		flowLoaderReport = new FlowLoaderReport(flowid, flowid);
	}

	private void finishFlowActionReport(String flowid, String event, String message, String messageType) {

		String eventCapitalized = StringUtils.capitalize(event);

		//logs event to
		if(messageType.equalsIgnoreCase("error")){
			log.error(eventCapitalized + " flow " + flowid + " failed | flowid=" + flowid, message);
		}else if(messageType.equalsIgnoreCase("warning"))
			log.warn(eventCapitalized + " flow" + flowid + " failed | flowid=" + flowid, message);
		else{
			log.info(message + " | flowid=" + flowid);
		}

		TreeMap<String, String> flowProps;
		try {
			flowProps = getFlowConfiguration(flowid);
			String version = flowProps.get("flow.version");

			if(version==null){
				version = "0";
			}

			flowLoaderReport.finishReport(event,version,message);

		} catch (Exception e) {
			flowLoaderReport.finishReport(event,"",message);
		}
		loadReport = flowLoaderReport.getReport();
	}

	public boolean isFlowStarted(String flowid) {

		if(hasFlow(flowid)) {
			ServiceStatus serviceStatus = null;
			List<Route> routes = getRoutesByFlowId(flowid);

			for(Route route : routes){
				serviceStatus = routeController.getRouteStatus(route.getId());
				if(!serviceStatus.isStarted()){
					return false;
				}
			}
			return serviceStatus != null;
		}else {
			return false;
		}

	}

	public String getFlowInfo(String id, String mediaType) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject flow = new JSONObject();

		TreeMap<String, String> properties = super.getFlowConfiguration(id);

		if(properties != null){
			flow.put("id",properties.get("id"));
			flow.put("name",properties.get("flow.name"));
			flow.put("version",properties.get("flow.version"));
			flow.put("environment",properties.get("environment"));
			flow.put("isRunning",isFlowStarted(id));
			flow.put("status",getFlowStatus(id));
			flow.put("uptime",getFlowUptime(id));
		}else{
			flow.put("id",id);
			flow.put("status",getFlowStatus(id));
		}

		json.put("flow",flow);

		String integrationInfo = json.toString(4);
		if(mediaType.contains("xml")) {
			integrationInfo = DocConverter.convertJsonToXml(integrationInfo);
		}

		return integrationInfo;

	}


	public String getFlowStatus(String id) {

		if(hasFlow(id)) {
			String updatedId;
			if(id.contains("-")){
				updatedId = id;
			}else{
				updatedId = id + "-";
			}

			try {
				List<Route> routesList = getRoutesByFlowId(updatedId);
				if(routesList.isEmpty()){
					flowStatus = "unconfigured";
				}else{
					String flowId = routesList.get(0).getId();
					ServiceStatus serviceStatus = routeController.getRouteStatus(flowId);
					flowStatus = serviceStatus.toString().toLowerCase();
				}
			}catch (Exception e) {
				log.error("Get status flow " + id + " failed.",e);

				flowStatus = "error: " + e.getMessage();
			}

		}else {
			flowStatus = "unconfigured";
		}

		return flowStatus;

	}

	public String getFlowUptime(String id) {

		String flowUptime;
		if(hasFlow(id)) {
			Route route = getRoutesByFlowId(id).get(0);
			flowUptime = route.getUptime();
		}else {
			flowUptime = "0";
		}

		return flowUptime;
	}

	public String getFlowLastError(String id) {

		List<Route> routeList = getRoutesByFlowId(id);
		StringBuilder sb = new StringBuilder();
		for(Route r : routeList){
			String routeId = r.getId();
			ManagedRouteMBean route = managed.getManagedRoute(routeId);

			if(route != null){
				RouteError lastError = route.getLastError();
				if(lastError != null){
					sb.append("RouteID: ");
					sb.append(routeId);
					sb.append("Error: ");
					sb.append(lastError);
					sb.append(";");
				}
			}
		}
		if(sb.toString().isEmpty()){
			flowInfo = "0";
		} else{
			flowInfo = sb.toString();
		}

		return flowInfo;
	}

	public String getFlowMessages(String flowId, boolean includeSteps, String mediaType) throws Exception  {

		JSONObject json = new JSONObject();
		JSONObject flow = new JSONObject();
		JSONArray steps = new JSONArray();

		long totalMessages = 0;
		long completedMessages = 0;
		long failedMessages = 0;
		long pendingMessages = 0;

		List<Route> routes = getRoutesByFlowId(flowId);

		for(Route r : routes){
			String routeId = r.getId();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null){
				totalMessages += route.getExchangesTotal();
				completedMessages += route.getExchangesCompleted();
				failedMessages += route.getExchangesFailed();
				pendingMessages += route.getExchangesInflight();
				if(includeSteps){
					JSONObject step = new JSONObject();
					String stepId = StringUtils.substringAfter(routeId,flowId + "-");
					step.put("id", stepId);
					step.put("total",route.getExchangesTotal());
					step.put("completed",route.getExchangesCompleted());
					step.put("failed",route.getExchangesFailed());
					step.put("pending",route.getExchangesInflight());
					steps.put(step);
				}
			}
		}

		flow.put("id",flowId);
		flow.put("total",totalMessages);
		flow.put("completed",completedMessages);
		flow.put("failed",failedMessages);
		flow.put("pending",pendingMessages);
		if(includeSteps){
			flow.put("steps",steps);
		}
		json.put("flow",flow);

		String flowStats = json.toString(4);
		if(mediaType.contains("xml")) {
			flowStats = DocConverter.convertJsonToXml(flowStats);
		}

		return flowStats;

	}

	public String getFlowTotalMessages(String flowId) throws Exception {

		List<Route> routeList = getRoutesByFlowId(flowId);

		long totalMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();
			ManagedRouteMBean route = managed.getManagedRoute(routeId);

			if(route != null){
				totalMessages += route.getExchangesTotal();
			}
		}

		flowInfo = Long.toString(totalMessages);

		return flowInfo;

	}

	public String getFlowCompletedMessages(String flowId) throws Exception {

		List<Route> routeList = getRoutesByFlowId(flowId);
		long completedMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null){
				completedMessages += route.getExchangesCompleted();
			}
		}

		flowInfo = Long.toString(completedMessages);

		return flowInfo;

	}

	public String getFlowFailedMessages(String flowId) throws Exception  {

		List<Route> routeList = getRoutesByFlowId(flowId);
		long failedMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null){
				failedMessages += route.getExchangesFailed();
			}
		}

		flowInfo = Long.toString(failedMessages);

		return flowInfo;

	}

	public String getFlowPendingMessages(String flowId) throws Exception  {

		List<Route> routeList = getRoutesByFlowId(flowId);
		long pendingMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null){
				pendingMessages += route.getExchangesInflight();
			}
		}

		flowInfo = Long.toString(pendingMessages);

		return flowInfo;

	}

	public String getStepMessages(String flowId, String stepId, String mediaType) throws Exception  {

		long totalMessages = 0;
		long completedMessages = 0;
		long failedMessages = 0;
		long pendingMessages = 0;

		String routeId = flowId + "-" + stepId;

		ManagedRouteMBean route = managed.getManagedRoute(routeId);
		if(route != null){
			totalMessages += route.getExchangesTotal();
			completedMessages += route.getExchangesCompleted();
			failedMessages += route.getExchangesFailed();
			pendingMessages += route.getExchangesInflight();
		}

		JSONObject json = new JSONObject();
		JSONObject step = new JSONObject();

		step.put("id",flowId);
		step.put("total",totalMessages);
		step.put("completed",completedMessages);
		step.put("failed",failedMessages);
		step.put("pending",pendingMessages);
		json.put("step",step);

		String flowStats = json.toString(4);
		if(mediaType.contains("xml")) {
			flowStats = DocConverter.convertJsonToXml(flowStats);
		}

		return flowStats;

	}

	public String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception  {

		Date date = new Date();
		String today = new SimpleDateFormat("yyyyMMdd").format(date);
		File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

		if(file.exists()) {
			List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
			if(numberOfEntries!=null && numberOfEntries < lines.size()) {
				lines = lines.subList(lines.size()-numberOfEntries, lines.size());
			}
			return StringUtils.join(lines, ',');
		}else {
			return "0";
		}
	}

	public TreeMap<String, String> getIntegrationAlertsCount() throws Exception  {

		TreeMap<String, String> numberOfEntriesList = new TreeMap<>();

		flowsMap.forEach((flowId, flowProps) -> {
			try {
				String numberOfEntries =  getFlowAlertsCount(flowId);
				numberOfEntriesList.put(flowId, numberOfEntries);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		return numberOfEntriesList;

	}

	public String getFlowAlertsCount(String id) throws Exception  {

		Date date = new Date();
		String today = new SimpleDateFormat("yyyyMMdd").format(date);
		File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

		if(file.exists()) {
			List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
			return Integer.toString(lines.size());
		}else {
			return "0";
		}
	}

	public String getFlowEventsLog(String id, Integer numberOfEntries) throws Exception  {

		Date date = new Date();
		String today = new SimpleDateFormat("yyyyMMdd").format(date);
		File file = new File(baseDir + "/events/" + id + "/" + today + "_events.log");

		if(file.exists()) {
			List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
			if(numberOfEntries!=null && numberOfEntries < lines.size()) {
				lines = lines.subList(lines.size()-numberOfEntries, lines.size());
			}
			return StringUtils.join(lines, ',');
		}else {
			return "0";
		}
	}

	public String getCamelRouteConfiguration(String id, String mediaType) throws Exception {

		StringBuilder buf = new StringBuilder();

		for (Route route : context.getRoutes()) {
			if(route.getId().equals(id) || route.getId().startsWith(id + "-")) {
				ManagedRouteMBean managedRoute = managed.getManagedRoute(route.getId());
				String xmlConfiguration = managedRoute.dumpRouteAsXml(true);
				xmlConfiguration = xmlConfiguration.replaceAll("<\\?xml(.+?)\\?>", "").trim();
				buf.append(xmlConfiguration);
			}
		}

		String camelRouteConfiguration = buf.toString();

		if(camelRouteConfiguration.isEmpty()) {
			camelRouteConfiguration = "0";
		}else {
			camelRouteConfiguration = "<routes xmlns=\"http://camel.apache.org/schema/spring\">" +
					camelRouteConfiguration +
					"</routes>";
			if(mediaType.contains("json")) {
				camelRouteConfiguration = DocConverter.convertXmlToJson(camelRouteConfiguration);
			}
		}

		return camelRouteConfiguration;
	}


	public String getFlowStats(String flowId, boolean fullStats, boolean includeMetaData, boolean includeSteps, String filter) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject flow = createBasicFlowJson(flowId);

		List<Route> routes = getRoutesByFlowId(flowId);
		if(filter!=null && !filter.isEmpty()){
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
		long total = 0, completed = 0, failed = 0, pending = 0;

		List<Long> uptimeList = new ArrayList<>();
		List<Date> lastFailedList = new ArrayList<>();
		List<Date> lastCompletedList = new ArrayList<>();

		for (Route r : routes) {

			ManagedRouteMBean route = managed.getManagedRoute(r.getId());

			total += route.getExchangesTotal();
			completed += route.getExchangesCompleted();
			failed += route.getExchangesFailed();
			pending += route.getExchangesInflight();

			if (fullStats) {
				// Update uptime if not set
				if (stats.uptime == null) {
					uptimeList.add(route.getUptimeMillis());
				}
				if (stats.lastFailed == null) {
					lastFailedList.add(route.getLastExchangeFailureTimestamp());
				}
				if (stats.lastCompleted == null) {
					lastCompletedList.add(route.getLastExchangeCompletedTimestamp());
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
					.max(Date::compareTo)
					.orElse(null);
			stats.lastCompleted = lastCompletedList.stream()
					.filter(Objects::nonNull)
					.max(Date::compareTo)
					.orElse(null);
		}

		return stats;
	}

	private void populateBasicStats(JSONObject flow, FlowStatistics stats) {
		flow.put("total", stats.totalMessages);
		flow.put("completed", stats.completedMessages);
		flow.put("failed", stats.failedMessages);
		flow.put("pending", stats.pendingMessages);
	}

	private void populateDetailedStats(JSONObject flow, FlowStatistics stats) throws MalformedObjectNameException {
		flow.put("status", getFlowStatus(flow.getString("id")));
		flow.put("timeout", getTimeout(context));
		flow.put("uptime", stats.uptime);
		flow.put("uptimeMillis", stats.uptimeMillis);
		flow.put("lastFailed", stats.lastFailed != null ? stats.lastFailed : "");
		flow.put("lastCompleted", stats.lastCompleted != null ? stats.lastCompleted : "");
	}

	private void populateMetadata(JSONObject flow, String flowId) throws Exception {
		TreeMap<String, String> flowProps = getFlowConfiguration(flowId);
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
		String uptime = null;
		Date lastFailed = null;
		Date lastCompleted = null;
	}

	private long getTimeout(CamelContext context) throws MalformedObjectNameException {
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
		if(mediaType.contains("xml")) {
			stepStats = DocConverter.convertJsonToXml(stepStats);
		}

		return stepStats;
	}

	private JSONObject getStepStats(String routeid, boolean fullStats) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject step = new JSONObject();

		ManagedRouteMBean route = managed.getManagedRoute(routeid);

		String stepStatus = getFlowStatus(routeid);

		step.put("id", routeid);
		step.put("status", stepStatus);

		if(route!=null && flowStatus.equals("started")) {

			if(fullStats){
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
			step.put("stats",stepStatsObject.get("stats"));
		}

		json.put("step", step);

		return json;

	}

	@Override
	public String getHealth(String type, String mediaType) throws Exception {

		Set<String> flowIds = new HashSet<>();

		List<Route> routes = context.getRoutes();

		for(Route route: routes){
			String routeId = route.getId();
			String flowId = StringUtils.substringBefore(routeId,"-");
			if(flowId!=null && !flowId.isEmpty()) {
				flowIds.add(flowId);
			}
		}

		String result = getHealthFromList(flowIds, type);

		if(mediaType.contains("xml")) {
			result = DocConverter.convertJsonToXml(result);
		}

		return result;
	}

	@Override
	public String getHealthByFlowIds(String flowIds, String type, String mediaType) throws Exception {

		String[] values = flowIds.split(",");

		Set<String> flowSet = new HashSet<>(Arrays.asList(values));

		String result = getHealthFromList(flowSet, type);

		if(mediaType.contains("xml")) {
			result = DocConverter.convertJsonToXml(result);
		}

		return result;

	}


	private String getHealthFromList(Set<String> flowIds, String type) throws Exception {

		JSONArray flows = new JSONArray();

		for(String flowId: flowIds){
			String flowHealth = getFlowHealth(flowId,type,false,false,false, "application/json");
			JSONObject flow = new JSONObject(flowHealth);
			flows.put(flow);
		}

		return flows.toString();

	}

	@Override
	public String getFlowHealth(String flowId, String type, boolean includeSteps, boolean includeError, boolean includeDetails, String mediaType) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject flow = new JSONObject();
		JSONArray steps = new JSONArray();

		String state = "UNKNOWN";

		List<Route> routes = getRoutesByFlowId(flowId);

		for(Route r : routes){

			String routeId = r.getId();
			String healthCheckId = type + ":" + routeId;
			JSONObject step = getStepHealth(routeId,healthCheckId,includeError,includeDetails);

			String stepState= step.getJSONObject("step").getString("state");
			if(!state.equalsIgnoreCase("DOWN")){
				state = stepState;
			}
			steps.put(step);

		}

		flow.put("id",flowId);
		flow.put("state",state);

		if(includeSteps) {
			flow.put("steps", steps);
		}
		json.put("flow",flow);

		String flowStats = json.toString(4);

		if(mediaType.contains("xml")) {
			flowStats = DocConverter.convertJsonToXml(flowStats);
		}

		return flowStats;

	}

	@Override
	public String getFlowStepHealth(String flowId, String stepId, String type, boolean includeError, boolean includeDetails, String mediaType) throws Exception {

		String routeid = flowId + "-" + stepId;
		String healthCheckId = type + ":" + routeid;

		JSONObject json = getStepHealth(routeid, healthCheckId, includeError, includeDetails);
		String stepHealth = json.toString(4);
		if(mediaType.contains("xml")) {
			stepHealth = DocConverter.convertJsonToXml(stepHealth);
		}

		return stepHealth;
	}

	private JSONObject getStepHealth(String routeid, String healthCheckId, boolean includeError, boolean includeDetails) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject step = new JSONObject();

		step.put("id", routeid);

		HealthCheck healthCheck = HealthCheckHelper.getHealthCheck(context, healthCheckId);

		if(healthCheck!=null && healthCheck.isReadiness()) {

			HealthCheck.Result result = healthCheck.callReadiness();
			step.put("state", result.getState().toString());

			if(includeError){
				JSONObject error = new JSONObject();
				Optional<Throwable> errorResultOptional = result.getError();
				if(errorResultOptional.isPresent()){
					Throwable errorResult = errorResultOptional.get();
					error.put("message",errorResult.getMessage());
					error.put("class",errorResult.getClass().getName());
				}
				step.put("error", error);
			}

			if(includeDetails){
				JSONObject details = new JSONObject();

				for (Map.Entry<String, Object> entry : result.getDetails().entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					details.put(key,value);
				}

				step.put("details", details);
			}

		}else{
			step.put("state", "UNKNOWN");
		}


		json.put("step", step);

		return json;
	}

	public String getStats(String mediaType) throws Exception {

		JSONObject json = new JSONObject();

		ManagedCamelContextMBean managedCamelContext = managed.getManagedCamelContext();

		json.put("camelId",managedCamelContext.getCamelId());
		json.put("camelVersion",managedCamelContext.getCamelVersion());
		json.put("status",managedCamelContext.getState());
		json.put("uptime",managedCamelContext.getUptime());
		json.put("uptimeMillis",managedCamelContext.getUptimeMillis());
		json.put("startedFlows",countFlows("started", "text/plain"));
		json.put("startedSteps",managedCamelContext.getStartedRoutes());
		json.put("exchangesTotal",managedCamelContext.getExchangesTotal());
		json.put("exchangesCompleted",managedCamelContext.getExchangesCompleted());
		json.put("exchangesInflight",managedCamelContext.getExchangesInflight());
		json.put("exchangesFailed",managedCamelContext.getExchangesFailed());
		json.put("cpuLoadLastMinute",managedCamelContext.getLoad01());
		json.put("cpuLoadLast5Minutes",managedCamelContext.getLoad05());
		json.put("cpuLoadLast15Minutes",managedCamelContext.getLoad15());
		json.put("memoryUsage",getMemoryUsage());
		json.put("totalThreads",ManagementFactory.getThreadMXBean().getThreadCount());

		String stats = json.toString(4);
		if(mediaType.contains("xml")) {
			stats = DocConverter.convertJsonToXml(stats);
		}

		return stats;

	}

	public String getThreads(String mediaType, String filter, int topEntries) throws Exception {

		List<JSONObject> jsonObjectList = new ArrayList<>();
		ThreadInfo[] threadInfoArray = ManagementFactory.getThreadMXBean().dumpAllThreads(true, true, 1);

		for(ThreadInfo threadInfo: threadInfoArray){
			JSONObject thread = new JSONObject();
			thread.put("id",threadInfo.getThreadId());
			thread.put("name",threadInfo.getThreadName());
			thread.put("status",threadInfo.getThreadState().name());
			thread.put("cpuTime",ManagementFactory.getThreadMXBean().getThreadCpuTime(threadInfo.getThreadId()));
			jsonObjectList.add(thread);
		}

		// Filter by name
		if(!filter.isEmpty()){

			jsonObjectList = jsonObjectList.stream()
					.filter(obj -> obj.getString("name").contains(filter))
					.toList();
		}


		// Filter by top entries
		if(topEntries >= 1){
			if(topEntries > jsonObjectList.size()){
				topEntries = jsonObjectList.size();
			}
			jsonObjectList = jsonObjectList.subList(0,topEntries);
		}

		// Sort by cpuTime
		List<JSONObject> sortedList = jsonObjectList.stream()
				.sorted(Comparator.comparingInt((JSONObject o) -> o.getInt("cpuTime")).reversed())
				.toList();

		// Rebuild the JSONArray from the sorted and filtered list
		JSONArray jsonArray = new JSONArray(sortedList);
		String result = jsonArray.toString();

		if(mediaType.contains("xml")) {
			result = DocConverter.convertJsonToXml(result);
		}

		return result;

	}

	private double getMemoryUsage(){

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

		ManagedCamelContextMBean managedCamelContext = managed.getManagedCamelContext();

		String result = managedCamelContext.dumpRoutesStatsAsXml(true,false);

		if(mediaType.contains("json")) {
			result = DocConverter.convertXmlToJson(result);
		}

		return result;

	}


	public String getFlowsStats(String mediaType) throws Exception {

		Set<String> flowIds = new HashSet<>();

		List<Route> routes = context.getRoutes();

		for(Route route: routes){
			String routeId = route.getId();
			String flowId = StringUtils.substringBefore(routeId,"-");
			if(flowId!=null && !flowId.isEmpty()) {
				flowIds.add(flowId);
			}
		}

		String result = getStatsFromList(flowIds, true, false, false);

		if(mediaType.contains("xml")) {
			result = DocConverter.convertJsonToXml(result);
		}

		return result;

	}

	public String getStatsByFlowIds(String flowIds, String filter, String mediaType) throws Exception {

		String[] values = flowIds.split(",");

		Set<String> flowSet = new HashSet<>(Arrays.asList(values));

		JSONArray flows = new JSONArray();

		Iterator<String> it = flowSet.iterator();
		while (it.hasNext()) {
			String flowId = it.next();

			JSONObject json = new JSONObject();
			JSONObject flow = createBasicFlowJson(flowId);

			List<Route> routes = getRoutesByFlowId(flowId);

			if(filter!=null && !filter.isEmpty()){
				routes = filterRoutes(routes, filter);
			}

			// Calculate basic statistics
			FlowStatistics stats = calculateFlowStatistics(routes, true);

			// Populate basic stats
			populateBasicStats(flow, stats);
			try {
				populateDetailedStats(flow, stats);
			} catch (MalformedObjectNameException e) {
				throw new RuntimeException(e);
			}

			// Build final response
			json.put("flow", flow);

			flows.put(json);
		}

		return flows.toString();

	}

	private String getStatsFromList(Set<String> flowIds, boolean fullStats, boolean includeMetaData, boolean includeSteps) throws Exception {
		return getStatsFromList(flowIds, "", fullStats, includeMetaData, includeSteps);
	}

	private String getStatsFromList(Set<String> flowIds, String filter, boolean fullStats, boolean includeMetaData, boolean includeSteps) throws Exception {

		JSONArray flows = new JSONArray();

		for(String flowId: flowIds){
			String flowStats = getFlowStats(flowId, fullStats, includeMetaData, includeSteps, filter);
			JSONObject flow = new JSONObject(flowStats);
			flows.put(flow);
		}

		return flows.toString();

	}

	public String getMessages(String mediaType) throws Exception {

		Set<String> flowIds = new HashSet<>();

		List<Route> routes = context.getRoutes();

		for(Route route: routes){
			String routeId = route.getId();
			String flowId = StringUtils.substringBefore(routeId,"-");
			if(flowId!=null && !flowId.isEmpty()) {
				flowIds.add(flowId);
			}
		}

		String result = getStatsFromList(flowIds, false, false, false);

		if(mediaType.contains("xml")) {
			result = DocConverter.convertJsonToXml(result);
		}

		return result;

	}

	public String getMetrics(String mediaType) throws Exception {

		String integrationStats = "0";
		MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);

		if(metricsService!=null) {
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

		if(historyService!=null) {
			integrationStats = historyService.dumpStatisticsAsJson();
			if(mediaType.contains("xml")) {
				integrationStats = DocConverter.convertJsonToXml(integrationStats);
			}
		}

		return integrationStats;

	}

	public String info(String mediaType) throws Exception {

		JSONObject json = new JSONObject();
		JSONObject info = new JSONObject();

		info.put("name",context.getName());
		info.put("version",context.getVersion());
		info.put("startDate", CamelContextHelper.getStartDate(context));
		info.put("startupType",context.getStartupSummaryLevel());
		info.put("uptime",context.getUptime());
		info.put("uptimeMiliseconds", context.getUptime().toMillis());
		info.put("numberOfRunningSteps",context.getRoutesSize());

		json.put("info",info);

		String integrationInfo = json.toString(4);
		if(mediaType.contains("xml")) {
			integrationInfo = DocConverter.convertJsonToXml(integrationInfo);
		}

		return integrationInfo;

	}

	private Set<String> getListOfFlowIds(String filter){

		//get all routes
		List<Route> routes = context.getRoutes();

		Set<String> flowIds = new HashSet<>();

		//filter flows from routes
		for(Route route: routes){
			String routeId = route.getId();
			String flowId = StringUtils.substringBefore(routeId,"-");
			if(flowId!=null && !flowId.isEmpty()) {
				if (filter != null && !filter.isEmpty()) {
					String serviceStatus = getFlowStatus(flowId);
					if (serviceStatus.equalsIgnoreCase(filter)) {
						flowIds.add(flowId);
					}
				}else{
					flowIds.add(flowId);
				}
			}
		}

		return flowIds;

	}

	public String getListOfFlows(String filter, String mediaType) throws Exception {

		Set<String> flowIds = getListOfFlowIds(filter);

		JSONArray flowsArray = new JSONArray();

		for(String flowId: flowIds){
			JSONObject flowObject = new JSONObject();
			flowObject.put("id",flowId);
			flowsArray.put(flowObject);
		}

		String result = flowsArray.toString();

		if(mediaType.contains("xml")) {
			JSONObject flowsObject = new JSONObject();
			JSONObject flowObject = new JSONObject();
			flowObject.put("flow",flowsArray);
			flowsObject.put("flows",flowObject);
			result = DocConverter.convertJsonToXml(flowsObject.toString());
		}

		return result;

	}

	public String getListOfFlowsDetails(String filter, String mediaType) throws Exception {

		Set<String> flowIds = getListOfFlowIds(filter);

		JSONArray flowsArray = new JSONArray();

		for(String flowId: flowIds) {
			JSONObject flowObject = new JSONObject(getFlowInfo(flowId, "application/json"));
			flowsArray.put(flowObject);
		}

		String result = flowsArray.toString();

		if(mediaType.contains("xml")) {
			JSONObject flowsObject = new JSONObject();
			JSONObject flowObject = new JSONObject();
			flowObject.put("flow",flowsArray);
			flowsObject.put("flows",flowObject);
			result = DocConverter.convertJsonToXml(flowsObject.toString());
		}

		return result;

	}

	public String getListOfSoapActions(String url, String mediaType) throws Exception {

		String result;

		Class<?> clazz;
		try {
			clazz = Class.forName("org.assimbly.soap.SoapActionsService");
			Object soapActions =  clazz.getDeclaredConstructor().newInstance();
			Method method = clazz.getDeclaredMethod("getSoapActions", String.class);
			result = (String) method.invoke(soapActions, url);
		} catch (Exception e) {
			log.error("SOAP Actions couldn't be retrieved.", e);
			result = "[]";
		}

		return result;

	}

	public String countFlows(String filter, String mediaType) throws Exception {

		Set<String> flowIds = getListOfFlowIds(filter);

		return Integer.toString(flowIds.size());

	}

	public String countSteps(String filter, String mediaType) throws Exception {

		List<Route> routes = context.getRoutes();

		Set<String> stepIds = new HashSet<>();

		for(Route route: routes){
			String routeId = route.getId();
			ManagedRouteMBean managedRoute = managed.getManagedRoute(routeId);

			if (filter != null && !filter.isEmpty()) {
				String serviceStatus = managedRoute.getState();
				if (serviceStatus.equalsIgnoreCase(filter)) {
					stepIds.add(routeId);
				}
			}else{
				stepIds.add(routeId);
			}
		}

		return Integer.toString(stepIds.size());

	}

	//Other management tasks

	public void setConnection(TreeMap<String, String> props, String key) throws Exception {
		new Connection(context, props, key).start();
	}

	public String getDocumentation(String componentType, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String doc = catalog.componentJSonSchema(componentType);

		if(doc==null || doc.isEmpty()) {
			doc = "Unknown component";
		}

		return doc;
	}

	public String getDocumentationVersion() {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		return catalog.getCatalogVersion();
	}

	public String getComponents(boolean includeCustomComponents, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String components = catalog.listComponentsAsJson();

		if(includeCustomComponents){
			URL url = Resources.getResource("custom-steps.json");
			String customComponent = Resources.toString(url, StandardCharsets.UTF_8);
			components = JsonHelper.mergeJsonArray(components,customComponent);
		}

		if(mediaType.contains("xml")) {
			components = DocConverter.convertJsonToXml(components);
		}

		return components;
	}

	public String getComponentSchema(String componentType, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String schema = catalog.componentJSonSchema(componentType);

		if(schema==null || schema.isEmpty()) {
			URL url = Resources.getResource("custom-steps-parameters.json");
			String customSchemas = Resources.toString(url, StandardCharsets.UTF_8);
			JSONArray jsonArray = new JSONArray(customSchemas);
			for(int i=0;i<jsonArray.length();i++)
			{
				JSONObject components = jsonArray.getJSONObject(i);
				JSONObject component = components.getJSONObject("component");
				String name = component.getString("name");
				if(name.equalsIgnoreCase(componentType)){
					schema = components.toString();
					break;
				}
			}
		}

		if(schema==null || schema.isEmpty()) {
			schema = "Unknown component";
		}else if(mediaType.contains("xml")) {
			schema = DocConverter.convertJsonToXml(schema);
		}

		return schema;
	}

	@Override
	public String getComponentParameters(String componentType, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String parameters = catalog.componentJSonSchema(componentType);

		if(parameters==null || parameters.isEmpty()) {
			parameters = "Unknown component";
		}else if(mediaType.contains("xml")) {
			parameters = DocConverter.convertJsonToXml(parameters);
		}

		return parameters;
	}

	public String validateFlow(String uri) {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		EndpointValidationResult valid = catalog.validateEndpointProperties(uri);

		if(valid.hasErrors()){
			return "invalid: " + valid.summaryErrorMessage(false);
		}else {
			return "valid";
		}

	}

	public String resolveDependency(String scheme) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();
		String jsonString = catalog.componentJSonSchema(scheme);

		if(jsonString == null || jsonString.isEmpty()){
			log.info("Unknown scheme: " + scheme + ". Error: Could not found scheme in catalog.");
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
			result = "Dependency " + dependency + " resolved failed. Error message: "  + e.getMessage();
		}

		return result;

	}


	public List<Class<?>> resolveMavenDependency(String groupId, String artifactId, String version) throws Exception {
		DependencyUtil dependencyUtil = new DependencyUtil();
		List<Path> paths = dependencyUtil.resolveDependency(groupId, artifactId, version);
		return dependencyUtil.loadDependency(paths);

	}

	public Component getComponent(List<Class<?>> classes, String scheme) throws Exception {

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

	public  CamelContext getContext() {
		return context;
	}

	public ProducerTemplate getProducerTemplate() {
		return context.createProducerTemplate();
	}

	public ConsumerTemplate getConsumerTemplate() {
		return context.createConsumerTemplate();
	}

	public void send(Object messageBody, ProducerTemplate template) {
		template.sendBody(messageBody);
	}

	public void sendWithHeaders(Object messageBody, TreeMap<String, Object> messageHeaders, ProducerTemplate template) {
		template.sendBodyAndHeaders(messageBody, messageHeaders);
	}


	public void send(String uri,Object messageBody, Integer numberOfTimes) throws IOException {

		try(ProducerTemplate template = context.createProducerTemplate()) {

			if (numberOfTimes.equals(1)) {
				log.info("Sending " + numberOfTimes + " message to " + uri);
				template.sendBody(uri, messageBody);
			} else {
				log.info("Sending " + numberOfTimes + " messages to " + uri);
				IntStream.range(0, numberOfTimes).forEach(i -> template.sendBody(uri, messageBody));
			}
		}
	}

	public void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes) throws IOException {

		try(ProducerTemplate template = context.createProducerTemplate()) {

			Exchange exchange = new DefaultExchange(context);
			exchange.getIn().setBody(messageBody);
			exchange = setHeaders(exchange, messageHeaders);

			if (numberOfTimes.equals(1)) {
				log.info("Sending " + numberOfTimes + " message to " + uri);
				template.send(uri, exchange);
			} else {
				log.info("Sending " + numberOfTimes + " messages to " + uri);
				Exchange finalExchange = exchange;
				IntStream.range(0, numberOfTimes).forEach(i -> template.send(uri, finalExchange));
			}
		}

	}

	public String sendRequest(String uri,Object messageBody) throws IOException {

		try(ProducerTemplate template = context.createProducerTemplate()) {

			log.info("Sending request message to " + uri);

			return template.requestBody(uri, messageBody, String.class);
		}
	}

	public String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders) {

		try(ProducerTemplate template = context.createProducerTemplate()){
			Exchange exchange = new DefaultExchange(context);
			exchange.getIn().setBody(messageBody);
			exchange = setHeaders(exchange, messageHeaders);
			exchange.setPattern(ExchangePattern.InOut);

			log.info("Sending request message to " + uri);
			Exchange result = template.send(uri,exchange);

			return result.getMessage().getBody(String.class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}


	}

	public Exchange setHeaders(Exchange exchange, TreeMap<String, Object> messageHeaders){
		for(Map.Entry<String,Object> messageHeader : messageHeaders.entrySet()) {

			String key = messageHeader.getKey();
			String value = StringUtils.substringBetween(messageHeader.getValue().toString(),"(",")");
			String language = StringUtils.substringBefore(messageHeader.getValue().toString(),"(");
			String result;

			if(value.startsWith("constant")) {
				exchange.getIn().setHeader(key,value);
			}else if(value.startsWith("xpath")){
				XPathFactory fac = new XPathFactoryImpl();
				result = XPathBuilder.xpath(key).factory(fac).evaluate(exchange, String.class);
				exchange.getIn().setHeader(key,result);
			}else{
				Language resolvedLanguage = exchange.getContext().resolveLanguage(language);
				Expression expression = resolvedLanguage.createExpression(value);
				result = expression.evaluate(exchange, String.class);
				exchange.getIn().setHeader(key,result);
			}

		}

		return exchange;
	}


	public Certificate[] getCertificates(String url) {

		Certificate[] certificates = new Certificate[0];

		try {
			CertificatesUtil util = new CertificatesUtil();
			certificates = util.downloadCertificates(url);
		} catch (Exception e) {
			log.error("Start certificates for url " + url + " failed.",e);
		}
		return certificates;
	}

	public Certificate getCertificateFromKeystore(String keystoreName, String keystorePassword, String certificateName) {
		String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
		CertificatesUtil util = new CertificatesUtil();
		return util.getCertificate(keystorePath, keystorePassword, certificateName);
	}

	public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) {

		try {
			CertificatesUtil util = new CertificatesUtil();
			Certificate[] certificates = util.downloadCertificates(url);
			String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
			util.importCertificates(keystorePath, keystorePassword, certificates);
		} catch (Exception e) {
			log.error("Set certificates for url " + url + " failed.",e);
		}
	}

	public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

		File file = new File(keystorePath);

		String result;

		if(file.exists()) {
			result = util.importCertificate(keystorePath, keystorePassword, certificateName,certificate);
		}else{
			result = "Keystore doesn't exist";
		}

		return result;

	}


	public Map<String,Certificate> importCertificatesInKeystore(String keystoreName, String keystorePassword, Certificate[] certificates) throws Exception {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

		File file = new File(keystorePath);

		if(file.exists()) {
			return util.importCertificates(keystorePath, keystorePassword, certificates);
		}else{
			throw new KeyStoreException("Keystore " + keystoreName + "doesn't exist");
		}

	}

	public Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;
		return util.importP12Certificate(keystorePath, keystorePassword, p12Certificate, p12Password);

	}

	public void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName) {

		String keystorePath = baseDir + SEP + SECURITY_PATH + SEP + keystoreName;

		CertificatesUtil util = new CertificatesUtil();
		util.deleteCertificate(keystorePath, keystorePassword);
	}

	@Override
	public ValidationErrorMessage validateCron(String cronExpression) {
		CronValidator cronValidator = new CronValidator();
		return cronValidator.validate(cronExpression);
	}

	@Override
	public HttpsCertificateValidator.ValidationResult validateCertificate(String httpsUrl) {
		HttpsCertificateValidator httpsCertificateValidator = new HttpsCertificateValidator();
		return httpsCertificateValidator.validate(httpsUrl);
	}


	@Override
	public ValidationErrorMessage validateUrl(String url) {
		UrlValidator urlValidator = new UrlValidator();
		return urlValidator.validate(url);
	}

	@Override
	public List<org.assimbly.dil.validation.beans.Expression> validateExpressions(List<org.assimbly.dil.validation.beans.Expression> expressions, boolean isPredicate) {
		ExpressionsValidator expressionValidator = new ExpressionsValidator();
		return expressionValidator.validate(expressions, isPredicate);
	}

	@Override
	public ValidationErrorMessage validateFtp(FtpSettings ftpSettings) throws IOException {
		FtpValidator ftpValidator = new FtpValidator();
		return ftpValidator.validate(ftpSettings);
	}

	@Override
	public AbstractMap.SimpleEntry validateRegex(Regex regex) {
		RegexValidator regexValidator = new RegexValidator();
		return regexValidator.validate(regex);
	}

	@Override
	public EvaluationResponse validateScript(EvaluationRequest scriptRequest) {
		ScriptValidator scriptValidator = new ScriptValidator();
		return scriptValidator.validate(scriptRequest);
	}

	@Override
	public List<ValidationErrorMessage> validateXslt(String url, String xsltBody) {
		XsltValidator xsltValidator = new XsltValidator();
		return xsltValidator.validate(url, xsltBody);
	}

	@Override
	public void setEncryptionProperties(Properties encryptionProperties) {
		this.encryptionProperties = encryptionProperties;
		EncryptionUtil encryptionUtil = getEncryptionUtil();

		EncryptableProperties encryptableProperties = new EncryptableProperties(encryptionUtil.getTextEncryptor());

		registry.bind("encryptableProperties", encryptableProperties);
		registry.bind("encryptionUtil", EncryptionUtil.class, encryptionUtil);

	}

	public EncryptionUtil getEncryptionUtil() {
		return new EncryptionUtil(encryptionProperties.getProperty("password"), encryptionProperties.getProperty("algorithm"));
	}

	private void setSSLContext() throws Exception {

		String baseDir2 = FilenameUtils.separatorsToUnix(baseDir);

		File securityPath = new File(baseDir + SEP + SECURITY_PATH + SEP);

		if (!securityPath.exists()) {
			boolean securityPathCreated = securityPath.mkdirs();
			if(!securityPathCreated){
				throw new Exception("Directory: " + securityPath.getAbsolutePath() + " cannot be create to store keystore files");
			}
		}

		String keyStorePath = baseDir2 + SEP + SECURITY_PATH + SEP + KEYSTORE_FILE;
		String trustStorePath = baseDir2 + SEP + SECURITY_PATH + SEP + TRUSTSTORE_FILE;

		SSLConfiguration sslConfiguration = new SSLConfiguration();

		SSLContextParameters sslContextParameters = sslConfiguration.createSSLContextParameters(keyStorePath, getKeystorePassword(), trustStorePath, getKeystorePassword());

		SSLContextParameters sslContextParametersKeystoreOnly = sslConfiguration.createSSLContextParameters(keyStorePath, getKeystorePassword(), null, null);

		SSLContextParameters sslContextParametersTruststoreOnly = sslConfiguration.createSSLContextParameters(null, null, trustStorePath, getKeystorePassword());

		registry.bind("default", sslContextParameters);
		registry.bind("sslContext", sslContextParameters);
		registry.bind("keystore", sslContextParametersKeystoreOnly);
		registry.bind("truststore", sslContextParametersTruststoreOnly);

		try {
			SSLContext sslContext = sslContextParameters.createSSLContext(context);
			sslContext.createSSLEngine();
		}catch (Exception e){
			log.error("Can't set SSL context for certificate keystore. TLS/SSL certificates are not available. Reason: " + e.getMessage());
		}

		String[] sslComponents = {"ftps", "https", "imaps", "kafka", "jetty", "netty", "netty-http", "smtps", "vertx-http"};

		sslConfiguration.setUseGlobalSslContextParameters(context, sslComponents);

	}

	/**
	 * This method returns a List of all Routes of a flow given the flowID, or a single route (from or to) given a routeID.
	 * @param id The flowID or routeID
	 * @return A List of Routes
	 */
	private List<Route> getRoutesByFlowId(String id){
		return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).toList();
	}

	private List<RouteStartupOrder> getRoutesStartupOrderByFlowId(String id){
		List<RouteStartupOrder> routeStartupOrder = context.getCamelContextExtension().getRouteStartupOrder();
		return routeStartupOrder.stream().filter(r -> r.getRoute().getId().startsWith(id)).toList();
	}

	private List<String> getAllRoutesByFlowId(String id) {
		return context.getRoutes().stream()
				.map(Route::getId)
				.filter(routeId -> routeId.startsWith(id))
				.toList();
	}

	private String getKeystorePassword() {
		String keystorePwd = System.getenv(KEYSTORE_PWD);
		if(StringUtils.isEmpty(keystorePwd)) {
			return "supersecret";
		}

		return keystorePwd;
	}

	public static BigDecimal parseBigDecimal(String value) {
		if (value == null || value.isEmpty()) {
			return BigDecimal.ZERO;  // or handle as needed
		}
		return new BigDecimal(value);
	}

	private static String getEnvironmentVariable(String envName, String defaultValue){
		String environmentVariable = System.getenv(envName);
		String value = defaultValue;
		if (environmentVariable != null && !environmentVariable.isEmpty()) {
			value = environmentVariable;
		}
		return value;
	}

	private static int getEnvironmentVariableAsInteger(String envName, int defaultValue){
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

}