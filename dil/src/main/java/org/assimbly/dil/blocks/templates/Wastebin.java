package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Wastebin extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("wastebin-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("mock:wastebin");

    }

}
