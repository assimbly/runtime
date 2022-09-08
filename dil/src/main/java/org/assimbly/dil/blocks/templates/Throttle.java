package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Throttle extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("throttle-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("timeperiod","1000")
                 .templateParameter("maximumrequestcount","1")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .throttle(1).timePeriodMillis(1000)
                     .throttle(constant("{{maximumrequestcount}}")).timePeriodMillis("{{timeperiod}}")
                     .to("{{out}}");

    }

}
