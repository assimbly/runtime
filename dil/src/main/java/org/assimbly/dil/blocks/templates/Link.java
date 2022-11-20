package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Link extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("link-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("out")
                 .templateParameter("exchangePattern","InOnly")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                        .to("{{out}}");

         routeTemplate("link-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("{{out}}");

         routeTemplate("link-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .templateParameter("exchangePattern","InOnly")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}");

         routeTemplate("link-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out_list")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .recipientList().constant("{{out_list}}");

     }

}
