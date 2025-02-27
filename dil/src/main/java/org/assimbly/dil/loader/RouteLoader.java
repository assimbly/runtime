package org.assimbly.dil.loader;

import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.spi.Resource;
import org.apache.camel.spi.RoutesBuilderLoader;
import org.apache.camel.spi.RoutesLoader;
import org.apache.camel.support.PluginHelper;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RouteLoader extends RouteBuilder {

	protected Logger log = LoggerFactory.getLogger(getClass());
	private CamelContext context;
	private RoutesBuilderLoader routesBuilderLoader;
	private final String routeId;
	private final String route;
	private String flowEvent;
	private boolean isFlowLoaded = true;
	private final FlowLoaderReport flowLoaderReport;

	public RouteLoader(String routeId, String route, FlowLoaderReport flowLoaderReport){
		this.routeId = routeId;
		this.route = route;
		this.flowLoaderReport = flowLoaderReport;
	}

	@Override
	public void configure() throws Exception {

		init();

		load();

		finish();

	}

	private void init() throws Exception {

		setExtendedcontext();

	}

	private void load() throws Exception {

		try {

			Resource resource = IntegrationUtil.setResource(route);
			RoutesBuilder builder = routesBuilderLoader.loadRoutesBuilder(resource);
			context.addRoutes(builder);

			flowLoaderReport.setStep(routeId, null, "route", "success", null);

		}catch (Exception e) {
			String errorMessage = e.getMessage();
			log.error("Failed loading route id=" + routeId);
			flowLoaderReport.setStep(routeId, null, "route", "error", errorMessage);
			isFlowLoaded = false;
		}

	}

	private void finish() {

		if (isFlowLoaded){
			flowLoaderReport.finishReport(flowEvent, "", "Route installed successfully");
		}else{
			flowLoaderReport.finishReport(flowEvent, "","Route installed failed");
		}

	}

	private void setExtendedcontext() throws Exception {
		context = getContext();
		RoutesLoader loader = PluginHelper.getRoutesLoader(context);
		routesBuilderLoader = loader.getRoutesLoader("xml");
	}

	public String getReport(){
		return flowLoaderReport.getReport();
	}

}