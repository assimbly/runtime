package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Xslt extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("'xslt-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xslt:{{path}}?saxon=true&saxonExtensionFunctions=#uuid-function")
                     .to("{{out}}");

         routeTemplate("xslt-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xslt:{{path}}?saxon=true&saxonExtensionFunctions=#uuid-function");
    }

}
