package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Filter extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("filter-xpath-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("expression")
                 .templateParameter("in")
                 .templateParameter("out_rule")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                         .filter().xpath("{{expression}}")
                         .to("{{out_rule}}");

    }

}
