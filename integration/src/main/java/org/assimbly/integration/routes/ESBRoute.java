package org.assimbly.integration.routes;

import java.util.Set;
import java.util.TreeMap;

import org.apache.camel.*;
import org.apache.camel.builder.*;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
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
	private DeadLetterChannelBuilder routeErrorHandler;

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

		setErrorHandlers();

		setRouteConfigurations();

		setRouteTemplates();

		setRoutes();

	}

	private void setExtendedCamelContext() {		
		context = getContext();
		extendedCamelContext = context.adapt(ExtendedCamelContext.class);
	}

	private void setErrorHandlers() throws Exception{

		for(String prop : props.keySet()){
			if(prop.startsWith("error") && prop.endsWith("uri")){
				String errorUri = props.get(prop);
				setErrorHandler(errorUri);
			}
		}

	}

	private void setRouteConfigurations() throws Exception{

		for(String prop : props.keySet()){
			if(prop.endsWith("routeconfiguration")){
				String routeConfiguration = props.get(prop);
				loadRoute(routeConfiguration, "routeconfiguration");
			}
		}
	}
	private void setRouteTemplates() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("routetemplate")){

				String routeTemplate = props.get(prop);

				loadRoute(routeTemplate, "routeTemplate");

			}
		}
	}

	private void setRoutes() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){							

				String route = props.get(prop);

				if(prop.startsWith("route")){
					updateRoute(route, "route");
				}else{
					loadRoute(route, "route");
				}

			}
		}
	}

	private void updateRoute(String route, String type) throws Exception {
		loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);

		log.info("Updating " + type +": \n\n" + route + "\n\n flow=" + flowName);

		Set<String> updatedRoutes = loader.updateRoutes(resource);

		for(String updateRoute : updatedRoutes ){
			log.info("Updated " + type + " | flow=" + flowName + " routeid=" + updateRoute);
		}

	}


	private void loadRoute(String route, String type) throws Exception {
		loader = extendedCamelContext.getRoutesLoader();
		Resource resource = IntegrationUtil.setResource(route);

		log.info("Loading " + type +  ": \n\n" + route + "\n\n flow=" + flowName);

		try{
			loader.loadRoutes(resource);
		}catch(java.lang.IllegalArgumentException e){
			loader.updateRoutes(resource);
		}
		
		log.info("Loaded " + type + " | flow=" + flowName);
	
	}

	private void setErrorHandler(String errorUri) throws Exception {

		if (errorUri!=null && !errorUri.isEmpty()) {
			routeErrorHandler = new DeadLetterChannelBuilder();
			routeErrorHandler = deadLetterChannel(errorUri);
		}else{
			routeErrorHandler = deadLetterChannel("log:org.assimbly.integration.routes.ESBRoute?level=ERROR");
		}

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props);

		routeErrorHandler = errorHandler.configure();

		extendedCamelContext.setErrorHandlerFactory(routeErrorHandler);

	}

}