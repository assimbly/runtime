package org.assimbly.connector.impl;

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
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.connector.event.EventCollector;
import org.assimbly.connector.routes.DefaultRoute;
import org.assimbly.connector.routes.SimpleRoute;
import org.assimbly.connector.service.Connection;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.*;
import org.jasypt.properties.EncryptableProperties;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class CamelConnector extends BaseConnector {

	private CamelContext context;

	private boolean started = false;
	private int stopTimeout = 30;
	private ServiceStatus status;
	private String flowStatus;
	private String flowUptime;

	private String flowStats;
	private String connectorStats;
	private MetricRegistry metricRegistry = new MetricRegistry();
	private org.apache.camel.support.SimpleRegistry registry = new org.apache.camel.support.SimpleRegistry();
	private String flowInfo;

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
	private RouteController routeController;
	private ManagedCamelContext managed;

	private Properties encryptionProperties;

	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.CamelConnector");

	public CamelConnector() {
		try {
			setBasicSettings();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public CamelConnector(String connectorId, String configuration) throws Exception {
		setBasicSettings();
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}

	public CamelConnector(String connectorId, URI configuration) throws Exception {
		setBasicSettings();
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}

	public void setBasicSettings() throws Exception {

		//set basic settings
		context = new DefaultCamelContext(registry);
		context.setStreamCaching(true);
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

		//setting transport security globally
        context.setSSLContextParameters(createSSLContextParameters());
        ((SSLContextParametersAware) context.getComponent("ftps")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("https")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("imaps")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("kafka")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("netty")).setUseGlobalSslContextParameters(true);
		((SSLContextParametersAware) context.getComponent("netty-http")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("smtps")).setUseGlobalSslContextParameters(true);
		((SSLContextParametersAware) context.getComponent("vertx-http")).setUseGlobalSslContextParameters(true);
		//((SSLContextParametersAware) context.getComponent("jetty")).setUseGlobalSslContextParameters(false);

		//set default metrics
		context.addRoutePolicyFactory(new MetricsRoutePolicyFactory());

		//set history metrics
		MetricsMessageHistoryFactory factory = new MetricsMessageHistoryFactory();
		factory.setPrettyPrint(true);
		factory.setMetricsRegistry(metricRegistry);
		context.setMessageHistoryFactory(factory);

		//collect events
		context.getManagementStrategy().addEventNotifier(new EventCollector());

		//set management tasks
		routeController = context.getRouteController();
		managed = context.getExtension(ManagedCamelContext.class);

	}

	@Override
	public void setEncryptionProperties(Properties encryptionProperties) {
		this.encryptionProperties = encryptionProperties;
		setEncryptedPropertiesComponent();
	}

	@Override
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


	public void start() throws Exception {

		// start Camel context
		context.start();
		started = true;
		logger.info("Connector started");

	}

	public void stop() throws Exception {
		super.getConfiguration().clear();
		if (context != null){
			for (Route route : context.getRoutes()) {
				
				routeController.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
				context.removeRoute(route.getId());
			}
			context.stop();
			started = false;
			logger.info("Connector stopped");
		}
	}	

	public boolean isStarted() {
		return started;
	}
	
	public void setTracing(boolean tracing) {
        context.setTracing(tracing);
	}

	public void setDebugging(boolean debugging) {
        context.setDebugging(debugging);
	}
	
	public void addFlow(TreeMap<String, String> props) throws Exception {
		
		//create connections & install dependencies if needed
		for (String key : props.keySet()){

			if (key.endsWith("service.id")){
				props = setConnection(props, key);
			}

			if (key.equals("flow.components")){

				String[] schemes = StringUtils.split(props.get(key), ",");

				for (String scheme : schemes) {

					scheme = scheme.toLowerCase();

					if(!DependencyUtil.CompiledDependency.hasCompiledDependency(scheme) && context.hasComponent(scheme) == null) {
						logger.info(resolveDependency(scheme));
					}
				}

			}

		}

		//set up route by type
		String route  = props.get("flow.type");
		if (route == null){
			addDefaultFlow(props);
			route = "unknown";
			logger.info("Loaded flow configuration | type=" + route);
		}else if(route.equalsIgnoreCase("default")){
			addDefaultFlow(props);
			logger.info("Loaded flow configuration | type=" + route);
		}else if(route.equalsIgnoreCase("simple")){
			addDefaultFlow(props);
			logger.info("Loaded flow configuration | type=" + route);
		}else if(route.equalsIgnoreCase("xml")){
			addXmlFlow(props);
			logger.info("Loaded flow configuration | type=" + route);
		}else{
			logger.info("Unknown flow type.");
		}


	}

	public void addDefaultFlow(final TreeMap<String, String> props) throws Exception {
		DefaultRoute flow = new DefaultRoute(props);

		context.addRoutes(flow);
	}

	public void addSimpleFlow(final TreeMap<String, String> props) throws Exception {
		context.addRoutes(new SimpleRoute(props));
	}

	public void addXmlFlow(final TreeMap<String, String> props) throws Exception {

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

		List<TreeMap<String, String>> allProps = super.getConfiguration();
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
		
		List<TreeMap<String, String>> allProps = super.getConfiguration();
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
		List<TreeMap<String, String>> allProps = super.getConfiguration();
        
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
		
		List<TreeMap<String, String>> allProps = super.getConfiguration();
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
		
		List<TreeMap<String, String>> allProps = super.getConfiguration();
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
	
	public String startFlow(String id) {

		logger.info("Start flow | id=" + id);

		boolean flowAdded = false;
		
		try {

			List<TreeMap<String, String>> allProps = super.getConfiguration();
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

				logger.info("Started flow | id=" + id);
				return status.toString().toLowerCase();
				
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
			
		}catch (Exception e) {
			if(!context.isStarted()) {
				logger.info("Unable to start flow. Connector isn't running");
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
			ServiceStatus status = routeController.getRouteStatus(getRoutesByFlowId(id).get(0).getId());
			flowStatus = status.toString().toLowerCase();		
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

	public TreeMap<String, String> getConnectorAlertsCount() throws Exception  {
		  
		TreeMap<String, String> numberOfEntriesList = new TreeMap<String, String>();
		List<TreeMap<String, String>> allProps = super.getConfiguration();
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
				connectorStats = historyService.dumpStatisticsAsJson();
				if(mediaType.contains("xml")) {
					connectorStats = DocConverter.convertJsonToXml(connectorStats);
				}
			}else {
				connectorStats = "0";
			}
		}else {
			MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);
			
			if(metricsService!=null) {
				connectorStats = metricsService.dumpStatisticsAsJson();
				if(mediaType.contains("xml")) {
					connectorStats = DocConverter.convertJsonToXml(connectorStats);
				}
			}else {
				connectorStats = "0";
			}
		}
		
		return connectorStats;

	}	


	public TreeMap<String, String> setConnection(TreeMap<String, String> props, String key) throws Exception {
		return new Connection(context, props, key).start();
	}

	
	public String getDocumentation(String componentType, String mediaType) throws Exception {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();
 		
		String doc = catalog.componentHtmlDoc(componentType);

		if(doc==null || doc.isEmpty()) {
			doc = "Unknown component";
		}
		
		return doc;		
	}

	public String getDocumentationVersion() {

		DefaultCamelCatalog catalog = new DefaultCamelCatalog();
		
		return catalog.getCatalogVersion();
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
		String version = component.getString("version");

		String result = resolveDependency(groupId, artifactId, version);

		//This maybe needed to activate the component
		//Component component2 = context.getComponent("file");
		
		return result;
			
	}

	
	public String resolveDependency(String groupId, String artifactId, String version) {
		
		String result;
		DependencyUtil dependencyUtil = new DependencyUtil();
		String dependency = groupId + ":" + artifactId + ":" + version;
		
		try {
			dependencyUtil.resolveDependency(groupId, artifactId, version);
			result = "Dependency " + dependency + " resolved";
		} catch (Exception e) {
			result = "Dependency " + dependency + "resolved failed. Error message: "  + e.getMessage();
		}
		
		return result;
			
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

	public Certificate getCertificateFromTruststore(String certificateName) {
		String truststorePath = baseDir + "/security/truststore.jks";
		CertificatesUtil util = new CertificatesUtil();
    	return util.getCertificate(truststorePath, certificateName); 
	}	


	public String importCertificateInTruststore(String certificateName, Certificate certificate) {

		String keystorePath = baseDir + "/security/keystore.jks";
		String truststorePath = baseDir + "/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
		util.importCertificate(keystorePath, certificateName,certificate);    	
    	return util.importCertificate(truststorePath, certificateName,certificate); 
				
	}

	
	public Map<String,Certificate> importCertificatesInTruststore(Certificate[] certificates) {

		String keystorePath = baseDir + "/security/keystore.jks";
		String truststorePath = baseDir + "/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
		util.importCertificates(keystorePath, certificates);    	
    	return util.importCertificates(truststorePath, certificates); 
				
	}

	public void setCertificatesInTruststore(String url) {

		try {
			CertificatesUtil util = new CertificatesUtil();
    		Certificate[] certificates = util.downloadCertificates(url);
    		String truststorePath = baseDir + "/security/truststore.jks";
        	util.importCertificates(truststorePath, certificates);
    	} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}
	
	
	public void deleteCertificatesInTruststore(String certificateName) {
		String truststorePath = baseDir + "/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
    	util.deleteCertificate(truststorePath, certificateName);
	}

	public void importP12Certificate(String fileP12, String passwordP12) throws Exception {

		CertificatesUtil util = new CertificatesUtil();

		String keystorePath = baseDir + "/security/keystore.jks";
		util.importP12Certificate(fileP12,passwordP12,keystorePath,"supersecret");

		//String truststorePath = baseDir + "/security/truststore.jks";
		//util.importP12Certificate(fileP12,passwordP12,truststorePath,"supersecret");

	}
	
    private SSLContextParameters createSSLContextParameters() throws GeneralSecurityException, IOException {

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    	File securityPath = new File(baseDir + "/security");
    	File trustStorePath = new File(baseDir + "/security/truststore.jks");
    	File keyStorePath = new File(baseDir + "/security/keystore.jks");

    	if(!securityPath.exists()){ 
    		securityPath.mkdirs();
    	}
    	
    	if(!trustStorePath.exists()){ 
    		try {
    			trustStorePath.createNewFile();
    			InputStream is = classloader.getResourceAsStream("truststore.jks");
    			Files.copy(is, trustStorePath.toPath(), StandardCopyOption.REPLACE_EXISTING);
        		is.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}

    	if(!keyStorePath.exists()){ 
    		try {
    			keyStorePath.createNewFile();
    			InputStream is = classloader.getResourceAsStream("keystore.jks");
    			Files.copy(is, keyStorePath.toPath(), StandardCopyOption.REPLACE_EXISTING);        	
        		is.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
        KeyStoreParameters ksp = new KeyStoreParameters();
        ksp.setResource(baseDir + "/security/keystore.jks");
        ksp.setPassword("supersecret");
		KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword("supersecret");
		kmp.setKeyStore(ksp);

        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(baseDir + "/security/truststore.jks");
        tsp.setPassword("supersecret");
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);

        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        //sslContextParameters.setTrustManagers(tmp);

		registry.bind("ssl", sslContextParameters);

		return sslContextParameters;
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
