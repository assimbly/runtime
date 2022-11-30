package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Throttle extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("throttle-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("maximumrequestcount","1")
                 .templateParameter("timeperiod","1000")
                 .templateParameter("asyncDelayed","true")
                 .templateParameter("rejectExecution","false")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .throttle().simple("{{maximumrequestcount}}")
                        .asyncDelayed("{{asyncDelayed}}")
                        .rejectExecution("{{rejectExecution}}")
                        .timePeriodMillis("{{timeperiod}}")
                     .to("{{out}}");

    }

}
