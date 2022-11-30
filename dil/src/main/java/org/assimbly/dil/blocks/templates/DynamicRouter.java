package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class DynamicRouter extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("dynamic-router")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("out_rules_list")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("routingRules").constant("{{out_rules_list}}")
                     .setProperty("defaultEndpoint").constant("{{out}}")
                     .process("RoutingRulesProcessor")
                     .toD("${exchangeProperty.endpoint}");

     }
}
