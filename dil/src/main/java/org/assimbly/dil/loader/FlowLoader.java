package org.assimbly.dil.loader;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteConfigurationDefinition;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.assimbly.util.EncryptionUtil;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class FlowLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private final TreeMap<String, String> props;
	private CamelContext context;
	private RoutesLoader loader;
	private final String flowId;
	private boolean isFlowLoaded = true;
	private final FlowLoaderReport flowLoaderReport;
	private final EncryptionUtil encryptionUtil;

	// Define the fixed metadata key constants
	public static final String METADATA_TENANT_NAME      = "MetaData.TenantName";
	public static final String METADATA_ENVIRONMENT_NAME = "MetaData.EnvironmentName";
	public static final String METADATA_FLOW_NAME        = "MetaData.FlowName";
	public static final String METADATA_FLOW_ID          = "MetaData.FlowID";
	public static final String METADATA_FLOW_VERSION     = "MetaData.FlowVersion";


	public FlowLoader(final TreeMap<String, String> props, FlowLoaderReport flowLoaderReport, EncryptionUtil encryptionUtil){
		super();
		this.props = props;
		this.flowLoaderReport = flowLoaderReport;
		this.flowId = props.get("id");
		this.encryptionUtil = encryptionUtil;
	}

	public interface FailureProcessorListener {
		void onFailure();
	}

	@Override
	public void configure() throws Exception {

		setExtendedcontext();

		setResources();

		setMetadata();

		setErrorHandlers();

		setRouteConfigurations();

		defineRouteTemplates();

		setRouteTemplates();

		setRoutes();

	}

	private void setExtendedcontext() {
		context = getContext();
		loader = PluginHelper.getRoutesLoader(context);
	}

	private void setResources() {

		Registry registry = context.getRegistry();

		for(Map.Entry<String, String> prop : props.entrySet()){
			String key = prop.getKey();
			if(key.startsWith("resource")){
				String id = StringUtils.substringAfter(key,"resource.");
				String resource = prop.getValue();

				registry.unbind(id);
				registry.bind(id, resource);

			}
		}

	}

	private void setErrorHandlers() {

		String errorUri = "";
		String id = "0";
		boolean useErrorHandler = true;

		for(Map.Entry<String, String> prop : props.entrySet()){
			String key = prop.getKey();
			if(key.startsWith("error") && key.endsWith("uri")){
				id = StringUtils.substringBetween(key,"error.",".uri");
				errorUri = props.get(key);
				if(props.containsKey("error." + id + ".route") && props.containsKey("error." + id + ".routeconfiguration")){
					useErrorHandler = false;
				}
			}
		}

		IntegrationUtil.printTreemap(props);

		if(useErrorHandler) {
			log.info("ErrorHandler is set. uri={}",errorUri);
			setErrorHandler(id, errorUri);
		}else{
			log.warn("ErrorHandler is not set (errors are only logged)");
		}

	}

	private void setRouteConfigurations() {

		removeRouteConfiguration(flowId);

		for(Map.Entry<String, String> prop : props.entrySet()){
			String key = prop.getKey();
			if(key.endsWith("routeconfiguration")){

				String id = props.get(key + ".id");
				String routeConfiguration = prop.getValue();

				loadStep(routeConfiguration, "routeconfiguration", id, null);

			}
		}
	}

	//this route defines a route template
	private void defineRouteTemplates() {
		for(Map.Entry<String, String> prop : props.entrySet()){
			String key = prop.getKey();
			if(key.endsWith("routetemplatedefinition")){

				String id = props.get(key + ".id");

				loadStep(key, "routeTemplate definition", id, null);

			}
		}
	}

	//this route create a route template (from a routetemplate definition)
	private void setRouteTemplates() {

		props.forEach((key, value) -> {
			if (key.endsWith("routetemplate")) {
				try {
					String basePath = StringUtils.substringBefore(key,"routetemplate");
					String id = props.get(basePath + "routetemplate.id");
					String uri = props.get(basePath + "uri");

					loadStep(value, "routeTemplate", id, uri);

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

	}

	private void setRoutes() {

		for(String key : props.descendingKeySet()){
			if(key.endsWith("route")){
				String route = props.get(key);
				String id = props.get(key + ".id");
				loadRoute(route, id);
			}
		}

	}


	private void loadRoute(String route, String id) {

		try {

			log.info("Load route:\n\n{}", route);

			loader.loadRoutes(IntegrationUtil.setResource(route));

			flowLoaderReport.setStep(id, null, "route", "success", null, null);

		}catch (Exception e) {

			log.error("""
        Failed loading step:
          [Route ID]: {}
          [Error]  : {}
        """,  id, e.getMessage());
			isFlowLoaded = false;

			flowLoaderReport.setStep(id, null, "route", "error", e.getMessage(), ExceptionUtils.getStackTrace(e));

		}

	}

	private void loadStep(String step, String type, String id, String uri) {

		try {

			log.info("Load step:\n\n{}", step);

			String resolvedStep = decryptStepIfNeeded(step);
			loader.loadRoutes(IntegrationUtil.setResource(resolvedStep));

			flowLoaderReport.setStep(id, uri, type, "success", null, null);

		}catch (Exception e) {

			isFlowLoaded = false;

			log.error("""
        Failed loading step:
          [Step ID]: {}
          [Error]  : {}
        """,  id, e.getMessage());

			flowLoaderReport.setStep(id, uri, type, "error", e.getMessage(), ExceptionUtils.getStackTrace(e));

		}

	}

	private void setErrorHandler(String id, String errorUri) {

		DeadLetterChannelBuilder routeErrorHandler;
		if (errorUri!=null && !errorUri.isEmpty() && !errorUri.startsWith("failedexchange")) {
			routeErrorHandler = new DeadLetterChannelBuilder(errorUri);
		}else{
			routeErrorHandler = deadLetterChannel("log:org.assimbly.integration.routes.ESBRoute?level=ERROR");
		}

		ErrorHandler errorHandler = new ErrorHandler(routeErrorHandler, props, flowId);

		DeadLetterChannelBuilder updatedErrorHandler = errorHandler.configure();

		context.getCamelContextExtension().setErrorHandlerFactory(updatedErrorHandler);

		flowLoaderReport.setStep(id, errorUri, "error", "success", null, null);

	}

	private void removeRouteConfiguration(String flowId) {

		ModelCamelContext modelContext = (ModelCamelContext) context;

		List<RouteConfigurationDefinition> routeConfigurationsToRemove = modelContext.getRouteConfigurationDefinitions().stream()
				.filter(Objects::nonNull) // Exclude null entries
				.filter(routeConfig -> routeConfig.getId().startsWith(flowId))
				.toList(); // Collect into a new list to avoid modifying the original list during iteration

		routeConfigurationsToRemove.forEach(routeConfig -> {
			try {
				modelContext.removeRouteConfiguration(routeConfig);
				log.info("Removed routeConfiguration: {}", routeConfig.getId());
			} catch (Exception _) {
				log.warn("Failed to remove route configuration: {}", routeConfig.getId());
			}
		});

	}

	public String getReport(){
		return flowLoaderReport.getReport();
	}

	public boolean isFlowLoaded(){
		return isFlowLoaded;
	}

	private String decryptStepIfNeeded(String step) {

		if (step == null || !step.contains(EncryptionUtil.ENCODE_PREFIX)) {
			return step;
		}

		Pattern pattern = Pattern.compile(EncryptionUtil.ENCODE_PREFIX_REGEX);
		Matcher matcher = pattern.matcher(step);

		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String encrypted = matcher.group();

			String decrypted = encryptionUtil.decrypt(encrypted);

			matcher.appendReplacement(result,
					Matcher.quoteReplacement(decrypted));
		}

		matcher.appendTail(result);

		return result.toString();
	}

	public void setMetadata() {

		setVariable(METADATA_FLOW_ID, "id");
		setVariable(METADATA_FLOW_NAME, "flow.name");
		setVariable(METADATA_FLOW_VERSION, "flow.version");
		setVariable(METADATA_ENVIRONMENT_NAME, "flow.environment");
		setVariable(METADATA_TENANT_NAME, "flow.tenant");

	}

	private void setVariable(String type, String property){
		if(props.containsKey(property)) {
			context.setVariable("group:" + flowId + ":" + type, props.get(property));
		}
	}

}