package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Wiretap extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("wiretap-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .wireTap("{{path}}").executorService("wiretapProfile")
                     .to("{{out}}");

         routeTemplate("wiretap-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .wireTap("{{path}}").executorService("wiretapProfile");
    }

}
