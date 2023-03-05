package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetUUID extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setuuid-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path","uuid")
                 .templateParameter("generator","default")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setHeader("{{path}}").simple("${uuid({{generator}}}")
                     .to("{{out}}");

         routeTemplate("setuuid-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path","uuid")
                 .templateParameter("UuidGenerator","default")
                 .templateParameter("in")
                     .from("{{in}}")
                         .routeConfigurationId("{{routeconfiguration_id}}")
                         .setHeader("{{path}}").simple("${uuid({{generator}}}");
     }
}
