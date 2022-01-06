package org.assimbly.integration.routes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.builder.DefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;

import org.assimbly.integration.routes.errorhandler.ErrorHandler;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ESBRoute extends RouteBuilder {

	TreeMap<String, String> props;
	
	private ManagedCamelContextMBean managedContext;
	
	private DefaultErrorHandlerBuilder routeErrorHandler;
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.integration.routes.ESBRoute");
	
	private String flowId;
	private String flowName;

	private List<String> errorUriKeys;

	
	
	public ESBRoute(final TreeMap<String, String> props){
		this.props = props;
	}

	public ESBRoute() {}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		flowId = props.get("id");
		errorUriKeys = getUriKeys("error");	

		//ErrorHandler errorHandler = new ErrorHandler(props, errorUriKeys);
		//routeErrorHandler = errorHandler.setErrorHandler();

		setManagedContext();
		
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){
				addXmlRoute(prop);				
			}
		}
	}

	private void setManagedContext() {
		CamelContext context = getContext();
		ManagedCamelContext managed = context.getExtension(ManagedCamelContext.class);
		managedContext = managed.getManagedCamelContext();
	}
	
	private void addXmlRoute(String xml) throws Exception {
		managedContext.addOrUpdateRoutesFromXml(xml);
	}

	private List<String> getUriKeys(String endpointType) {

		List<String> keys = new ArrayList<>();

		for(String prop : props.keySet()){
			if(prop.startsWith(endpointType) && prop.endsWith("uri")){
				keys.add(prop);
			}
		}

		return keys;

	}
}