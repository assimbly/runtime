package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;
import org.assimbly.dil.blocks.beans.enrich.EnrichStrategy;

public class Enrich extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("enrich-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("out_rule")
                 .templateParameter("enrichType","text/xml")
                 .templateParameter("errorRoute","false")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("Enrich-Type").constant("{{enrichType}}")
                     .setProperty("Error-Route").constant("{{errorRoute}}")
                     .enrich("{{out_rule}}", new EnrichStrategy())
                     .to("{{out}}");

    }

}
