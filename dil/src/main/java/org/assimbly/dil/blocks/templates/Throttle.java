package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Throttle extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("throttle-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("maximumRequestCount","1")
                 .templateParameter("timePeriod","1000")
                 .templateParameter("asyncDelayed","true")
                 .templateParameter("rejectExecution","false")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .throttle().simple("{{maximumRequestCount}}")
                        .asyncDelayed("{{asyncDelayed}}")
                        .rejectExecution("{{rejectExecution}}")
                        .timePeriodMillis("{{timePeriod}}")
                     .to("{{out}}");

    }

}
