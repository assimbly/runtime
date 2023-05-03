package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Fmuta extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         //to do
         routeTemplate("fmuta-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("path")
                 .templateParameter("options")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("fmuta:{{path}}?{{options}}")
                     .to("{{out}}");

         //to do
         routeTemplate("fmuta-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("path")
                 .templateParameter("options")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("fmuta:{{path}}?{{options}}");

    }

}
