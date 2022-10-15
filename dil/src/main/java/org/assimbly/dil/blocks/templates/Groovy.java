package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Groovy extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("groovy-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("sandbox://groovy?script=RAW({{path}})")
                     .to("{{out}}");

         routeTemplate("groovy-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("sandbox://groovy?script=RAW({{path}})");

    }

}
