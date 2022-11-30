package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class GoogleDrive extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("googledrive-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("options")
                 .templateParameter("out")
                 .from("googledrive://?{{options}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("{{out}}");

         routeTemplate("googledrive-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("googledrive://?{{options}}")
                 .to("{{out}}");

         routeTemplate("googledrive-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .to("googledrive://?{{options}}");

     }

}
