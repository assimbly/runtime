package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Amazon extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("amazon-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("amazon:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("amazon-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("amazon:{{path}}?{{options}}");
    }

}
