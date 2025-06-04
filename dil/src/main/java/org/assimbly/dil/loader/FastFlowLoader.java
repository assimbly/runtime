package org.assimbly.dil.loader;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteConfigurationDefinition;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.assimbly.dil.blocks.errorhandler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class FastFlowLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private final TreeMap<String, String> props;
	private CamelContext context;
	private RoutesLoader loader;
	private final String flowId;
	private boolean isFlowLoaded = true;
	private final FlowLoaderReport flowLoaderReport;

	public FastFlowLoader(final TreeMap<String, String> props, FlowLoaderReport flowLoaderReport, String flowId) {
		super();
		this.props = props;
		this.flowId = flowId;
		this.flowLoaderReport = flowLoaderReport;
	}

	public interface FailureProcessorListener {
		void onFailure();
	}

	@Override
	public void configure() throws Exception {

		context = getContext();
		loader = PluginHelper.getRoutesLoader(context);

		setErrorHandlers();

		removeRouteConfiguration(flowId);

		load();

	}

	private void setErrorHandlers() throws Exception{

		String errorUri = "";
		String id = "0";
		boolean useErrorHandler = true;

		for(Map.Entry<String, String> prop : props.entrySet()){
			String key = prop.getKey();
			if(key.startsWith("error") && key.endsWith("uri")){
				id = StringUtils.substringBetween(key,"error.",".uri");
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

	private void load() {

		final String routeTemplate = "routetemplate";
		final String routeConfiguration = "routeconfiguration";

		for (Map.Entry<String, String> entry : props.entrySet()) {
			String key = entry.getKey();
			int lastDot = key.lastIndexOf('.');
			String type = (lastDot >= 0) ? key.substring(lastDot + 1) : key;

			if (type.equals(routeTemplate) || type.equals(routeConfiguration)) {
				String id = props.get(key + ".id");

				//log.info("Installing " + type + " id=" + id);
				//log.info("Step:\n\n" + entry.getValue());

				try {
					Resource step = ResourceHelper.fromString(type + "_" + id + ".xml", entry.getValue());
					loader.loadRoutes(step);
					flowLoaderReport.setStep(id, null, type, "success", null, null);
				} catch (Exception e) {
					isFlowLoaded = false;
					log.error("Failed Loaded " + type + " id=" + id + " Config: \n\n" + entry.getValue());
					flowLoaderReport.setStep(id, null, type, "error", e.getMessage(), "Config:" + entry.getValue() + " Trace:" + ExceptionUtils.getRootCauseMessage(e));
				}
			}
		}
	}

	private void setErrorHandler(String id, String errorUri) throws Exception {

		DeadLetterChannelBuilder routeErrorHandler;
		if (errorUri!=null && !errorUri.isEmpty()) {
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
			} catch (Exception e) {
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

}
