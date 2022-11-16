package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class PollEnrich extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("pollenrich-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("timeout","60000")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .pollEnrich().simple("{{path}}?{{options}}").timeout("{{timeout}}")
                     .to("{{out}}");

         routeTemplate("pollenrich-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("timeout","60000")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .pollEnrich().simple("{{path}}?{{options}}").timeout("{{timeout}}");

    }

}
