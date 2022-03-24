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
	
	public ESBRoute(final TreeMap<String, String> props){
		this.props = props;
	}

	public ESBRoute() {}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		System.out.println("Configure ESB Route");
	
		flowId = props.get("id");
		
		setManagedContext();
		
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){
				System.out.println("prop=" + prop);
				String xml = props.get(prop);
				addXmlRoute(xml);				
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

}