package org.assimbly.dil.blocks.templates;

import org.apache.camel.BindToRegistry;
import org.apache.camel.builder.RouteBuilder;

public class Generic extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("generic-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("out")
                 .from("{{uri}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .toD("{{out}}");

         routeTemplate("generic-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .toD("{{uri}}")
                    .to("{{out}}");

         routeTemplate("generic-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .toD("{{uri}}");

     }

}
