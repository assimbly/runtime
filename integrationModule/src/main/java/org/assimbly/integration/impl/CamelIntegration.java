package org.assimbly.integration.impl;

import com.codahale.metrics.MetricRegistry;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.language.xpath.XPathBuilder;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.Language;
import org.apache.camel.spi.RouteController;
import org.apache.camel.spi.Tracer;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.integration.configuration.ssl.SSLConfiguration;
import org.assimbly.integration.event.EventCollector;
import org.assimbly.integration.routes.ConnectorRoute;
import org.assimbly.integration.routes.ESBRoute;
import org.assimbly.integration.routes.SimpleRoute;
import org.assimbly.integration.service.Connection;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.integration.beans.CustomHttpBinding;
import org.assimbly.util.*;
import org.assimbly.util.file.DirectoryWatcher;
import org.jasypt.properties.EncryptableProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CamelIntegration extends BaseIntegration {

	private CamelContext context;

	private boolean started = false;
	private int stopTimeout = 30;
	private ServiceStatus status;
	private String flowStatus;
	private String flowUptime;

	private String flowStats;
	private String integrationStats;
	private MetricRegistry metricRegistry = new MetricRegistry();
	private org.apache.camel.support.SimpleRegistry registry = new org.apache.camel.support.SimpleRegistry();
	private String flowInfo;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private RouteController routeController;
	private ManagedCamelContext managed;

	private Properties encryptionProperties;

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.integration.impl.CamelIntegration");

	public CamelIntegration() {
		try {
			initIntegration(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CamelIntegration(boolean useDefaultSettings) {
		try {
			initIntegration(useDefaultSettings);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	public CamelIntegration(String integrationId, String configuration, boolean useDefaultSettings) throws Exception {
		initIntegration(useDefaultSettings);
		setFlowConfiguration(convertXMLToFlowConfiguration(integrationId, configuration));
	}

	public CamelIntegration(String integrationId, String configuration) throws Exception {
		initIntegration(true);
		setFlowConfiguration(convertXMLToFlowConfiguration(integrationId, configuration));
	}

	public CamelIntegration(String integrationId, URI configuration) throws Exception {
		initIntegration(true);
		setFlowConfiguration(convertXMLToFlowConfiguration(integrationId, configuration));
	}

	public void initIntegration(boolean useDefaultSettings) throws Exception {

		//set basic settings
		context = new DefaultCamelContext(registry);

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

		setDebugging(false);

		setSuppressLoggingOnTimeout(true);

		setStreamCaching(true);

		setCertificateStore(true);

		setMetrics(true);

		setHistoryMetrics(true);
		
		//Start Dovetail specific beans
		CustomHttpBinding customHttpBinding = new CustomHttpBinding();
		registry.bind("customHttpBinding", customHttpBinding);
		//End Dovetail specific beans

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


	// (Un)install files
	
	public void setDeployDirectory(boolean deployOnStart, boolean deployOnEvent) throws Exception {
			
		Path path = Paths.get(baseDir + "/deploy");
		
		//Create the deploy directory if not exist
		Files.createDirectories(path);

		if(deployOnStart){
			//Check & Start files found in the deploy directory
			checkDeployDirectory(path);
		}
		
		if(deployOnEvent){
			//Monitor files in the deploy directory after start
			watchDeployDirectory(path);
		}	
				
	}

	private void checkDeployDirectory(Path path) throws Exception {
		Files.walk(path)
		 .filter(fPath -> fPath.toString().endsWith(".xml"))
		 .forEach(fPath -> {
			 try{
				fileInstall(fPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	
	private void watchDeployDirectory(Path path) throws Exception {
		
		DirectoryWatcher watcher = new DirectoryWatcher.Builder()
				.addDirectories(path)
				.setPreExistingAsCreated(false)
				.build(new DirectoryWatcher.Listener() {
					public void onEvent(DirectoryWatcher.Event event, Path path) {
						switch (event) {
							case ENTRY_CREATE:
								logger.info("Deploy folder | File created: " + path);	
								try {
									fileInstall(path);
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
							case ENTRY_MODIFY:
								logger.info("Deploy folder | File modified: " + path);	
								try {
									fileInstall(path);
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
								
							case ENTRY_DELETE:
								logger.info("Deploy folder | File deleted: " + path);	
								try {
									fileUninstall(path);
								} catch (Exception e) {
									e.printStackTrace();
								}
								break;
						}
					}
				});
				
		watcher.start();			
		
	} 
	
	
	public void fileInstall(Path path) throws Exception {
		
		String pathAsString = path.toString();
		String flowId = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);
		String configuration = FileUtils.readFileToString(new File(pathAsString), "UTF-8");

		logger.info("File install flowid=" + flowId + " | path=" + pathAsString);	

		configureAndStartFlow(flowId, mediaType, configuration);
		
	}

	public void fileUninstall(Path path) throws Exception {
		
		String pathAsString = path.toString();
		String flowId = FilenameUtils.getBaseName(pathAsString);
		String mediaType = FilenameUtils.getExtension(pathAsString);

		logger.info("File uninstall flowid=" + flowId + " | path=" + pathAsString);	

		stopFlow(flowId);
		
	}
	
	
	//Manage integration
	
	public void start() throws Exception {

		// start Camel context
		context.start();
		started = true;
		logger.info("Integration started");

		setDeployDirectory(true, true);
		
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
			logger.info("Integration stopped");
		}
	}	

	public boolean isStarted() {
		return started;
	}
	


	//Manage flows

	public void addFlow(TreeMap<String, String> props) throws Exception {
		
		//create connections & install dependencies if needed
		for (String key : props.keySet()){

			if (key.endsWith("service.id")){
				props = setConnection(props, key);
			}

			if (key.equals("flow.components") && props.get(key) != null){
				
				String[] schemes = StringUtils.split(props.get(key), ",");

				for (String scheme : schemes) {

					scheme = scheme.toLowerCase();

					if(!DependencyUtil.CompiledDependency.hasCompiledDependency(scheme) && context.hasComponent(scheme) == null) {
						logger.warn("Component " + scheme + " is not supported by Assimbly. Try to resolve dependency dynamically.");
						if(INetUtil.isHostAvailable("repo1.maven.org")){
							logger.info(resolveDependency(scheme));
						}else{
							logger.error("Failed to resolve " + scheme + ". No available internet is found. Cannot reach http://repo1.maven.org/maven2/");
						}
					}
				}
			}
		}

		//set up flow by type
		String flowType  = props.get("flow.type");

		if (flowType == null || flowType.isEmpty() || flowType.equals("default")){
			//use connector flow as the default flow
			addConnectorFlow(props);
			flowType = "default";
		}else if(flowType.equalsIgnoreCase("connector")){
			addConnectorFlow(props);
		}else if(flowType.equalsIgnoreCase("esb")){
			addESBFlow(props);
		}else if(flowType.equalsIgnoreCase("simple")){
			addSimpleFlow(props);
		}else if(flowType.equalsIgnoreCase("routes")){
			addRoutesFlow(props);
		}
		
		logger.info("Loaded flow configuration | type=" + flowType);

	}

	public void addConnectorFlow(final TreeMap<String, String> props) throws Exception {
		ConnectorRoute flow = new ConnectorRoute(props);
		flow.updateRoutesToCamelContext(context);
	}

	public void addESBFlow(final TreeMap<String, String> props) throws Exception {
		ESBRoute flow = new ESBRoute(props);
		flow.updateRoutesToCamelContext(context);
	}
	
	public void addSimpleFlow(final TreeMap<String, String> props) throws Exception {
		context.addRoutes(new SimpleRoute(props));
	}

	public void addRoutesFlow(final TreeMap<String, String> props) throws Exception {

		for (String key : props.keySet()) {

			if (key.endsWith("route")){
				String xml = props.get(key);
				addXmlRoute(xml);
			}
		}
	}

	public void addXmlRoute(String xml) throws Exception {
		ManagedCamelContextMBean managedContext = managed.getManagedCamelContext();
		managedContext.addOrUpdateRoutesFromXml(xml);
	}
	
	public void addEventNotifier(EventNotifier eventNotifier) throws Exception {
		context.getManagementStrategy().addEventNotifier(eventNotifier);
	}

	public boolean removeFlow(String id) throws Exception {
		
		if(!hasFlow(id)) {
			return false;
		}else {
			return context.removeRoute(id);	
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
		logger.info("Starting all flows");

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
		logger.info("Restarting all flows");
		
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
		logger.info("Pause all flows");
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
		logger.info("Resume all flows");
		
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
		logger.info("Stopping all flows");
		
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
	
	public String testFlow(String flowId, String mediaType, String configuration) throws Exception {
		return configureAndStartFlow(flowId, mediaType, configuration);
	}

	public String fileInstallFlow(String flowId, String mediaType, String configuration) throws Exception {
		
		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.writeStringToFile(flowFile, configuration, true);
			return "saved";	
		} catch (Exception e) {
			e.printStackTrace();
			return "failed to save flow " + e.getMessage();			
		}			
	
	}

	public String fileUninstallFlow(String flowId, String mediaType) throws Exception {
		
		try {
			File flowFile = new File(baseDir + "/deploy/" + flowId + ".xml");
			FileUtils.deleteQuietly(flowFile);
			return "deleted";	
		} catch (Exception e) {
			e.printStackTrace();
			return "failed to delete flow " + e.getMessage();			
		}			
	
	}
	
	
	public String routesFlow(String flowId, String mediaType, String configuration) throws Exception {
		
		TreeMap<String, String> props = new TreeMap<>();
		props.put("id",flowId);
		props.put("flow.name",flowId);
		props.put("flow.type","esb");
		props.put("esb.1.route", configuration);
		
		addESBFlow(props);
		
		String status = startFlow(flowId);
	
		return status;	
	
	}

	
	public String startFlow(String id) {

		logger.info("Start flow | id=" + id);

		boolean flowAdded = false;
		
		try {

			List<TreeMap<String, String>> allProps = super.getFlowConfigurations();
			for(int i = 0; i < allProps.size(); i++){
				TreeMap<String, String> props = allProps.get(i);

				if (props.get("id").equals(id)) {
					
					logger.info("Load flow configuration | id=" + id);
					addFlow(props);
					flowAdded = true;
				}
			
			}
			
			if(flowAdded){

				List<Route> routeList = getRoutesByFlowId(id);

				for(Route route : routeList){
					String routeId = route.getId();
					status = routeController.getRouteStatus(routeId);
					if(!status.isStarted()) {
						logger.info("Starting route " + routeId);
						routeController.startRoute(routeId);

						int count = 1;

						do {
							if(status.isStarted()) {break;}
							Thread.sleep(10);
							count++;

						} while (status.isStarting() || count < 3000);

					} else {
						logger.info("Started route | id=" + routeId);
					}
				}

				//logger.info("Started flow | id=" + id);
				return status.toString().toLowerCase();
				
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
			
		}catch (Exception e) {
			if(!context.isStarted()) {
				logger.info("Unable to start flow. Integration isn't running");
			}	
			e.printStackTrace();
			stopFlow(id);			
			return e.getMessage();
		}
	}

	public String restartFlow(String id) {

		logger.info("Restart flow | id=" + id);

		try {
			if(hasFlow(id)) {

				stopFlow(id);

				return startFlow(id);

			}else {
				return "Configuration is not set and running";
			}
	        
		}catch (Exception e) {
			stopFlow(id);
			e.printStackTrace();
			return e.getMessage();
		}
	}
	
	public String stopFlow(String id) {
		logger.info("Stop flow | id=" + id);
		try {
			List<Route> routeList = getRoutesByFlowId(id);
			for (Route route : routeList) {
				routeController.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
				context.removeRoute(route.getId());
			}

	        return "stopped";

		}catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String pauseFlow(String id) {
		logger.info("Pause flow | id=" + id);
		
		try {

			if(hasFlow(id)) {

				List<Route> routeList = getRoutesByFlowId(id);
				status = routeController.getRouteStatus(routeList.get(0).getId());

				for(Route route : routeList){
					if(!routeController.getRouteStatus(route.getId()).isSuspendable()){
						return "Flow isn't suspendable (Route " + route.getId() + ")";
					}
				}

				for(Route route : routeList){
					String routeId = route.getId();

					routeController.suspendRoute(routeId);

					int count = 1;

					do {
						status = routeController.getRouteStatus(routeId);
						if(status.isSuspended()) {
							logger.info("Paused (suspend) flow | id=" + id + ", route " + routeId);
							break;
						}else if(status.isStopped()){
							logger.info("Paused (stopped) flow | id=" + id + ", route " + routeId);

							break;
						}

						Thread.sleep(10);
						count++;

					} while (status.isSuspending() || count < 6000);
				}
				logger.info("Paused flow id=" + id);
				return status.toString().toLowerCase();

			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
		
		}catch (Exception e) {
			e.printStackTrace();
			stopFlow(id); //Stop flow if one of the routes cannot be pauzed. Maybe find a more elegant solution?
			return e.getMessage();
		}

		
	}

	public String resumeFlow(String id) throws Exception {
		logger.info("Resume flow id=" + id);
		
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
						logger.info("Resumed flow id=" + id + ", route " + routeId);

					}
					else if (status.isStopped()){

						logger.info("Starting route as route " + id + " is currently stopped (not suspended)");
						startFlow(routeId);
						resumed = true;
					}
				}
				if(resumed){
					logger.info("Resumed flow id=" + id);
					return status.toString().toLowerCase();
				}else {
					return "Flow isn't suspended (nothing to resume)";
				}
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
		
		}catch (Exception e) {
			e.printStackTrace();
			stopFlow(id); //Stop flow if one of the routes cannot be resumed. Maybe find an more elegant solution?
			return e.getMessage();
		}

		
	}	

	public boolean isFlowStarted(String id) {
		
		if(hasFlow(id)) {
			ServiceStatus status = null;
			List routeList = getRoutesByFlowId(id);

			for(Route route : getRoutesByFlowId(id)){
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
			if(!id.contains("-")){
				id = id + "-";
			}
			try {
				ServiceStatus status = routeController.getRouteStatus(getRoutesByFlowId(id).get(0).getId());
				flowStatus = status.toString().toLowerCase();
			}catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				flowStatus = "error: " + e.getMessage();
			}

		}else {
			flowStatus = "unconfigured";			
		}
		
		return flowStatus;
		
	}

	public String getFlowUptime(String id) {
	
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
		if(!sb.toString().isEmpty()){
			flowInfo = sb.toString();
		} else{
			flowInfo = "0";
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
	
	
	public String getFlowStats(String id, String endpointid, String mediaType) throws Exception {

		String routeid = id + "-" + endpointid;

		ManagedRouteMBean route = managed.getManagedRoute(routeid);

		flowStatus = getFlowStatus(routeid);
		
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
				
		String parameters = managed.getManagedCamelContext().componentParameterJsonSchema(componentType);
		
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
			logger.info("Unknown scheme: " + scheme);
			return null;
		}
		JSONObject componentSchema = new JSONObject(jsonString);
		JSONObject component = componentSchema.getJSONObject("component");

		String groupId = component.getString("groupId");
		String artifactId = component.getString("artifactId");
		String version = catalog.getCatalogVersion();  //versionManager.getLoadedVersion(); //component.getString("version");

		String dependency = groupId + ":" + artifactId + ":" + version;
		String result = "";

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
			logger.info("Sending " + numberOfTimes + " message to " + uri);
			template.sendBody(uri, messageBody);
		}else{
			logger.info("Sending " + numberOfTimes + " messages to " + uri);
			IntStream.range(0, numberOfTimes).forEach(i -> template.sendBody(uri, messageBody));
		}
	}

	public void sendWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders, Integer numberOfTimes) {

		ProducerTemplate template = context.createProducerTemplate();

		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(messageBody);
		exchange = setHeaders(exchange, messageHeaders);

		if(numberOfTimes.equals(1)){
			logger.info("Sending " + numberOfTimes + " message to " + uri);
			template.send(uri,exchange);
		}else{
			logger.info("Sending " + numberOfTimes + " messages to " + uri);
			Exchange finalExchange = exchange;
			IntStream.range(0, numberOfTimes).forEach(i -> template.send(uri, finalExchange));
		}

	}

	public String sendRequest(String uri,Object messageBody) {

		ProducerTemplate template = context.createProducerTemplate();

		logger.info("Sending request message to " + uri);

		return template.requestBody(uri, messageBody,String.class);
	}

	public String sendRequestWithHeaders(String uri, Object messageBody, TreeMap<String, Object> messageHeaders) {

		ProducerTemplate template = context.createProducerTemplate();

		Exchange exchange = new DefaultExchange(context);
		exchange.getIn().setBody(messageBody);
		exchange = setHeaders(exchange, messageHeaders);
		exchange.setPattern(ExchangePattern.InOut);

		logger.info("Sending request message to " + uri);
		Exchange result = template.send(uri,exchange);

		return result.getMessage().getBody(String.class);
	}

	public Exchange setHeaders(Exchange exchange, TreeMap<String, Object> messageHeaders){
		for(Map.Entry<String,Object> messageHeader : messageHeaders.entrySet()) {

			String key = messageHeader.getKey();
			String value = StringUtils.substringBetween(messageHeader.getValue().toString(),"(",")");
			String language = StringUtils.substringBefore(messageHeader.getValue().toString(),"(");
			String result = "";

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
    	try {
    		CertificatesUtil util = new CertificatesUtil();
    		Certificate[] certificates = util.downloadCertificates(url);
    		return certificates;
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	return null;
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
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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

		String result;

		if(file.exists()) {
			return util.importCertificates(keystorePath, keystorePassword, certificates);
		}else{
			throw new Exception("Keystore doesn't exist");
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
		registry.bind("keystore", sslContextParametersKeystoreOnly);
		registry.bind("truststore", sslContextParametersTruststoreOnly);

		context.setSSLContextParameters(sslContextParameters);

		String[] sslComponents = {"ftps", "https", "imaps", "kafka", "netty", "netty-http", "smtps", "vertx-http"};

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
    
}