package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Python extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("python-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                        .setBody().python("{{path}}")
                        .to("{{out}}");

         routeTemplate("python-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody().python("{{path}}");

    }

}
