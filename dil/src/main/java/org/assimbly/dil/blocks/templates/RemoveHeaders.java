package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class RemoveHeaders extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("removeheaders-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("excludePatterns","")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .removeHeaders("{{path}}","{{excludePattern}}")
                     .to("{{out}}");

         routeTemplate("removeheaders-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("excludePatterns","")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .removeHeaders("{{path}}","{{excludePattern}}");
     }
}
