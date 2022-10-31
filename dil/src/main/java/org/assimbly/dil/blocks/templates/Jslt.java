package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Jslt extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("jslt-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setHeader("CamelJsltString").constant("{{path}}")
                     .to("jslt:dummy?allowTemplateFromHeader=true")
                     .to("{{out}}");

         routeTemplate("jslt-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setHeader("CamelJsltString").constant("{{path}}")
                     .to("jslt:dummy?allowTemplateFromHeader=true");
    }

}
