package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Aleris extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("aleris-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("id")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("aleris:{{id}}")
                     .to("{{out}}");

         routeTemplate("aleris-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("id")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("aleris:{{id}}");

    }

}
