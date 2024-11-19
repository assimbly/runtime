package org.assimbly.dil.loader;

import java.util.*;

import org.apache.camel.*;
import org.apache.camel.builder.*;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteConfigurationDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private TreeMap<String, String> props;
	private CamelContext context;
	private RoutesLoader loader;
	private RoutesBuilderLoader routesBuilderLoader;
	private DeadLetterChannelBuilder routeErrorHandler;
	private String flowId;
	private String flowName;
	private String flowEvent;
	private String flowVersion;
	private String flowEnvironment;
	private boolean isFlowLoaded = true;
	private FlowLoaderReport flowLoaderReport;

	public FlowLoader(final TreeMap<String, String> props, FlowLoaderReport flowLoaderReport){
		super();
		this.props = props;
		this.flowLoaderReport = flowLoaderReport;
	}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		init();

		load();

		finish();

	}

	private void init() throws Exception {

		flowId = props.get("id");
		flowName = props.get("flow.name");
		flowVersion = props.get("flow.version");
		flowEnvironment = props.get("environment");
		flowEvent = "start";

		if(flowLoaderReport==null){
			flowLoaderReport = new FlowLoaderReport();
			flowLoaderReport.initReport(flowId, flowName, "start");
		}

		setExtendedcontext();

	}

	private void load() throws Exception {

		setErrorHandlers();

		setRouteConfigurations();

		defineRouteTemplates();

		setRouteTemplates();

		setRoutes();

	}


	private void finish() {

		flowLoaderReport.logResult(flowId,flowName,flowEvent);

		if (isFlowLoaded){
			flowLoaderReport.finishReport(flowId, flowName, flowEvent, flowVersion, flowEnvironment, "Started flow successfully");
		}else{
			flowLoaderReport.finishReport(flowId, flowName, flowEvent, flowVersion, flowEnvironment, "Failed to load flow");
		}
	}

	private void setExtendedcontext() throws Exception {
		context = getContext();
		loader = PluginHelper.getRoutesLoader(context);
		routesBuilderLoader = loader.getRoutesLoader("xml");
	}

	private void setErrorHandlers() throws Exception{

		String errorUri = "";
		String id = "0";
		Boolean useErrorHandler = true;

		for(String prop : props.keySet()){
			if(prop.startsWith("error") && prop.endsWith("uri")){
				errorUri = props.get(prop);
				id = StringUtils.substringBetween(prop,"error.",".uri");
				if(props.containsKey("error." + id + ".route") && props.containsKey("error." + id + ".routeconfiguration")){
					useErrorHandler = false;
				}
			}
		}

		if(useErrorHandler) {
			setErrorHandler(id, errorUri);
		}else{
			log.warn("ErrorHandler is not set");
		}

	}

	private void setRouteConfigurations() throws Exception{

		removeRouteConfiguration(flowId);

		for(String prop : props.keySet()){
			if(prop.endsWith("routeconfiguration")){

				String routeConfiguration = props.get(prop);
				String id = props.get(prop + ".id");

				if(routeConfiguration!=null && !routeConfiguration.isEmpty()){
					loadStep(routeConfiguration, "routeconfiguration", id, null);
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
					loadStep(routeTemplate, "routeTemplate definition", id, null);
				}

			}
		}
	}

	//this route create a route template (from a routetemplate definition)
	private void setRouteTemplates() throws Exception{

		props.forEach((key, value) -> {
			if (key.endsWith("routetemplate")) {
				try {
					String routeTemplate = value;
					String basePath = StringUtils.substringBefore(key,"routetemplate");
					String id = props.get(basePath + "routetemplate.id");
					String uri = props.get(basePath + "uri");

					loadStep(routeTemplate, "routeTemplate", id, uri);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

	}

	private void setRoutes() throws Exception{

		for(String key : props.descendingKeySet()){
			if(key.endsWith("route")){
				String route = props.get(key);
				String id = props.get(key + ".id");
				loadRoute(route, "route",id);
			}
		}

	}


	private void loadRoute(String route, String type, String id) throws Exception {

		try {

			Resource resource = IntegrationUtil.setResource(route);
			RoutesBuilder builder = routesBuilderLoader.loadRoutesBuilder(resource);
			context.addRoutes(builder);

			flowLoaderReport.setStep(id, null, type, "success", null);

		}catch (Exception e) {
			String errorMessage = e.getMessage();
			log.error("Failed loading step | stepid=" + id);
			flowLoaderReport.setStep(id, null, type, "error", errorMessage);
			flowEvent = "error";
			isFlowLoaded = false;
		}
	}

	private void loadStep(String step, String type, String id, String uri) throws Exception {

		try {

			log.info("Load step:\n\n" + step);

			loader.loadRoutes(IntegrationUtil.setResource(step));

			flowLoaderReport.setStep(id, uri, type, "success", null);

		}catch (Exception e) {
			String errorMessage = e.getMessage();
			log.error("Failed loading step | stepid=" + id);
			flowLoaderReport.setStep(id, uri, type, "error", errorMessage);
			flowEvent = "error";
			isFlowLoaded = false;
		}
	}

	private void setErrorHandler(String id, String errorUri) throws Exception {

		if (errorUri!=null && !errorUri.isEmpty()) {
			routeErrorHandler = new DeadLetterChannelBuilder(errorUri);
		}else{
			routeErrorHandler = deadLetterChannel("log:org.assimbly.integration.routes.ESBRoute?level=ERROR");
		}

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props, flowId);

		DeadLetterChannelBuilder updatedErrorHandler = errorHandler.configure();

		context.getCamelContextExtension().setErrorHandlerFactory(updatedErrorHandler);

		flowLoaderReport.setStep(id, errorUri, "error", "success", null);

	}

	private void removeRouteConfiguration(String flowId) {

		ModelCamelContext modelContext = (ModelCamelContext) context;

		List<RouteConfigurationDefinition> routeConfigurationsToRemove = modelContext.getRouteConfigurationDefinitions().stream()
				.filter(routeConfig -> routeConfig.getId().startsWith(flowId))
				.toList(); // Collect into a new list to avoid modifying the original list during iteration

		routeConfigurationsToRemove.forEach(routeConfig -> {
			try {
				modelContext.removeRouteConfiguration(routeConfig);
				log.info("Removed routeConfiguration: " + routeConfig.getId());
			} catch (Exception e) {
				log.warn("Failed to remove route configuration: " + routeConfig.getId());
			}
		});

	}

	public String getReport(){
		return flowLoaderReport.getReport();
	}

	public boolean isFlowLoaded(){
		return isFlowLoaded;
	}

}