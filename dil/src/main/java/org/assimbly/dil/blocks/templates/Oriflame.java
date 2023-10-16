package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Oriflame extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("oriflame-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("oriflame:{{path}}//?{{options}}")
                    .to("{{out}}");

         routeTemplate("oriflame-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("oriflame:{{path}}//?{{options}}");
    }

}
