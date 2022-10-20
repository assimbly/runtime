package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Base64 extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("texttobase64-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .marshal("base64")
                     .to("{{out}}");

         routeTemplate("texttobase64-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .marshal("base64");

         routeTemplate("base64totext-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .unmarshal("base64")
                 .to("{{out}}");

         routeTemplate("base64totext-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .unmarshal("base64");

     }

}
