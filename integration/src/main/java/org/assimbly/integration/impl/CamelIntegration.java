package org.assimbly.integration.impl;

import com.codahale.metrics.MetricRegistry;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ThreadPoolProfileBuilder;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.component.directvm.DirectVmComponent;
import org.apache.camel.component.jetty9.JettyHttpComponent9;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.vm.VmComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.*;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.util.concurrent.ThreadPoolRejectedPolicy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.beans.AggregateStrategy;
import org.assimbly.dil.blocks.beans.CustomHttpBinding;
import org.assimbly.dil.blocks.beans.UuidExtensionFunction;
import org.assimbly.dil.blocks.processors.*;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.loader.ConnectorRoute;
import org.assimbly.dil.loader.FlowLoader;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.transpiler.ssl.SSLConfiguration;
import org.assimbly.dil.event.EventCollector;
import org.assimbly.util.*;
import org.assimbly.util.file.DirectoryWatcher;
import org.assimbly.util.mail.ExtendedHeaderFilterStrategy;
import org.jasypt.properties.EncryptableProperties;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CamelIntegration extends BaseIntegration {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private CamelContext context;

	private static boolean started = false;
	private final int stopTimeout = 10;
	private ServiceStatus status;
	private String flowStatus;

	private final MetricRegistry metricRegistry = new MetricRegistry();
	private org.apache.camel.support.SimpleRegistry registry = new org.apache.camel.support.SimpleRegistry();
	private String flowInfo;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private RouteController routeController;
	private ManagedCamelContext managed;

	private Properties encryptionProperties;

	private boolean watchDeployDirectoryInitialized = false;
	private TreeMap<String, String> props;

	private TreeMap<String, String> confFiles = new TreeMap<String, String>();


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

		//setting tracing standby to true, so it can be enabled during runtime
		context.setTracingStandby(true);

		if(useDefaultSettings){
			setDefaultSettings();
		}

		//collect events
		context.getManagementStrategy().addEventNotifier(new EventCollector());

		//set management tasks
		routeController = context.getRouteController();
		managed = context.getExtension(ManagedCamelContext.class);

	}

	public void setDefaultSettings() throws Exception {

		setRouteTemplates();

		setDefaultBlocks();

		setThreadProfile(0,5,5000);

		setDebugging(false);

		setSuppressLoggingOnTimeout(true);

		setStreamCaching(true);

		setCertificateStore(true);

		setMetrics(true);

		setHistoryMetrics(true);

	}

	public void setTracing(boolean tracing, String traceType) {
		if(traceType.equalsIgnoreCase("backlog")){
			context.setBacklogTracing(true);
		}else if (traceType.equalsIgnoreCase("default")) {
			Tracer tracer = context.getTracer();
			tracer.setEnabled(tracing);
		}
	}

	public void setDebugging(boolean debugging) {
		context.setDebugging(debugging);
	}

	public void setStreamCaching(boolean streamCaching) {
		context.setStreamCaching(streamCaching);
	}

	public void setSuppressLoggingOnTimeout(boolean suppressLoggingOnTimeout) {
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(suppressLoggingOnTimeout);
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
	}

	public void setDefaultBlocks() throws Exception {

		registry.bind("customHttpBinding", new CustomHttpBinding());
		registry.bind("uuid-function", new UuidExtensionFunction());
		registry.bind("ExtendedHeaderFilterStrategy", new ExtendedHeaderFilterStrategy());

		context.addComponent("sync", new DirectVmComponent());
		context.addComponent("async", new VmComponent());

		context.addComponent("jetty-nossl", new JettyHttpComponent9());

		registry.bind("ManageFlowProcessor", new ManageFlowProcessor());

		registry.bind("SetBodyProcessor", new SetBodyProcessor());
		registry.bind("SetHeadersProcessor", new SetHeadersProcessor());
		registry.bind("SetPatternProcessor", new SetPatternProcessor());
		registry.bind("RoutingRulesProcessor", new RoutingRulesProcessor());

		registry.bind("CurrentAggregateStrategy", new AggregateStrategy());

		//following beans are registered by name, because they are not always available (and are ignored if not available).
		//bindByName("","world.dovetail.aggregate.AggregateStrategy");
		bindByName("CurrentEnrichStrategy","world.dovetail.enrich.EnrichStrategy");
		bindByName("Er7ToHl7Converter","world.dovetail.hl7.Er7Encoder");
		bindByName("ExtendedHeaderFilterStrategy","world.dovetail.cookies.CookieStore");
		bindByName("flowCookieStore","world.dovetail.cookies.CookieStore");
		bindByName("Hl7ToXmlConverter","world.dovetail.hl7.XmlMarshaller");
		bindByName("multipartProcessor","world.dovetail.multipart.processor.MultipartProcessor");
		bindByName("QueueMessageChecker","world.dovetail.throttling.QueueMessageChecker");
		bindByName("XmlToHl7Converter","world.dovetail.hl7.XmlEncoder");

		addServiceByName("world.dovetail.xmltojson.CustomXmlJsonDataFormat");

	}

	public void setThreadProfile(int poolSize, int maxPoolSize, int maxQueueSize) {

		ThreadPoolProfileBuilder builder = new ThreadPoolProfileBuilder("wiretapProfile");
		builder.poolSize(poolSize).maxPoolSize(maxPoolSize).maxQueueSize(maxQueueSize).rejectedPolicy(ThreadPoolRejectedPolicy.DiscardOldest).keepAliveTime(10L);
		context.getExecutorServiceManager().registerThreadPoolProfile(builder.build());

	}


	//loads templates in the template package
	public void setRouteTemplates() throws Exception {

		// create scanner and disable default filters (that is the 'false' argument)
		final ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
		provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(".*")));

		// get matching classes defined in the package
		final Set<org.springframework.beans.factory.config.BeanDefinition> classes = provider.findCandidateComponents("org.assimbly.dil.blocks.templates");

		// this is how you can load the class type from BeanDefinition instance
		for (BeanDefinition bean: classes) {
			Class<?> clazz = Class.forName(bean.getBeanClassName());
			Object template = clazz.getDeclaredConstructor().newInstance();
			if(template instanceof RouteBuilder){
				context.addRoutes((RouteBuilder) template);
			}

		}

	}

	// (Un)install files

	/* not used yet
	public void setRoutesDirectory(boolean deployOnEvent) throws Exception{

		Path path = Paths.get(baseDir + "/routes");

		//Create the deploy directory if not exist
		Files.createDirectories(path);

		RouteWatcherReloadStrategy reload = new RouteWatcherReloadStrategy();
		reload.setFolder(path.toString());
		//reload.setPattern("*.xml");
		//reload.setRecursive(true);

		context.addService(reload);
		reload.start();

	}*/


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

	private void checkDeployDirectory(Path path) throws Exception {
		Files.walk(path)
		 .filter(fPath -> fPath.toString().endsWith(".xml") || fPath.toString().endsWith(".json")  || fPath.toString().endsWith(".yaml"))
		 .forEach(fPath -> {
			 try{
				fileInstall(fPath);
			} catch (Exception e) {
				 log.error("Check deploy directory "+ path.toString() + " + failed",e);

			 }
		});
	}

	private void watchDeployDirectory(Path path) throws Exception {

		if(watchDeployDirectoryInitialized){
			return;
		}

		log.info("Deploy folder | Init watching for changes: " + path);
		watchDeployDirectoryInitialized = true;

		DirectoryWatcher watcher = new DirectoryWatcher.Builder()
				.addDirectories(path)
				.setPreExistingAsCreated(false)
				.build(new DirectoryWatcher.Listener() {
					private long diff;
					private long timeCreated;
					private Path pathCreated;

					public void onEvent(DirectoryWatcher.Event event, Path path) {
						switch (event) {
							case ENTRY_CREATE:
								log.info("Deploy folder | File created: " + path);
								try {
									pathCreated = path;
									timeCreated = System.currentTimeMillis();
									fileInstall(path);
								} catch (Exception e) {
									log.error("FileInstall for created " + path.toString() + " failed",e);
								}
								break;
							case ENTRY_MODIFY:
								log.info("Deploy folder | File modified: " + path);
								Long timeModified = System.currentTimeMillis();
								String pathAsString = path.toString();
								String fileName = FilenameUtils.getBaseName(pathAsString);


									try {
									if (path.equals(pathCreated) && confFiles.get(fileName)!= null){
										diff = timeModified - timeCreated;
										if(diff > 3000){
											fileReinstall(path);
										}
									}else if (confFiles.get(fileName)!= null){
											fileReinstall(path);
									}else{
										fileInstall(path);
									}
								} catch (Exception e) {
									log.error("FileInstall for modified " + path.toString() + " failed",e);
								}
								break;

							case ENTRY_DELETE:
								log.info("Deploy folder | File deleted: " + path);
								try {
									fileUninstall(path);
								} catch (Exception e) {
									log.error("FileUnInstall for deleted " + path.toString() + " failed",e);
								}
								break;
						}
					}
				});

		watcher.start();

	}


	public void fileInstall(Path path) throws Exception {

		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);
		String configuration = FileUtils.readFileToString(new File(pathAsString), "UTF-8");

		confFiles.put(fileName,configuration);

		if(mediaType.contains("json")){
			configuration = DocConverter.convertJsonToXml(configuration);
			mediaType = "xml";
		}else if(mediaType.contains("yaml")){
			configuration = DocConverter.convertYamlToXml(configuration);
			mediaType = "xml";
		}

		String flowId = setFlowId(fileName, configuration);

		if(flowId!=null){
			log.info("File install flowid=" + flowId + " | path=" + pathAsString);
			String status = configureAndStartFlow(flowId, mediaType, configuration);
			if(!status.equalsIgnoreCase("started")||status.equalsIgnoreCase("restarted")){
				log.error(status);
			}
		}else{
			log.error("File install for " + pathAsString + " failed. Invalid configuration file.");
		}

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

		stopFlow(flowId);

		fileInstall(path);

	}


	public String setFlowId(String filename, String configuration) throws Exception {

		String flowId = null;

		String configurationUTF8 = new String(configuration.getBytes("UTF-8"));

		Document doc = DocConverter.convertStringToDoc(configurationUTF8);
		XPath xPath = XPathFactory.newInstance().newXPath();

		String root = doc.getDocumentElement().getTagName();

		if(root.equals("dil") || root.equals("integrations") || root.equals("flows")){
			flowId = xPath.evaluate("//flows/flow[id='" + filename + "']/id",doc);
			if(flowId==null || flowId.isEmpty()){
				flowId = xPath.evaluate("//flows/flow[1]/id",doc);
			}
			if(flowId==null || flowId.isEmpty()){
				flowId = xPath.evaluate("//flows/flow[1]/name",doc);
			}
		}else if(root.equals("flow")){
			flowId = xPath.evaluate("//flow[id='" + filename + "']/id",doc);
			if(flowId==null || flowId.isEmpty()){
				flowId = xPath.evaluate("//flow/id",doc);
			}
			if(flowId==null || flowId.isEmpty()){
				flowId = xPath.evaluate("//flow/name",doc);
			}
		}else if(root.equals("camelContext")){
			flowId = xPath.evaluate("/camelContext/@id",doc);
			if(flowId==null || flowId.isEmpty()){
				log.warn("Configuration: CamelContext element doesn't have an id attribute");
			}
		}else if(root.equals("routes")){
			flowId = xPath.evaluate("/routes/@id",doc);
			if(flowId==null || flowId.isEmpty()){
				log.warn("Configuration: routes element doesn't have an id attribute");
			}
		}

		return flowId;

	}

	public void fileUninstall(Path path) throws Exception {

		String pathAsString = path.toString();
		String fileName = FilenameUtils.getBaseName(pathAsString);
		String configuration = confFiles.get(fileName);
		confFiles.remove(fileName);

		String flowId = setFlowId(fileName, configuration);

		if(flowId!=null){
			log.info("File uninstall flowid=" + flowId + " | path=" + pathAsString);
			stopFlow(flowId);
		}else{
			log.error("File uninstall for " + pathAsString + " failed. FlowId is null.");
		}

		/*
		String pathAsString = path.toString();
		String flowId = FilenameUtils.getBaseName(pathAsString);

		log.info("File uninstall flowid=" + flowId + " | path=" + pathAsString);

		stopFlow(flowId);
		*/
	}


	//Manage integration

	public void start() throws Exception {

		// start Camel context
		if(!started){

			context.start();
			started = true;

			log.info("Integration started");

		}

	}

	public void stop() throws Exception {
		super.getFlowConfigurations().clear();
		if (context != null){
			for (Route route : context.getRoutes()) {
				routeController.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
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

	public String addFlow(TreeMap<String, String> props)  {

		try{
			//create connections & install dependencies if needed
			createConnections(props);

			//set up flow by type
			String flowType  = props.get("flow.type");

			if(flowType.equalsIgnoreCase("connector")){
				addConnectorFlow(props);
			}else if(flowType.equalsIgnoreCase("routes")){
				addRoutesFlow(props);
			}else{
				return loadFlow(props);
			}

		}catch (Exception e){
			log.error("add flow failed: ", e);
			return e.getMessage();
		}

		return "loaded";

	}

	public void createConnections(TreeMap<String, String> props) throws Exception {

		for (String key : props.keySet()){

			if (key.endsWith("connection.id")){
				setConnection(props, key);
			}

			if (key.equals("flow.dependencies") && props.get(key) != null){

				String[] schemes = StringUtils.split(props.get(key), ",");

				for (String scheme : schemes) {
					if(!DependencyUtil.CompiledDependency.hasCompiledDependency(scheme.toLowerCase()) && context.hasComponent(scheme.toLowerCase()) == null) {
						log.warn("Component " + scheme + " is not supported by Assimbly. Try to resolve dependency dynamically.");
						if(INetUtil.isHostAvailable("repo1.maven.org")){
							log.info(resolveDependency(scheme));
						}else{
							log.error("Failed to resolve " + scheme + ". No available internet is found. Cannot reach http://repo1.maven.org/maven2/");
						}
					}
				}
			}
		}
	}

	public void addConnectorFlow(final TreeMap<String, String> props) throws Exception {
		ConnectorRoute flow = new ConnectorRoute(props);
		flow.updateRoutesToCamelContext(context);
	}

	public String loadFlow(final TreeMap<String, String> props) throws Exception {

		FlowLoader flow = new FlowLoader(props);
		flow.updateRoutesToCamelContext(context);

		if(!flow.isFlowLoaded()){
			return flow.getReport();
		}

		return "started";

	}

	public void addRoutesFlow(final TreeMap<String, String> props) throws Exception {

		for (String key : props.keySet()) {

			if (key.endsWith("route")){
				String xml = props.get(key);
				updateRoute(xml);
			}
		}
	}

	//later move to https://www.javadoc.io/doc/org.apache.camel/camel-api/3.14.2/org/apache/camel/spi/RoutesLoader.html
	//ExtendedCamelContext extended = context.getExtension(ExtendedCamelContext.class);
	//extended.getRoutesLoader().updateRoutes(resources);
	// https://stackoverflow.com/questions/67758503/load-a-apache-camel-route-at-runtime-from-a-file

	public void updateRoute(String route) throws Exception {
		ExtendedCamelContext extendedCamelContext = context.adapt(ExtendedCamelContext.class);
		RoutesLoader loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);
		loader.updateRoutes(resource);
	}

	public void addEventNotifier(EventNotifier eventNotifier) throws Exception {
		context.getManagementStrategy().addEventNotifier(eventNotifier);
	}

	public boolean removeFlow(String id) throws Exception {

		if(hasFlow(id)) {
			return context.removeRoute(id);
		}else {
			return false;
		}

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

		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
        Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			flowStatus = startFlow(props.get("id"));
			if(!flowStatus.equals("started")) {
				return "failed to start flow with id " + props.get("id") + ". Status is " + flowStatus;
			}
        }

		return "started";
	}

	public String restartAllFlows() throws Exception {
		log.info("Restarting all flows");

		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
        Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			flowStatus = restartFlow(props.get("id"));
			if(!flowStatus.equals("restarted")) {
				return "failed to restart flow with id " + props.get("id") + ". Status is " + flowStatus;
			}
        }

		return "restarted";
	}

	public String pauseAllFlows() throws Exception {
		log.info("Pause all flows");
		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();

		Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			flowStatus = restartFlow(props.get("id"));
			if(!flowStatus.equals("restarted")) {
				return "failed to restart flow with id " + props.get("id") + ". Status is " + flowStatus;
			}
        }

		return "paused";
	}

	public String resumeAllFlows() throws Exception {
		log.info("Resume all flows");

		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
        Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			flowStatus = resumeFlow(props.get("id"));
			if(!flowStatus.equals("restarted")) {
				return "failed to resume flow with id " + props.get("id") + ". Status is " + flowStatus;
			}
        }

		return "started";
	}

	public String stopAllFlows() throws Exception {
		log.info("Stopping all flows");

		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
        Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			flowStatus = stopFlow(props.get("id"));
			if(!flowStatus.equals("restarted")) {
				return "failed to stop flow with id " + props.get("id") + ". Status is " + flowStatus;
			}
        }

		return "stopped";
	}

	public String configureAndStartFlow(String flowId, String mediaType, String configuration) throws Exception {
		super.setFlowConfiguration(flowId, mediaType, configuration);
		String status = startFlow(flowId);
		return status;
	}

	public String configureAndRestartFlow(String flowId, String mediaType, String configuration) throws Exception {
		super.setFlowConfiguration(flowId, mediaType, configuration);
		String status = restartFlow(flowId);
		return status;
	}

	public String testFlow(String flowId, String mediaType, String configuration, boolean stopTest) throws Exception {
		if(stopTest){
			return stopFlow(flowId);
		}
		return configureAndStartFlow(flowId, mediaType, configuration);
	}

	public String fileInstallFlow(String flowId, String mediaType, String configuration) throws Exception {

		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.writeStringToFile(flowFile, configuration, Charset.defaultCharset());
			return "saved";
		} catch (Exception e) {
			log.error("FileInstall flow " + flowId + " failed",e);
			return "Fail to save flow " + flowId + " Error: " + e.getMessage();
		}

	}

	public String fileUninstallFlow(String flowId, String mediaType) throws Exception {

		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.deleteQuietly(flowFile);
			return "deleted";
		} catch (Exception e) {
			log.error("FileUninstall flow " + flowId + " failed",e);
			return "failed to delete flow " + e.getMessage();
		}

	}


	public String routesFlow(String flowId, String mediaType, String configuration) throws Exception {

		TreeMap<String, String> props = new TreeMap<>();
		props.put("id",flowId);
		props.put("flow.name",flowId);
		props.put("flow.type","esb");
		props.put("route.1.route", configuration);

		loadFlow(props);

		String status = startFlow(flowId);

		return status;

	}


	public String startFlow(String id) {

		boolean addFlow = false;
		String loadReport;

		try {

			List<TreeMap<String, String>> allProps = super.getFlowConfigurations();

			for (int i = 0; i < allProps.size(); i++) {
				props = allProps.get(i);

				String configureId = props.get("id");

				if (configureId.equals(id)) {
					addFlow = true;
				}

			}

			if(addFlow){
				loadReport = addFlow(props);
			}else{
				loadReport = "Starting flow failed | Flow ID: " + id + " does not match Flow ID in configuration";
				log.error(loadReport);
			}

			if (!loadReport.equals("loaded") && !loadReport.equals("started")){
				stopFlow(id);
				return loadReport;
			}else if(loadReport.equals("loaded")) {

				List<Route> steps = getRoutesByFlowId(id);

				log.info("Starting " + steps.size() + " steps");

				for (Route step : steps) {
					status = startStep(step);
				}

				if (status != null) {
					log.info("Started flow | id=" + id);
					return status.toString().toLowerCase();
				} else {
					log.info("Failed starting flow | id=" + id);
					return "error: can't get status";
				}
			}else{
				return "started";
			}

		}catch (Exception e) {
			if(context.isStarted()) {
				stopFlow(id);
				log.error("Start flow " + id + " failed.",e);
				return e.getMessage();
			}else{
				log.error("Unable to start flow " + id + ". Integration isn't running");
				return "Unable to start flow " + id + ". Integration isn't running";
			}
		}
	}

	private ServiceStatus startStep(Route route){

		String routeId = route.getId();

		status = routeController.getRouteStatus(routeId);

		if(status.isStarted()) {
			log.info("Started step | id=" + routeId);
		} else {
			try {

				log.info("Starting step | id=" + routeId);

				routeController.startRoute(routeId);

				int count = 1;

				do {
					if(status.isStarted()) {break;}
					Thread.sleep(10);
					count++;

				} while (status.isStarting() || count < 3000);

			} catch (Exception e) {
				log.error("Failed starting step | id=" + routeId);
				return status;
			}

			log.info("Started step | id=" + routeId);

		}

		return status;
	}


	public String restartFlow(String id) {

		log.info("Restart flow | id=" + id);

		try {

			if(hasFlow(id)) {

				stopFlow(id);

				return startFlow(id);

			}else {
				log.warn("FlowId: " + id + " couldn't be found. Start flow, instead of restart.");

				return startFlow(id);
			}

		}catch (Exception e) {
			log.error("Restart flow " + id + " failed.",e);
			return e.getMessage();
		}
	}

	public String stopFlow(String id) {

		log.info("Stopping flow | id=" + id);

		try {

			List<Route> routeList = getRoutesByFlowId(id);

			for (Route route : routeList) {
				String routeId = route.getId();
				log.info("Stopping step | id=" + route.getId());
				routeController.stopRoute(routeId, stopTimeout, TimeUnit.SECONDS);
				context.removeRoute(routeId);
			}

			log.info("Stopped flow | id=" + id);
	        return "stopped";

		}catch (Exception e) {
			log.error("Stop flow " + id + " failed.",e);
			return e.getMessage();
		}

	}

	public String pauseFlow(String id) {
		log.info("Pause flow | id=" + id);

		try {

			if(hasFlow(id)) {

				List<Route> routeList = getRoutesByFlowId(id);
				status = routeController.getRouteStatus(routeList.get(0).getId());

				for(Route route : routeList){
					if(!routeController.getRouteStatus(route.getId()).isSuspendable()){
						return "Flow isn't suspendable (Step " + route.getId() + ")";
					}
				}

				for(Route route : routeList){
					String routeId = route.getId();

					routeController.suspendRoute(routeId);

					int count = 1;

					do {
						status = routeController.getRouteStatus(routeId);
						if(status.isSuspended()) {
							log.info("Paused (suspend) flow | id=" + id + ", step " + routeId);
							break;
						}else if(status.isStopped()){
							log.info("Paused (stopped) flow | id=" + id + ", step " + routeId);

							break;
						}

						Thread.sleep(10);
						count++;

					} while (status.isSuspending() || count < 6000);
				}
				log.info("Paused flow id=" + id);
				return status.toString().toLowerCase();

			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}

		}catch (Exception e) {
			log.error("Pause flow " + id + " failed.",e);
			stopFlow(id); //Stop flow if one of the routes cannot be paused.
			return e.getMessage();
		}


	}

	public String resumeFlow(String id) throws Exception {
		log.info("Resume flow id=" + id);

		try {

			if(hasFlow(id)) {
				boolean resumed = true;
				List<Route> routeList = getRoutesByFlowId(id);
				for(Route route : routeList){
					String routeId = route.getId();
					status = routeController.getRouteStatus(routeId);
					if(status.isSuspended()){
						routeController.resumeRoute(routeId);

						int count = 1;

						do {
							status = routeController.getRouteStatus(routeId);
							if(status.isStarted()) {break;}
							Thread.sleep(10);
							count++;

						} while (status.isStarting() || count < 3000);

						resumed = true;
						log.info("Resumed flow id=" + id + ", step " + routeId);

					}
					else if (status.isStopped()){

						log.info("Starting route as route " + id + " is currently stopped (not suspended)");
						startFlow(routeId);
						resumed = true;
					}
				}
				if(resumed){
					log.info("Resumed flow id=" + id);
					return status.toString().toLowerCase();
				}else {
					return "Flow isn't suspended (nothing to resume)";
				}
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}

		}catch (Exception e) {
			log.error("Resume flow " + id + " failed.",e);
			stopFlow(id); //Stop flow if one of the routes cannot be resumed.
			return e.getMessage();
		}


	}

	public boolean isFlowStarted(String id) {

		if(hasFlow(id)) {
			ServiceStatus status = null;
			List<Route> routes = getRoutesByFlowId(id);

			for(Route route : routes){
				status = routeController.getRouteStatus(route.getId());

				if(!status.isStarted()){
					return false;
				}
			}
			return status != null && status.isStarted();
		}else {
			return false;
		}

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
				ServiceStatus status = routeController.getRouteStatus(getRoutesByFlowId(updatedId).get(0).getId());
				flowStatus = status.toString().toLowerCase();
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
				org.apache.camel.api.management.mbean.RouteError lastError = route.getLastError();
				if(lastError != null){
					sb.append("RouteID: ");
					sb.append(routeId);
					sb.append("Error: ");
					sb.append(lastError.toString());
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


	public String getFlowTotalMessages(String id) throws Exception {

		List<Route> routeList = getRoutesByFlowId(id);

		long totalMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();
			String description = r.getDescription();
			ManagedRouteMBean route = managed.getManagedRoute(routeId);

			if(route != null && "from".equals(description)){
				totalMessages += route.getExchangesTotal();
			}
		}

		flowInfo = Long.toString(totalMessages);

		return flowInfo;

	}

	public String getFlowCompletedMessages(String id) throws Exception {

		List<Route> routeList = getRoutesByFlowId(id);
		long completedMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();
			String description = r.getDescription();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null && "from".equals(description)){
				completedMessages += route.getExchangesCompleted();
			}
		}

		flowInfo = Long.toString(completedMessages);

		return flowInfo;

	}

	public String getFlowFailedMessages(String id) throws Exception  {

		List<Route> routeList = getRoutesByFlowId(id);
		long failedMessages = 0;

		for(Route r : routeList){
			String routeId = r.getId();
			String description = r.getDescription();

			ManagedRouteMBean route = managed.getManagedRoute(routeId);
			if(route != null && "from".equals(description)){
				failedMessages += route.getExchangesFailed();
			}
		}

		flowInfo = Long.toString(failedMessages);

		return flowInfo;

	}

	public String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception  {

		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

		  if(file.exists()) {
		  List<String> lines = FileUtils.readLines(file, "utf-8");
		  if(numberOfEntries!=null && numberOfEntries < lines.size()) {
			  lines = lines.subList(lines.size()-numberOfEntries, lines.size());
		  }
		  	  String alertsLog = StringUtils.join(lines, ',');

		  	  return alertsLog;
		  }else {
			  return "0";
		  }
	}

	public TreeMap<String, String> getIntegrationAlertsCount() throws Exception  {

		TreeMap<String, String> numberOfEntriesList = new TreeMap<String, String>();
		List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
        Iterator<TreeMap<String, String>> it = allProps.iterator();
        while(it.hasNext()){
            TreeMap<String, String> props = it.next();
			String flowId = props.get("id");
			String numberOfEntries =  getFlowAlertsCount(flowId);
			numberOfEntriesList.put(flowId, numberOfEntries);
        }
		return numberOfEntriesList;

	}

	public String getFlowAlertsCount(String id) throws Exception  {

		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  File file = new File(baseDir + "/alerts/" + id + "/" + today + "_alerts.log");

		  if(file.exists()) {
			  List<String> lines = FileUtils.readLines(file, "utf-8");
			  String numberOfEntries = Integer.toString(lines.size());
		   	  return numberOfEntries;
		  }else {
			  return "0";
		  }
	}

	public String getFlowEventsLog(String id, Integer numberOfEntries) throws Exception  {

		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  File file = new File(baseDir + "/events/" + id + "/" + today + "_events.log");

		  if(file.exists()) {
		  List<String> lines = FileUtils.readLines(file, "utf-8");
		  if(numberOfEntries!=null && numberOfEntries < lines.size()) {
			  lines = lines.subList(lines.size()-numberOfEntries, lines.size());
		  }
		  	  String eventLog = StringUtils.join(lines, ',');

		  	  return eventLog;
		  }else {
			  return "0";
		  }
	}


	public String getCamelRouteConfiguration(String id, String mediaType) throws Exception {

		String camelRouteConfiguration = "";

		for (Route route : context.getRoutes()) {
			if(route.getId().equals(id) || route.getId().startsWith(id + "-")) {
				ManagedRouteMBean managedRoute = managed.getManagedRoute(route.getId());
				String xmlConfiguration = managedRoute.dumpRouteAsXml(true);
				xmlConfiguration = xmlConfiguration.replaceAll("\\<\\?xml(.+?)\\?\\>", "").trim();
				camelRouteConfiguration = camelRouteConfiguration + xmlConfiguration;
			}
		}


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


	public String getAllCamelRoutesConfiguration(String mediaType) throws Exception {

		ManagedCamelContextMBean managedCamelContext = managed.getManagedCamelContext();

		String camelRoutesConfiguration = managedCamelContext.dumpRoutesAsXml(true);

		if(mediaType.contains("json")) {
			camelRoutesConfiguration = DocConverter.convertXmlToJson(camelRoutesConfiguration);
		}

		return camelRoutesConfiguration;

	}


	public String getFlowStats(String id, String stepid, String mediaType) throws Exception {

		String routeid = id + "-" + stepid;

		ManagedRouteMBean route = managed.getManagedRoute(routeid);

		flowStatus = getFlowStatus(routeid);

		String flowStats;
		if(route!=null && flowStatus.equals("started")) {
			flowStats = route.dumpStatsAsXml(true);
			if(mediaType.contains("json")) {
				flowStats = DocConverter.convertXmlToJson(flowStats);
			}
		}else {
			flowStats = "0";
		}

		return flowStats;
	}

	public String getStats(String statsType, String mediaType) throws Exception {

		String integrationStats;
		if(statsType.equals("history")) {

			MetricsMessageHistoryService historyService = context.hasService(MetricsMessageHistoryService.class);

			if(historyService!=null) {
				integrationStats = historyService.dumpStatisticsAsJson();
				if(mediaType.contains("xml")) {
					integrationStats = DocConverter.convertJsonToXml(integrationStats);
				}
			}else {
				integrationStats = "0";
			}
		}else {
			MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);

			if(metricsService!=null) {
				integrationStats = metricsService.dumpStatisticsAsJson();
				if(mediaType.contains("xml")) {
					integrationStats = DocConverter.convertJsonToXml(integrationStats);
				}
			}else {
				integrationStats = "0";
			}
		}

		return integrationStats;

	}


	public String getRunningFlows(String mediaType) throws Exception {

		Set<String> flowIds = new HashSet<String>();

		List<Route> routes = context.getRoutes();

		for(Route route: routes){
			String routeId = route.getId();
			String flowId = StringUtils.substringBefore(routeId,"-");
			if(flowId!=null && !flowId.isEmpty()) {
				flowIds.add(flowId);
			}
		}

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


	//Other management tasks

	public TreeMap<String, String> setConnection(TreeMap<String, String> props, String key) throws Exception {
		return new Connection(context, props, key).start();
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


	public String getComponents(String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String components = catalog.listComponentsAsJson();

		if(mediaType.contains("xml")) {
			components = DocConverter.convertJsonToXml(components);
		}

		return components;
	}

	public String getComponentSchema(String componentType, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();

		String schema = catalog.componentJSonSchema(componentType);


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
			log.info("Unknown scheme: " + scheme);
			return null;
		}
		JSONObject componentSchema = new JSONObject(jsonString);
		JSONObject component = componentSchema.getJSONObject("component");

		String groupId = component.getString("groupId");
		String artifactId = component.getString("artifactId");
		String version = catalog.getCatalogVersion();  //versionManager.getLoadedVersion(); //component.getString("version");

		String dependency = groupId + ":" + artifactId + ":" + version;
		String result;

		try {
			List<Class> classes = resolveMavenDependency(groupId, artifactId, version);
			Component camelComponent = getComponent(classes, scheme);
			context.addComponent(scheme, camelComponent);
			result = "Dependency " + dependency + " resolved";
		} catch (Exception e) {
			result = "Dependency " + dependency + " resolved failed. Error message: "  + e.getMessage();
		}

		return result;

	}


	public List<Class> resolveMavenDependency(String groupId, String artifactId, String version) throws Exception {

		DependencyUtil dependencyUtil = new DependencyUtil();
		List<Path> paths = dependencyUtil.resolveDependency(groupId, artifactId, version);
		List<Class> classes = dependencyUtil.loadDependency(paths);

		return classes;

	}

    public Component getComponent(List<Class> classes, String scheme) throws Exception {

		Component component = null;
		for(Class classToLoad: classes){
			String className = classToLoad.getName().toLowerCase();
			if(className.endsWith(scheme + "component")){
				Object object =  classToLoad.newInstance();
				component = (Component) object;
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


	public void send(String uri,Object messageBody, Integer numberOfTimes) {

		ProducerTemplate template = context.createProducerTemplate();

		if(numberOfTimes.equals(1)){
			log.info("Sending " + numberOfTimes + " message to " + uri);
			template.sendBody(uri, messageBody);
		}else{
			log.info("Sending " + numberOfTimes + " messages to " + uri);
			IntStream.range(0, numberOfTimes).forEach(i -> template.sendBody(uri, messageBody));
		}
	}

	public void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes) {

		ProducerTemplate template = context.createProducerTemplate();

		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(messageBody);
		exchange = setHeaders(exchange, messageHeaders);

		if(numberOfTimes.equals(1)){
			log.info("Sending " + numberOfTimes + " message to " + uri);
			template.send(uri,exchange);
		}else{
			log.info("Sending " + numberOfTimes + " messages to " + uri);
			Exchange finalExchange = exchange;
			IntStream.range(0, numberOfTimes).forEach(i -> template.send(uri, finalExchange));
		}

	}

	public String sendRequest(String uri,Object messageBody) {

		ProducerTemplate template = context.createProducerTemplate();

		log.info("Sending request message to " + uri);

		return template.requestBody(uri, messageBody,String.class);
	}

	public String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders) {

		ProducerTemplate template = context.createProducerTemplate();

		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(messageBody);
		exchange = setHeaders(exchange, messageHeaders);
		exchange.setPattern(ExchangePattern.InOut);

		log.info("Sending request message to " + uri);
		Exchange result = template.send(uri,exchange);

		return result.getMessage().getBody(String.class);
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
				XPathFactory fac = new net.sf.saxon.xpath.XPathFactoryImpl();
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
		String keystorePath = baseDir + "/security/" + keystoreName;
		CertificatesUtil util = new CertificatesUtil();
    	return util.getCertificate(keystorePath, keystorePassword, certificateName);
	}

	public void setCertificatesInKeystore(String keystoreName, String keystorePassword, String url) {

		try {
			CertificatesUtil util = new CertificatesUtil();
			Certificate[] certificates = util.downloadCertificates(url);
			String keystorePath = baseDir + "/security/" + keystoreName;
			util.importCertificates(keystorePath, keystorePassword, certificates);
		} catch (Exception e) {
			log.error("Set certificates for url " + url + " failed.",e);
		}
	}

	public String importCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName, Certificate certificate) {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + "/security/" + keystoreName;

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

		String keystorePath = baseDir + "/security/" + keystoreName;

		File file = new File(keystorePath);

		if(file.exists()) {
			return util.importCertificates(keystorePath, keystorePassword, certificates);
		}else{
			throw new KeyStoreException("Keystore " + keystoreName + "doesn't exist");
		}

	}

	public Map<String,Certificate> importP12CertificateInKeystore(String keystoreName, String keystorePassword, String p12Certificate, String p12Password) throws Exception {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + "/security/" + keystoreName;
		return util.importP12Certificate(keystorePath, keystorePassword, p12Certificate, p12Password);

	}

	public void deleteCertificateInKeystore(String keystoreName, String keystorePassword, String certificateName) {

		String keystorePath = baseDir + "/security/" + keystoreName;

		CertificatesUtil util = new CertificatesUtil();
    	util.deleteCertificate(keystorePath, keystorePassword, certificateName);
	}



	public void setEncryptionProperties(Properties encryptionProperties) {
		this.encryptionProperties = encryptionProperties;
		setEncryptedPropertiesComponent();
	}

	public EncryptionUtil getEncryptionUtil() {
		return new EncryptionUtil(encryptionProperties.getProperty("password"), encryptionProperties.getProperty("algorithm"));
	}

	private void setEncryptedPropertiesComponent() {
		EncryptionUtil encryptionUtil = getEncryptionUtil();
		EncryptableProperties initialProperties = new EncryptableProperties(encryptionUtil.getTextEncryptor());
		PropertiesComponent propertiesComponent = new PropertiesComponent();
		propertiesComponent.setInitialProperties(initialProperties);
		context.setPropertiesComponent(propertiesComponent);
	}

	private void setSSLContext() throws Exception {

		File securityPath = new File(baseDir + "/security");

		if (!securityPath.exists()) {
			securityPath.mkdirs();
		}

		String keyStorePath = baseDir + "/security/keystore.jks";
		String trustStorePath = baseDir + "/security/truststore.jks";

		SSLConfiguration sslConfiguration = new SSLConfiguration();

		SSLContextParameters sslContextParameters = sslConfiguration.createSSLContextParameters(keyStorePath, "supersecret", trustStorePath, "supersecret");

		SSLContextParameters sslContextParametersKeystoreOnly = sslConfiguration.createSSLContextParameters(keyStorePath, "supersecret", null, null);

		SSLContextParameters sslContextParametersTruststoreOnly = sslConfiguration.createSSLContextParameters(null, null, trustStorePath, "supersecret");

		registry.bind("default", sslContextParameters);
		registry.bind("sslContext", sslContextParameters);
		registry.bind("keystore", sslContextParametersKeystoreOnly);
		registry.bind("truststore", sslContextParametersTruststoreOnly);

		context.setSSLContextParameters(sslContextParameters);

		String[] sslComponents = {"ftps", "https", "imaps", "kafka", "jetty", "netty", "netty-http", "smtps", "vertx-http"};

		for (String sslComponent : sslComponents) {
			sslConfiguration.setUseGlobalSslContextParameters(context, sslComponent);
		}
	}

	/**
	 * This method returns a List of all Routes of a flow given the flowID, or a single route (from or to) given a routeID.
	 * @param id The flowID or routeID
	 * @return A List of Routes
	 */
	private List<Route> getRoutesByFlowId(String id){
		return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).collect(Collectors.toList());
	}

	public void bindByName(String beanId, String className){

		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			Object bean =  clazz.getDeclaredConstructor().newInstance();
			registry.bind(beanId, bean);
		} catch (Exception e) {
			//Ignore if class not found
		}

	}

	public void addServiceByName(String className){

		Class<?> clazz;
		try {
			clazz = Class.forName(className);
			Object bean =  clazz.getDeclaredConstructor().newInstance();
			context.addService(bean);
		} catch (Exception e) {
			//Ignore if class not found
		}

	}

}