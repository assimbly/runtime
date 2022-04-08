package org.assimbly.integration.routes;

import java.util.Set;
import java.util.TreeMap;

import org.apache.camel.*;
import org.apache.camel.api.management.ManagedCamelContext;
import org.apache.camel.api.management.mbean.ManagedCamelContextMBean;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.ResourceHelper;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ESBRoute extends RouteBuilder {

	TreeMap<String, String> props;
	private String logLevelAsString;
	
	private ManagedCamelContextMBean managedContext;
	private RoutesLoader loader;
	
	private static Logger logger = LoggerFactory.getLogger("org.assimbly.integration.routes.ESBRoute");
	
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

		String flowName = props.get("flow.name");
		
		setLogLevel();
		
		setManagedContext();
		
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){
				System.out.println("prop=" + prop);
				String xml = props.get(prop);
				addRoute(xml);				
			}
		}
	}

	private void setManagedContext() {
		
		CamelContext context = getContext();
		ExtendedCamelContext extendedCamelContext = context.adapt(ExtendedCamelContext.class);
		loader = extendedCamelContext.getRoutesLoader();
					
		ManagedCamelContext managed = context.getExtension(ManagedCamelContext.class);
		managedContext = managed.getManagedCamelContext();
	}

	private void setLogLevel(){
		if (this.props.containsKey("flow.logLevel")){
			logLevelAsString = props.get("flow.logLevel");
		}else {
			logLevelAsString = "OFF";
		}



		CamelContext context = getContext();
		Route x = context.getRoute("any");
		
		//x.setTracing(true);
		//x.setDebugging(debugging);

		context.getRouteController().setLoggingLevel(LoggingLevel.valueOf(logLevelAsString));	

	}

	private void addRoute(String route) throws Exception {
		System.out.println("load route from xml");
		Resource resource = setResource(route);		
		Set<String> x = loader.updateRoutes(resource);
		for(String y : x ){
			System.out.println("route=" + y);
		}
		//managedContext.addOrUpdateRoutesFromXml(xml);
	}

	private Resource setResource(String route){
		if(IntegrationUtil.isXML(route)){
			return ResourceHelper.fromString("route.xml", route); 
		}else if(IntegrationUtil.isYaml(route)){
			return ResourceHelper.fromString("route.yaml", route); 
		}else{
			log.warn("unknown route format");
			return ResourceHelper.fromString("route.xml", route); 
		}		
	}

}