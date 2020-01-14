package org.assimbly.connector.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.cert.Certificate;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.EndpointValidationResult;
import org.apache.camel.component.http4.HttpComponent;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.EventNotifier;
import org.apache.camel.spi.RouteError;
import org.apache.camel.SSLContextParametersAware;
import org.apache.camel.util.jsse.KeyManagersParameters;
import org.apache.camel.util.jsse.KeyStoreParameters;
import org.apache.camel.util.jsse.SSLContextParameters;
import org.apache.camel.util.jsse.TrustManagersParameters;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

import org.assimbly.connector.connect.util.CertificatesUtil;
import org.assimbly.connector.event.EventCollector;
import org.assimbly.connector.routes.DefaultRoute;
import org.assimbly.connector.routes.PollingJdbcRoute;
import org.assimbly.connector.routes.SimpleRoute;
import org.assimbly.connector.service.Connection;
import org.assimbly.docconverter.DocConverter;


public class CamelConnector extends BaseConnector {

	private CamelContext context;
	private ProducerTemplate template;
	private boolean started = false;
	private int stopTimeout = 30;
	private ServiceStatus status;
	private String flowStatus;
	private String flowUptime;

	private String flowStats;
	private String connectorStats;
	private MetricRegistry metricRegistry = new MetricRegistry();

	private String flowInfo;

	private final String userHomeDir = System.getProperty("user.home");
	
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
		SimpleRegistry registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		context.setStreamCaching(true);
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);

		//set HTTP as scheme (instead of HTTP4 and HTTPS4)
		context.addComponent("http", new HttpComponent());
		context.addComponent("https", new HttpComponent());

		//setting transport security globally
        context.setSSLContextParameters(createSSLContextParameters());
        ((SSLContextParametersAware) context.getComponent("ftps")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("https")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("https4")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("imaps")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("kafka")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("netty4")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("smtps")).setUseGlobalSslContextParameters(true);
        ((SSLContextParametersAware) context.getComponent("sftp")).setUseGlobalSslContextParameters(true);
        
		//set default metrics
		context.addRoutePolicyFactory(new MetricsRoutePolicyFactory());

		//set history metrics
	    MetricsMessageHistoryFactory factory = new MetricsMessageHistoryFactory();
	    factory.setPrettyPrint(true);
	    factory.setMetricsRegistry(metricRegistry);
		context.setMessageHistoryFactory(factory);
		
		//collect events
		context.getManagementStrategy().addEventNotifier(new EventCollector());

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
				context.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
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
	
	
	public void addFlow(TreeMap<String, String> props) throws Exception {
		
		//create connections if needed
		for (String key : props.keySet()){
			if (key.endsWith("service.id")){
				props = new Connection(context, props, key).start();
			}
		}
		
		//set up route by type
		String route  = props.get("flow.type");
		if (route == null){
			logger.info("Add default flow");
			addDefaultFlow(props);
		}else if(route.equals("default")){
			logger.info("Add default flow");
			addDefaultFlow(props);			
		}else if(route.equals("simple")){
			logger.info("Add simple flow");
			addDefaultFlow(props);			
		}else if(route.equals("fromJdbcTimer")){
			logger.info("Add scheduled flow");
			addFlowFromJdbcTimer(props);
		}
		else{
			logger.info("Invalid route.");
		}
	}
	
	
	public void addDefaultFlow(final TreeMap<String, String> props) throws Exception {
		DefaultRoute flow = new DefaultRoute(props);
		context.addRoutes(flow);		
	}

	public void addSimpleFlow(final TreeMap<String, String> props) throws Exception {
		context.addRoutes(new SimpleRoute(props));
	}
	
	public void addFlowFromJdbcTimer(final TreeMap<String, String> props) throws Exception {
		context.addRoutes(new PollingJdbcRoute(props));
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
				if (route.getId().equals(id)) {
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
		logger.info("Start flow " + id);
		
		boolean flowAdded = false;
		
		try {

			List<TreeMap<String, String>> allProps = super.getConfiguration();
			
			for(int i = 0; i < allProps.size(); i++){
				TreeMap<String, String> props = allProps.get(i);
			
				if (props.get("id").equals(id)) {
					
					logger.info("Adding route with id: " + id);
					addFlow(props);
					flowAdded = true;
				}
			
			}
			
			if(flowAdded){
				context.startRoute(id);
				
				int count = 1;
				
		        do {
		        	status = context.getRouteStatus(id);
		        	if(status.isStarted()) {break;}
		        	Thread.sleep(10);
		        	count++;
		        	
		        } while (status.isStarting() || count < 3000);
		
				logger.info("Started flow " + id);
				return status.toString().toLowerCase();
				
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
			
		}catch (Exception e) {
			stopFlow(id);
			e.printStackTrace();
			return e.getMessage();
		}
	}

	public String restartFlow(String id) {

		logger.info("Restart flow " + id);
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
		logger.info("Stop flow " + id);		
		try {		

			for (Route route : context.getRoutes()) {
				if(route.getId().equals(id) || route.getId().startsWith(id + "-")) {
					context.stopRoute(route.getId(), stopTimeout, TimeUnit.SECONDS);
					context.removeRoute(route.getId());	
				}
			}

	        return "stopped";

		}catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

	}

	public String pauseFlow(String id) {
		logger.info("Pause flow " + id);		
		
		try {
		
			if(hasFlow(id)) {
	        	status = context.getRouteStatus(id);
				if(status.isSuspendable()) {
					
					context.suspendRoute(id);
						
					int count = 1;
					
			        do {
			        	status = context.getRouteStatus(id);
			        	if(status.isSuspended()) {
							logger.info("Paused (suspend) flow " + id);		
			        		break;
			        	}else if(status.isStopped()){
							logger.info("Paused (stopped) flow " + id);		

			        		break;			        		
			        	}
			        	
			        	Thread.sleep(10);
			        	count++;
			        	
			        } while (status.isSuspending() || count < 6000);
			  
			        return status.toString().toLowerCase();
				
					
				}else {
					return "Flow isn't suspendable";
				}
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
		
		}catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

		
	}

	public String resumeFlow(String id) throws Exception {
		logger.info("Resume flow " + id);
		
		try {
		
			if(hasFlow(id)) {
	        	status = context.getRouteStatus(id);
				if(status.isSuspended()) {
					
					context.resumeRoute(id);
					
					int count = 1;
					
			        do {
			        	status = context.getRouteStatus(id);
			        	if(status.isStarted()) {break;}
			        	Thread.sleep(10);
			        	count++;
			        	
			        } while (status.isStarting() || count < 3000);
			        
			        logger.info("Resumed flow " + id);
			        return status.toString().toLowerCase();				
				}else if(status.isStopped()) {
					logger.info("Starting flow as flow " + id + " is currently stopped (not suspended)");
					return startFlow(id);
				}else {
					return "Flow isn't suspended (nothing to resume)";
				}
			}else {
				return "Configuration is not set (use setConfiguration or setFlowConfiguration)";
			}
		
		}catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		}

		
	}	
	
	public String getFlowStatus(String id) {
		
		if(hasFlow(id)) {
			ServiceStatus status = context.getRouteStatus(id);
			flowStatus = status.toString().toLowerCase();		
		}else {
			flowStatus = "unconfigured";			
		}
		
		return flowStatus;
		
	}

	public String getFlowUptime(String id) {
	
		if(hasFlow(id)) {
			Route route = context.getRoute(id);
			flowUptime = route.getUptime();
		}else {
			flowUptime = "0";
		}
				
		return flowUptime;
	}

	public String getFlowLastError(String id) {
		
		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			RouteError lastError = route.getLastError();
			if(lastError!=null) {
				flowInfo = lastError.toString();
			}else {
				flowInfo = "0";
			}
			
		}else {
			flowInfo = "0";
		}

		return flowInfo;
	}
	
	
	public String getFlowTotalMessages(String id) throws Exception {

		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long totalMessages = route.getExchangesTotal();
			flowInfo = Long.toString(totalMessages);
		}else {
			flowInfo = "0";
		}

		return flowInfo;

	}
	
	public String getFlowCompletedMessages(String id) throws Exception {

		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long completedMessages = route.getExchangesCompleted();
			flowInfo = Long.toString(completedMessages);
		}else {
			flowInfo = "0";
		}

		return flowInfo;

	}

	public String getFlowFailedMessages(String id) throws Exception  {
						
		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long failedMessages = route.getExchangesFailed();
			flowInfo = Long.toString(failedMessages);
		}else {
			flowInfo = "0";
		}

		return flowInfo;

	}

	public String getFlowAlertsLog(String id, Integer numberOfEntries) throws Exception  {
		  
		  Date date = new Date();
		  String today = new SimpleDateFormat("yyyyMMdd").format(date);
		  File file = new File(userHomeDir + "/.assimbly/logs/alerts/" + id + "/" + today + "_alerts.log");
		
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
		  File file = new File(userHomeDir + "/.assimbly/logs/alerts/" + id + "/" + today + "_alerts.log");
		
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
		  File file = new File(userHomeDir + "/.assimbly/logs/events/" + id + "/" + today + "_events.log");
		
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
	

	public String getFlowStats(String id, String mediaType) throws Exception {
		
		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);

		flowStatus = getFlowStatus(id);
		
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
		
		String parameters = context.explainComponentJson(componentType, true);
		
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
	
	public Object getContext() {		
		return context;		
	}	
	
	public void send(Object messageBody, ProducerTemplate template) {
		template.sendBody(messageBody);
	}

	public void sendWithHeaders(Object messageBody,
			TreeMap<String, Object> messageHeaders, ProducerTemplate template) {
		template.sendBodyAndHeaders(messageBody, messageHeaders);
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
		String truststorePath = userHomeDir + "/.assimbly/security/truststore.jks";
		CertificatesUtil util = new CertificatesUtil();
    	return util.getCertificate(truststorePath, certificateName); 
	}	


	public String importCertificateInTruststore(String certificateName, Certificate certificate) {

		String keystorePath = userHomeDir + "/.assimbly/security/keystore.jks";
		String truststorePath = userHomeDir + "/.assimbly/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
		util.importCertificate(keystorePath, certificateName,certificate);    	
    	return util.importCertificate(truststorePath, certificateName,certificate); 
				
	}

	
	public Map<String,Certificate> importCertificatesInTruststore(Certificate[] certificates) {

		String keystorePath = userHomeDir + "/.assimbly/security/keystore.jks";
		String truststorePath = userHomeDir + "/.assimbly/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
		util.importCertificates(keystorePath, certificates);    	
    	return util.importCertificates(truststorePath, certificates); 
				
	}

	public void setCertificatesInTruststore(String url) {

		try {
			CertificatesUtil util = new CertificatesUtil();
    		Certificate[] certificates = util.downloadCertificates(url);
    		String truststorePath = userHomeDir + "/.assimbly/security/truststore.jks";
        	util.importCertificates(truststorePath, certificates);
    	} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}			
	}
	
	
	public void deleteCertificatesInTruststore(String certificateName) {
		String truststorePath = userHomeDir + "/.assimbly/security/truststore.jks";
		
		CertificatesUtil util = new CertificatesUtil();
    	util.deleteCertificate(truststorePath, certificateName);
	}
	
	
    private SSLContextParameters createSSLContextParameters() {

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();

    	File securityPath = new File(userHomeDir + "/.assimbly/security");
    	File trustStorePath = new File(userHomeDir + "/.assimbly/security/truststore.jks");
    	File keyStorePath = new File(userHomeDir + "/.assimbly/security/keystore.jks");

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
        ksp.setResource(userHomeDir + "/.assimbly/security/keystore.jks");
        ksp.setPassword("supersecret");
        KeyManagersParameters kmp = new KeyManagersParameters();
        kmp.setKeyPassword("secret");
        kmp.setKeyStore(ksp);

        KeyStoreParameters tsp = new KeyStoreParameters();
        tsp.setResource(userHomeDir + "/.assimbly/security/truststore.jks");
        tsp.setPassword("supersecret");      
        TrustManagersParameters tmp = new TrustManagersParameters();
        tmp.setKeyStore(tsp);
        
        
        SSLContextParameters sslContextParameters = new SSLContextParameters();
        sslContextParameters.setKeyManagers(kmp);
        sslContextParameters.setTrustManagers(tmp);
                
        return sslContextParameters;
    }
    
}
