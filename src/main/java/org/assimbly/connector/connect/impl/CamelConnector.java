package org.assimbly.connector.connect.impl;


import java.net.URI;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.assimbly.connector.connect.Connection;
import org.assimbly.connector.routes.DefaultRoute;
import org.assimbly.connector.routes.PollingJdbcRoute;

public class CamelConnector extends BaseConnector {

	private CamelContext context;
	private ProducerTemplate template;
	private boolean started = false;
	private int stopTimeout = 30;
	private ServiceStatus status;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.camelconnector.connect.impl.CamelConnector");

	public CamelConnector() {

	}
	
	public CamelConnector(String connectorID, String configuration) throws Exception {
		setRouteConfiguration(convertXMLToRouteConfiguration(connectorID, configuration));
	}

	public CamelConnector(String connectorID, URI configuration) throws Exception {
		setRouteConfiguration(convertXMLToRouteConfiguration(connectorID, configuration));
	}
	
	
	public void start() throws Exception {

		SimpleRegistry registry = new SimpleRegistry();
		context = new DefaultCamelContext(registry);
		context.setStreamCaching(true);
		context.getShutdownStrategy().setSuppressLoggingOnTimeout(true);
		
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
	
	
	public void addRoute(TreeMap<String, String> props) throws Exception {
		for (String key : props.keySet()){
			if (key.contains("connection_id")){
				props = new Connection(context, props).start();
			}
		}
		template = context.createProducerTemplate();
		template.setDefaultEndpointUri(props.get("to.uri"));
		String route  = props.get("route");
		if (route == null){
			logger.info("add default route");
			addDefaultRoute(props);
		}else if(route.equals("default")){
			logger.info("add default route");
			addDefaultRoute(props);			
		}else if(route.equals("fromJdbcTimer")){
			addRouteFromJdbcTimer(props);
		}
		else{
			logger.info("Invalid route.");
		}
	}

	public void addDefaultRoute(final TreeMap<String, String> props) throws Exception {
		logger.info("defaultroutes");
		context.addRoutes(new DefaultRoute(props));
	}
		
	public void addRouteFromJdbcTimer(final TreeMap<String, String> props)	throws Exception {
		context.addRoutes(new PollingJdbcRoute(props));
		
	}

	public boolean hasRoute(String id) {
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


	public void removeRoute(String id) throws Exception {
		context.removeRoute(id);
	}

	public void startRoute(String id) throws Exception {
		if(!hasRoute(id)) {
			for (TreeMap<String, String> props : super.getConfiguration()) {
				if (props.get("id").equals(id)) {
					logger.info("Adding route with ids: " + id);
					addRoute(props);
				}
			}
		}

		context.startRoute(id);
	}

	public void restartRoute(String id) throws Exception {
				
		stopRoute(id);
		
		int count = 1;
		
        do {
        	status = context.getRouteStatus(id);
        	Thread.sleep(10);
        	count++;
        	
        } while (status.isStopping() || count < 3000);
		
        if(count==3000) {
			logger.error("Timed out after 30 seconds while stopping route with id: " + id);
        }else {
        	startRoute(id);	
        }	
	}
	
	public void stopRoute(String id) throws Exception {
		if(hasRoute(id)) {
        	status = context.getRouteStatus(id);
			if(status.isStoppable()) {
				context.stopRoute(id);	
			}
		}
	}

	public void pauseRoute(String id) throws Exception {
		if(hasRoute(id)) {
        	status = context.getRouteStatus(id);
			if(status.isSuspendable()) {
				context.suspendRoute(id);	
			}
		}
	}

	public void resumeRoute(String id) throws Exception {
		if(hasRoute(id)) {
        	status = context.getRouteStatus(id);
			if(status.isSuspended()) {
				context.resumeRoute(id);	
			}
		}
	}	
	
	public String getRouteStatus(String id) {
	
		ServiceStatus status = context.getRouteStatus(id);
		return status.toString();
		
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
