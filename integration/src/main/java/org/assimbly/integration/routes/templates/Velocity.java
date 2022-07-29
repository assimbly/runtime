package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

public class Velocity extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("velocity-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setHeader("CamelVelocityTemplate").simple("{{path}}")
                     .to("velocity:generate")
                     .to("{{out}}");

         routeTemplate("velocity-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setHeader("CamelVelocityTemplate").simple("{{path}}")
                     .to("velocity:generate?allowTemplateFromHeader=true");

    }

}
