package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class ReplaceInPdf extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("replaceinpdf-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("pdf-transform://{{path}}?{{options}}")
                    .to("{{out}}");

         routeTemplate("replaceinpdf-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("pdf-transform://{{path}}?{{options}}");

     }

}
