package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Javascript extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("javascript-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                        .setBody().js("{{path}}")
                        .to("{{out}}");

         routeTemplate("javascript-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody().js("{{path}}");

    }

}
