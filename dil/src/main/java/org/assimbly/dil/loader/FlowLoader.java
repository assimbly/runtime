package org.assimbly.dil.loader;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.camel.*;
import org.apache.camel.builder.*;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.dil.blocks.processors.SetHeadersProcessor;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());

	TreeMap<String, String> props;

	private CamelContext context;
	private ExtendedCamelContext extendedCamelContext;

	private RoutesLoader loader;
	private DeadLetterChannelBuilder routeErrorHandler;



	String flowId;
	String flowName;

	boolean isLoaded;

	public FlowLoader(final TreeMap<String, String> props){
		this.props = props;
	}

	public FlowLoader() {}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		flowName = props.get("flow.name");
		flowId = props.get("id");

		log.info("Configuring ESBRoute flow=" + flowName);

		setExtendedCamelContext();

		setErrorHandlers();

		setRouteConfigurations();

		defineRouteTemplates();

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
				String id = props.get(prop + ".id");

				if(routeConfiguration!=null && !routeConfiguration.isEmpty()){
					loadRoute(routeConfiguration, "routeconfiguration", id);
				}

			}
		}
	}

	//this route defines a route template
	private void defineRouteTemplates() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("routetemplatedefinition")){

				String routeTemplate = props.get(prop);
				String id = props.get(prop + ".id");
				if(routeTemplate!=null && !routeTemplate.isEmpty()){
					loadRoute(routeTemplate, "routeTemplate definition", id);
				}

			}
		}
	}

	//this route create a route template (from a routetemplate definition)
	private void setRouteTemplates() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("routetemplate")){

				String routeTemplate = props.get(prop);
				String id = props.get(prop + ".id");
				if(routeTemplate!=null && !routeTemplate.isEmpty()){
					loadRoute(routeTemplate, "routeTemplate", id);
				}

			}
		}
	}

	private void setRoutes() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){							

				String route = props.get(prop);
				String id = props.get(prop + ".id");

				if(prop.startsWith("route")){
					updateRoute(route, "route");
				}else{
					loadRoute(route, "route", id);
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


	private void loadRoute(String route, String type, String id) throws Exception {

		loader = extendedCamelContext.getRoutesLoader();

		Resource resource = IntegrationUtil.setResource(route);

		if(isLoaded(id)) {
			log.info("Updating flow=" + flowName + " | " + type +  " :\n\n" + route);
			loader.updateRoutes(resource);
			log.info("Updated flow=" + flowName + " | type=" + type);
		}else{
			log.info("Loading flow=" + flowName + " | " + type +  " :\n\n" + route);
			loader.loadRoutes(resource);
			log.info("Loaded flow=" + flowName + " | type=" + type);
		}

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

	private boolean isLoaded(String id){
		List<Route> routes = context.getRoutes().stream().filter(r -> r.getId().equals(id)).collect(Collectors.toList());
		if(routes.size()>0){
			return true;
		}

		return false;
	}

}