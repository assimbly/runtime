package org.assimbly.dil.loader;

import java.util.TreeMap;
import org.apache.camel.*;
import org.apache.camel.builder.*;
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
	private DeadLetterChannelBuilder routeErrorHandler;
	private String flowId;
	private String flowName;
	private String flowEvent;
	private String flowVersion;
	private String flowEnvironment;
	private boolean isFlowLoaded = true;
	private FlowLoaderReport flowLoaderReport;

	public FlowLoader() {
		super();
	}

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

	private void init() {

		flowId = props.get("id");
		flowName = props.get("flow.name");
		flowVersion = props.get("flow.version");
		flowEnvironment = props.get("environment");

		if(flowLoaderReport==null){
		  flowLoaderReport = new FlowLoaderReport();
		  flowLoaderReport.initReport(flowId, flowName, "start");
		}

		setExtendedCamelContext();

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

	private void setExtendedCamelContext() {
		context = getContext();
		loader = PluginHelper.getRoutesLoader(context);
	}
	private void setErrorHandlers() throws Exception{

		String errorUri = "";
		String id = "0";
		Boolean useErrorHandler = true;

		for(String prop : props.keySet()){
			if(prop.startsWith("error") && prop.endsWith("uri")){
				errorUri = props.get(prop);
				id = StringUtils.substringBetween(prop,"error.",".uri");
				if(!props.containsKey("error." + id + ".route") && !props.containsKey("error." + id + ".routeconfiguration")){
					useErrorHandler = false;
				}
			}
		}

		if(useErrorHandler) {
			setErrorHandler(id, errorUri);
		}else{
			System.out.println("Set error handler is true");
		}

	}

	private void setRouteConfigurations() throws Exception{

		for(String prop : props.keySet()){
			if(prop.endsWith("routeconfiguration")){

				String routeConfiguration = props.get(prop);
				String id = props.get(prop + ".id");

				if(routeConfiguration!=null && !routeConfiguration.isEmpty()){
					context.removeRoute(id);
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
		for(String prop : props.keySet()){
			if(prop.endsWith("routetemplate")){

				String routeTemplate = props.get(prop);
				String basePath = StringUtils.substringBefore(prop,"routetemplate");
				String id = props.get(basePath + "routetemplate.id");
				String uri = props.get(basePath + "uri");

				if(routeTemplate!=null && !routeTemplate.isEmpty()){
					loadStep(routeTemplate, "routeTemplate", id, uri);
				}

			}
		}
	}

	private void setRoutes() throws Exception{
		for(String prop : props.keySet()){
			if(prop.endsWith("route")){

				String route = props.get(prop);
				String id = props.get(prop + ".id");

				loadStep(route, "route",id, null);
			}
		}
	}

	private void loadStep(String route, String type, String id, String uri) throws Exception {

		try {
			log.info(logMessage("Loading step", id, type, route));

			if(context.getRoute(id)!=null){
				context.removeRoute(id);
			}

			loader.loadRoutes(IntegrationUtil.setResource(route));

			//context
			flowLoaderReport.setStep(id, uri, type, "success", null);
			flowEvent = "start";
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