package org.assimbly.integration.routes.templates;

import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class FromRouteTemplates extends RouteBuilder {

    @Override
    public void configure() throws Exception {
    	
    	
    	
        // create a route template with the given name
        /*
    	routeTemplate("fromRoute")
            //required input parameters
            .templateParameter("uri")
            .templateParameter("flowName")
            .templateParameter("flowId")
            .templateParameter("headerId")
            .templateParameter("endpointId")
            .templateParameter("routeId")
            .templateParameter("logLevelAsString","OFF")
            .templateParameter("hasAssimblyHeaders")
            .templateParameter("hasRoute")
            .templateParameter("uriList")
            .templateBean("headerProcessor",Processor.class)
            .templateBean("convertProcessor",Processor.class)
            // the route
            .from("{{uri}}")
				//.errorHandler(routeErrorHandler)
				.setHeader("AssimblyHeaderId", constant("{{headerId}}"))
				.choice()
					.when("{{hasAssimblyHeaders}}")
						.setHeader("AssimblyFlowID", constant("{{flowId}}"))
						.setHeader("AssimblyFrom", constant("{{endpointId}}"))
						.setHeader("AssimblyCorrelationId", simple("${date:now:yyyyMMdd}${exchangeId}"))
						.setHeader("AssimblyFromTimestamp", groovy("new Date().getTime()"))
				.end()
				.to("log:Flow={{flowName}}|ID={{flowId}}|RECEIVED?level={{logLevelAsString}}&showAll=true&multiline=true&style=Fixed")
				.process("{{headerProcessor}}")
				.id("headerProcessor{{flowId}}-{{endpointId}}")
				.process("{{convertProcessor}}")
				.id("convertProcessor{{flowId}}-{{endpointId}}")
				.choice()
					.when("{{hasRoute}}")
						.to("direct:flow={{flowId}}route={{flowId}}-{{endpointId}}-{{routeId}}")
				.end()
				.to("{{uriList}}");		
			*/
    }
}