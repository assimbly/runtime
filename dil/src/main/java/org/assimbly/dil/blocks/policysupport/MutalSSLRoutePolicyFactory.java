package org.assimbly.dil.blocks.policysupport;

import org.apache.camel.CamelContext;
import org.apache.camel.NamedNode;
import org.apache.camel.model.*;
import org.apache.camel.spi.RoutePolicy;
import org.apache.camel.spi.RoutePolicyFactory;
import org.apache.camel.support.DefaultExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MutalSSLRoutePolicyFactory implements RoutePolicyFactory {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public RoutePolicy createRoutePolicy(CamelContext camelContext, String routeId, NamedNode route) {
        String resource = null;
        String authPassword = null;
        boolean isMutualSSL = false;

        if (route instanceof RouteDefinition) {
            RouteDefinition routeDefinition = (RouteDefinition) route;
            List<ProcessorDefinition<?>> outputs = routeDefinition.getOutputs();
            for (ProcessorDefinition<?> output : outputs) {
                if (output instanceof StepDefinition) {
                    StepDefinition step = (StepDefinition) output;
                    List<ProcessorDefinition<?>> stepOutputs = step.getOutputs();
                    for (ProcessorDefinition<?> stepOutput : stepOutputs) {
                        if (stepOutput instanceof SetPropertyDefinition) {
                            SetPropertyDefinition propDefinition = (SetPropertyDefinition) stepOutput;
                            String propValue = getPropertyValue(camelContext, propDefinition);
                            switch(propDefinition.getName()) {
                                case "httpMutualSSL":
                                    isMutualSSL = (propValue!=null && propValue.equals("true") ? true : false);
                                    break;
                                case "resource":
                                    resource = propValue;
                                    break;
                                case "authPassword":
                                    authPassword = propValue;
                                    break;
                            }
                        }
                    }
                }
            }
        }

        if(isMutualSSL && resource!=null && authPassword!=null) {
            return new MutualSSLRoutePolicy(resource, authPassword);
        }

        return null;
    }

    private static String getPropertyValue(CamelContext camelContext, SetPropertyDefinition propDefinition) {
        return propDefinition.getExpression().evaluate(new DefaultExchange(camelContext), String.class);
    }


}
