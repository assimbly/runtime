package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Print extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("print-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .log("{{path}}");

         routeTemplate("print-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .log("{{path}}");

     }

}
