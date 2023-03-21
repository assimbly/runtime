package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Delay extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("delay-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("path","5000")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("log:delay_in")
                     .delay().constant("{{path}}").syncDelayed().callerRunsWhenRejected(true)
                     .to("log:delay_out")
                     .to("{{out}}");

    }

}
