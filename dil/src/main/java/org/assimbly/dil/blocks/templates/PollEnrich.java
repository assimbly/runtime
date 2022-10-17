package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class PollEnrich extends RouteBuilder {

     //configurable timeout (as string) only from 3.19.0

     @Override
     public void configure() throws Exception {

         routeTemplate("enrichendpoint-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("timeout","60000")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .pollEnrich().simple("{{path}}").timeout(60000)
                     .to("{{out}}");

         routeTemplate("enrichendpoint-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("timeout","60000")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .pollEnrich().simple("{{path}}").timeout(60000);

    }

}
