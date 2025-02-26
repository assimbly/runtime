package org.assimbly.dil.blocks.processors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Route;
import org.apache.camel.spi.RouteController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ManageFlowProcessor implements Processor {

	protected Logger log = LoggerFactory.getLogger(getClass());

	public void process(Exchange exchange) throws Exception {

		String flowId = exchange.getProperty("flowId",String.class);
		String action = exchange.getProperty("action",String.class);

		if(flowId != null && action != null){

			log.info("ManageFlow start: Action=" + action +  " Flow=" + flowId);

			CamelContext context = exchange.getContext();
			RouteController routeController = context.getRouteController();
			List<Route> routes = getRoutesByFlowId(flowId,context);

			for(Route route: routes){
			  manageFlow(routeController, route, action);
			}

			exchange.removeProperty("flowId");
			exchange.removeProperty("action");

			log.info("ManageFlow finished: Action=" + action +  " Flow=" + flowId);

		}else{
			if(flowId==null){
				log.warn("ManageFlow: Can't perform action. FlowId is not provided.");
			}

			if(action==null){
				log.warn("ManageFlow: Can't perform action. Action is not provided");
			}

		}

	}

	private void manageFlow(RouteController routeController, Route route, String action) throws Exception {

		String routeId = route.getId();

		if(action.equalsIgnoreCase("startflow")){
			log.info("start route: " + route);
			routeController.startRoute(routeId);
		}else if(action.equalsIgnoreCase("stopflow")){
			log.info("stop route: " + route);
			routeController.stopRoute(routeId,10, TimeUnit.SECONDS);
		}else if(action.equalsIgnoreCase("suspendflow") || action.equalsIgnoreCase("pauseflow")){
			log.info("suspend route: " + route);
			routeController.suspendRoute(routeId,10, TimeUnit.SECONDS);
		}else if(action.equalsIgnoreCase("resumeflow") || action.equalsIgnoreCase("continueflow")){
			log.info("resume route: " + route);
			routeController.resumeRoute(routeId);
		}

	}

	private List<Route> getRoutesByFlowId(String id, CamelContext context){
		return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).collect(Collectors.toList());
	}

}