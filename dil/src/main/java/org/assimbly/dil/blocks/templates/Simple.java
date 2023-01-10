package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Simple extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("simple-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                        .setBody().simple("{{path}}")
                        .to("{{out}}");

         routeTemplate("simple-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody().simple("{{path}}");

    }

}
