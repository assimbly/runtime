package org.assimbly.integration.routes;

import java.util.Set;
import java.util.TreeMap;

import org.apache.camel.*;
import org.apache.camel.builder.LegacyDefaultErrorHandlerBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.integration.routes.errorhandler.ErrorHandler;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ESBRoute extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());

	TreeMap<String, String> props;

	private CamelContext context;
	private ExtendedCamelContext extendedCamelContext;

	private RoutesLoader loader;
	private LegacyDefaultErrorHandlerBuilder routeErrorHandler;

	String flowName;
	
	public ESBRoute(final TreeMap<String, String> props){
		this.props = props;
	}

	public ESBRoute() {}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		flowName = props.get("flow.name");
		
		log.info("Configuring ESBRoute flow=" + flowName);

		setExtendedCamelContext();
	
		setRouteConfiguration();

		setRoutes();

	}

	private void setExtendedCamelContext() {		
		context = getContext();
		extendedCamelContext = context.adapt(ExtendedCamelContext.class);				
	}

	private void setRouteConfiguration() throws Exception{

		for(String prop : props.keySet()){
			if(prop.endsWith("route")){				
				String route = props.get(prop);

				if(route.startsWith("<routeConfiguration")){
					loadRoute(route);					
				}else if(prop.startsWith("error")){
					if(route.isEmpty()){
						setErrorHandler("");
					}else{
						setErrorHandler(route);
					}
				}
			}
		}
	}

	private void setRoutes() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){							
				String route = props.get(prop);

				if(!route.startsWith("<routeConfiguration")){
					updateRoute(route);
				}
			}
		}
	}

	private void updateRoute(String route) throws Exception {
		loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);
		
		Set<String> updatedRoutes = loader.updateRoutes(resource);

		for(String updateRoute : updatedRoutes ){
			log.info("Loaded route: \n\n" + route + "\n\n flow=" + flowName + " routeid=" + updateRoute);
		}

	}

	private void loadRoute(String route) throws Exception {
		loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);

		try{
			loader.loadRoutes(resource);
		}catch(java.lang.IllegalArgumentException e){
			loader.updateRoutes(resource);
		}
		
		log.info("Loaded route: \n\n" + route + "\n\n flow=" + flowName);
	
	}

	private void setErrorHandler(String route) throws Exception {

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props);

		routeErrorHandler = errorHandler.configure();

		extendedCamelContext.setErrorHandlerFactory(routeErrorHandler);

	}


}