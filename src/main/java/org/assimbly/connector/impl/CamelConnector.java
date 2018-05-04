package org.assimbly.connector.impl;

import java.net.URI;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.api.management.mbean.ManagedRouteMBean;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryFactory;
import org.apache.camel.component.metrics.messagehistory.MetricsMessageHistoryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRegistryService;
import org.apache.camel.component.metrics.routepolicy.MetricsRoutePolicyFactory;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.spi.RouteError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;

import org.assimbly.connector.connect.util.ConnectorUtil;
import org.assimbly.connector.routes.DefaultRoute;
import org.assimbly.connector.routes.PollingJdbcRoute;
import org.assimbly.connector.routes.SimpleRoute;
import org.assimbly.connector.service.Connection;

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
	private String flowStatsError;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.CamelConnector");

   
	public CamelConnector() {

	}
	
	public CamelConnector(String connectorId, String configuration) throws Exception {
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}

	public CamelConnector(String connectorId, URI configuration) throws Exception {
		setFlowConfiguration(convertXMLToFlowConfiguration(connectorId, configuration));
	}
	
	public void start() throws Exception {

		SimpleRegistry registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		context.setStreamCaching(true);
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
		
		//set default metrics
		context.addRoutePolicyFactory(new MetricsRoutePolicyFactory());

		//set history metrics
	    MetricsMessageHistoryFactory factory = new MetricsMessageHistoryFactory();
	    factory.setPrettyPrint(true);
	    factory.setMetricsRegistry(metricRegistry);
		context.setMessageHistoryFactory(factory);
		
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
		}
	}	

	public boolean isStarted() {
		return started;
	}
	
	
	public void addFlow(TreeMap<String, String> props) throws Exception {
		
		//create connections if needed
		for (String key : props.keySet()){
			if (key.contains("service.id")){
				props = new Connection(context, props).start();
			}
		}
		
		//create arraylist from touri
		String toUri = props.get("to.uri");
		String[] toUriArray = toUri.split(",");
		
		//set first to endpoint asdefault
		template = context.createProducerTemplate();
		template.setDefaultEndpointUri(toUriArray[0]);
		
		
		//set up route by type
		String route  = props.get("route");
		if (route == null){
			logger.info("add default route");
			addDefaultFlow(props);
		}else if(route.equals("default")){
			logger.info("add default route");
			addDefaultFlow(props);			
		}else if(route.equals("simple")){
			logger.info("add simple route");
			addDefaultFlow(props);			
		}else if(route.equals("fromJdbcTimer")){
			addFlowFromJdbcTimer(props);
		}
		else{
			logger.info("Invalid route.");
		}
	}

	public void addDefaultFlow(final TreeMap<String, String> props) throws Exception {
		logger.info("add default flow");
		context.addRoutes(new DefaultRoute(props));
	}

	public void addSimpleFlow(final TreeMap<String, String> props) throws Exception {
		logger.info("add simple flow");
		context.addRoutes(new SimpleRoute(props));
	}
	
	public void addFlowFromJdbcTimer(final TreeMap<String, String> props)	throws Exception {
		logger.info("add jdbc flow");
		context.addRoutes(new PollingJdbcRoute(props));
	}

	public boolean removeFlow(String id) throws Exception {
		return context.removeRoute(id);
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

	public String startFlow(String id) throws Exception {
		if(!hasFlow(id)) {
			for (TreeMap<String, String> props : super.getConfiguration()) {
				if (props.get("id").equals(id)) {
					logger.info("Adding route with ids: " + id);
					addFlow(props);
				}
			}
		}

		context.startRoute(id);

		int count = 1;
		
        do {
        	status = context.getRouteStatus(id);
        	if(status.isStarted()) {break;}
        	Thread.sleep(10);
        	count++;
        	
        } while (status.isStarting() || count < 3000);

		return status.toString().toLowerCase();
	}

	public String restartFlow(String id) throws Exception {
				
		stopFlow(id);
		
		int count = 1;
		
        do {
        	status = context.getRouteStatus(id);
        	if(status.isStopped()) {break;}
        	Thread.sleep(10);
        	count++;
        	
        } while (status.isStopping() || count < 3000);
		
        if(count==3000) {
			logger.error("Timed out after 30 seconds while stopping route with id: " + id);
			return "Timed out after 30 seconds while stopping route with id: " + id;
        }else {
        	return startFlow(id);	
        }	
	}
	
	public String stopFlow(String id) throws Exception {
		if(hasFlow(id)) {
        	status = context.getRouteStatus(id);
			if(status.isStoppable()) {
				context.stopRoute(id);	
			}
			
			int count = 1;
			
	        do {
	        	status = context.getRouteStatus(id);
	        	if(status.isStopped()) {break;}
	        	Thread.sleep(10);
	        	count++;
	        	
	        } while (status.isStopping() || count < 3000);
	        
	        return status.toString().toLowerCase().toLowerCase();
			
		}else {
			return "Configuration not set";
		}
		
	}

	public String pauseFlow(String id) throws Exception {
		if(hasFlow(id)) {
        	status = context.getRouteStatus(id);
			if(status.isSuspendable()) {
				context.suspendRoute(id);
				
				int count = 1;
				
		        do {
		        	status = context.getRouteStatus(id);
		        	if(status.isSuspended()) {break;}
		        	Thread.sleep(10);
		        	count++;
		        	
		        } while (status.isSuspending() || count < 3000);
		        
		        return status.toString().toLowerCase();
			
				
			}else {
				return "Flow isn't suspendable";
			}
		}else {
			return "Configuration not set";
		}
	}

	public String resumeFlow(String id) throws Exception {
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
		        
		        return status.toString().toLowerCase();				
			}else {
				return "Flow isn't suspended (nothing to resume)";
			}
		}else {
			return "Configuration not set";
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
			flowStatsError = lastError.toString();
		}else {
			flowStatsError = "0";
		}

		return flowStatsError;
	}
	
	
	public String getFlowTotalMessages(String id) throws Exception {

		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long totalMessages = route.getExchangesTotal();
			flowStats = Long.toString(totalMessages);
		}else {
			flowStats = "0";
		}

		return flowStats;

	}
	
	
	public String getFlowCompletedMessages(String id) throws Exception {

		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long completedMessages = route.getExchangesCompleted();
			flowStats = Long.toString(completedMessages);
		}else {
			flowStats = "0";
		}

		return flowStats;

	}

	public String getFlowFailedMessages(String id) throws Exception  {

		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			long failedMessages = route.getExchangesFailed();
			flowStats = Long.toString(failedMessages);
		}else {
			flowStats = "0";
		}

		return flowStats;

	}
	
	public String getFlowStats(String id, String mediaType) throws Exception {
		
		ManagedRouteMBean route = context.getManagedRoute(id, ManagedRouteMBean.class);
		
		if(route!=null) {
			flowStats = route.dumpStatsAsXml(true);
			if(mediaType.contains("json")) {
				flowStats = ConnectorUtil.convertXmlToJson(flowStats);
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
					connectorStats = ConnectorUtil.convertJsonToXml(connectorStats);
				}
			}else {
				connectorStats = "0";
			}
		}else {
			MetricsRegistryService metricsService = context.hasService(MetricsRegistryService.class);
			
			if(metricsService!=null) {
				connectorStats = metricsService.dumpStatisticsAsJson();
				if(mediaType.contains("xml")) {
					connectorStats = ConnectorUtil.convertJsonToXml(connectorStats);
				}
			}else {
				connectorStats = "0";
			}
		}
		
		return connectorStats;

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

}
