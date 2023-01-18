package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Fmuta extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("fmuta-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("fmuta:{{path}}?{{options}}")
                     .to("{{out}}");

         routeTemplate("fmuta-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("fmuta:{{path}}?{{options}}");

    }

}
