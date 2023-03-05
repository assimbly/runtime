package org.assimbly.dil.loader;

import java.util.List;
import java.util.TreeMap;
import org.apache.camel.*;
import org.apache.camel.builder.*;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FlowLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());

	private TreeMap<String, String> props;

	private CamelContext context;
	private ExtendedCamelContext extendedCamelContext;

	private RoutesLoader loader;
	private DeadLetterChannelBuilder routeErrorHandler;
	private String flowId;
	private String flowName;
	private String flowEvent;
	private String flowVersion;
	private String flowEnvironment;

	private boolean isFlowLoaded = true;

	private FlowLoaderReport flowLoaderReport;

	public FlowLoader(final TreeMap<String, String> props){
		super();
		this.props = props;
	}

	public FlowLoader() {
		super();
	}

	public interface FailureProcessorListener {
		public void onFailure();
	}

	@Override
	public void configure() throws Exception {

		init();

		loadFlowSteps();

		finish();
	}

	private void init(){

		flowId = props.get("id");
		flowName = props.get("flow.name");
		flowVersion = props.get("flow.version");
		flowEnvironment = props.get("environment");

		flowLoaderReport = new FlowLoaderReport();

		flowLoaderReport.initReport(flowId, flowName, "start");

		setExtendedCamelContext();

	}

	private void finish() {

		flowLoaderReport.logResult(flowId,flowName,flowEvent);

		if (isFlowLoaded){
			flowLoaderReport.finishReport(flowId, flowName, flowEvent, flowVersion, flowEnvironment, "Started flow successfully");
		}else{
			flowLoaderReport.finishReport(flowId, flowName, flowEvent, flowVersion, flowEnvironment, "Failed to load flow");
		}
	}

	private void setExtendedCamelContext() {
		context = getContext();
		extendedCamelContext = context.adapt(ExtendedCamelContext.class);
	}



	private void loadFlowSteps() throws Exception {

		setErrorHandlers();

		setRouteConfigurations();

		defineRouteTemplates();

		setRouteTemplates();

		setRoutes();

	}

	private void setErrorHandlers() throws Exception{

		for(String prop : props.keySet()){
			if(prop.startsWith("error") && prop.endsWith("uri")){
				String errorUri = props.get(prop);
				String id = StringUtils.substringBetween(prop,"error.",".uri");
				if(!props.containsKey("error." + id + ".route") && !props.containsKey("error." + id + ".routeconfiguration")){
					setErrorHandler(id, errorUri);
				}
			}
		}

	}

	private void setRouteConfigurations() throws Exception{

		for(String prop : props.keySet()){
			if(prop.endsWith("routeconfiguration")){

				String routeConfiguration = props.get(prop);
				String id = props.get(prop + ".id");

				if(routeConfiguration!=null && !routeConfiguration.isEmpty()){
					context.removeRoute(id);
					loadOrUpdateStep(routeConfiguration, "routeconfiguration", id, null);
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
					loadOrUpdateStep(routeTemplate, "routeTemplate definition", id, null);
				}

			}
		}
	}

	//this route create a route template (from a routetemplate definition)
	private void setRouteTemplates() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("routetemplate")){

				String routeTemplate = props.get(prop);
				String basePath = StringUtils.substringBefore(prop,"routetemplate");
				String id = props.get(basePath + "routetemplate.id");
				String uri = props.get(basePath + "uri");

				if(routeTemplate!=null && !routeTemplate.isEmpty()){
					loadOrUpdateStep(routeTemplate, "routeTemplate", id, uri);
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
					loadOrUpdateStep(route, "route",id, null);
				}else{
					loadOrUpdateStep(route, "route", id, null);
				}
			}
		}
	}



	private void loadOrUpdateStep(String route, String type, String id, String uri) throws Exception {

		loader = extendedCamelContext.getRoutesLoader();

		Resource resource = IntegrationUtil.setResource(route);

		if(isStepLoaded(id)) {
			updateStep(resource,route, type, id, uri);
		}else{
			loadStep(resource,route, type, id, uri);
		}

	}

	private void loadStep(Resource resource, String route, String type, String id, String uri){

		try {
			log.info(logMessage("Loading step", id, type, route));

			loader.loadRoutes(List.of(resource));

			flowEvent = "start";
			flowLoaderReport.setStep(id, uri, type, "success", null);
		}catch (Exception e){
			e.printStackTrace();
			try {
				String errorMessage = e.getMessage();

				if(errorMessage.contains("duplicate id detected")) {
					updateStep(resource, route, type, id, uri);
				}else if(errorMessage.contains("Route configuration already exists with id")){
					updateStep(resource, route, type, id, uri);
				}else{
					log.error("Failed loading step | stepid=" + id);
					isFlowLoaded = false;
					flowEvent = "error";
					flowLoaderReport.setStep(id, uri, type,"error",errorMessage);
				}
			}catch (Exception e2){
				log.error("Failed updating step | stepid=" + id);
				isFlowLoaded = false;
				flowEvent = "error";
				flowLoaderReport.setStep(id,uri, type,"error",e2.getMessage());
			}
		}
	}

	private void updateStep(Resource resource, String route, String type, String id, String uri){
		try {
			log.info(logMessage("Updating step", id, type, route));
			loader.updateRoutes(List.of(resource));

			//context
			flowLoaderReport.setStep(id, uri, type, "success", null);
			flowEvent = "start";
		}catch (Exception e) {
			String errorMessage = e.getMessage();
			log.error("Failed updating step | stepid=" + id);
			flowLoaderReport.setStep(id, uri, type, "error", errorMessage);
			flowEvent = "error";
			isFlowLoaded = false;
		}
	}

	private void setErrorHandler(String id, String errorUri) throws Exception {

		if (errorUri!=null && !errorUri.isEmpty()) {
			routeErrorHandler = new DeadLetterChannelBuilder();
			routeErrorHandler = deadLetterChannel(errorUri);
		}else{
			routeErrorHandler = deadLetterChannel("log:org.assimbly.integration.routes.ESBRoute?level=ERROR");
		}

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props);

		routeErrorHandler = errorHandler.configure();

		extendedCamelContext.setErrorHandlerFactory(routeErrorHandler);

		flowLoaderReport.setStep(id, errorUri, "error", "success", null);

	}

	public boolean isStepLoaded(String id){

		Route route = context.getRoute(id);

		if(route != null && route.getUptimeMillis()>0){
			return true;
		}

		return false;
	}

	public boolean isFlowLoaded(){
		return isFlowLoaded;
	}

	private String logMessage(String message, String stepId, String stepType, String route){

		String logMessage = message + "\n\n";
		logMessage = logMessage + "Step id: " + stepId + "\n";
		logMessage = logMessage + "Step type: " + stepType + "\n";
		if(route!=null) {
			logMessage = logMessage + "Step configuration:\n\n" + route + "\n";
		}

		return logMessage;
	}

	public String getReport(){
		return flowLoaderReport.getReport();
	}

}