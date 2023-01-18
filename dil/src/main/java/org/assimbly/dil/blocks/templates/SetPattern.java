package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetPattern extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setpattern-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("pattern","InOnly")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                     .setExchangePattern("{{pattern}}")
                     .to("{{out}}");

     }
}
