package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Velocity extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("velocity-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setHeader("CamelVelocityTemplate").simple("{{path}}")
                     .to("velocity:generate?allowTemplateFromHeader=true")
                     .to("{{out}}");

         routeTemplate("velocity-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setHeader("CamelVelocityTemplate").simple("{{path}}")
                     .to("velocity:generate?allowTemplateFromHeader=true");

    }

}
