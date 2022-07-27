package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

public class Generic extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("generic-source")
                 .templateParameter("uri")
                 .templateParameter("out")
                 .from("{{uri}}")
                     .to("{{out}}");

         routeTemplate("generic-action")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .to("{{uri}}")
                    .to("{{out}}");

         routeTemplate("generic-sink")
                 .templateParameter("uri")
                 .templateParameter("in")
                 .from("{{in}}")
                     .to("{{uri}}");

     }

}
