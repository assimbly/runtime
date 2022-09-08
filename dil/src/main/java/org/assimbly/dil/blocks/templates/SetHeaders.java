package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class SetHeaders extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setheaders-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("headers")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("assimbly.headers").constant("{{headers}}")
                     .process("SetHeadersProcessor")
                     .to("{{out}}");

         routeTemplate("setheaders-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("headers")
                 .templateParameter("in")
                     .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("assimbly.headers").constant("{{headers}}")
                     .process("SetHeadersProcessor");

     }
}
