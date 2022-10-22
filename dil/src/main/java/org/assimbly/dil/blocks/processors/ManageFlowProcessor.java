package org.assimbly.dil.blocks.processors;

import org.apache.camel.*;
import org.apache.camel.spi.RouteController;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

//set headers for each step
public class ManageFlowProcessor implements Processor {

	public void process(Exchange exchange) throws Exception {

		CamelContext context = exchange.getContext();

		String flowId = exchange.getProperty("flowId",String.class);
		String action = exchange.getProperty("action",String.class);

		  if(flowId != null && action != null){

			  RouteController routeController = context.getRouteController();
			  List<Route> routes = getRoutesByFlowId(flowId,context);

			  for(Route route: routes){

				  String routeId = route.getId();

				  if(action.equalsIgnoreCase("startflow")){
					  routeController.startRoute(routeId);
				  }else if(action.equalsIgnoreCase("stopflow")){
					  routeController.stopRoute(routeId,30, TimeUnit.SECONDS);
				  }else if(action.equalsIgnoreCase("suspendflow") || action.equalsIgnoreCase("pauseflow")){
					  routeController.suspendRoute(routeId,30, TimeUnit.SECONDS);
				  }else if(action.equalsIgnoreCase("resumeflow") || action.equalsIgnoreCase("resumeflow")){
					  routeController.resumeRoute(routeId);
				  }

			  }

		  }

		  exchange.removeProperty("pattern");

	}

	private List<Route> getRoutesByFlowId(String id, CamelContext context){
		return context.getRoutes().stream().filter(r -> r.getId().startsWith(id)).collect(Collectors.toList());
	}

}