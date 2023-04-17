package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;
import org.assimbly.dil.blocks.beans.AggregateStrategy;

public class Aggregate extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("aggregate-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("aggregateType","text/xml")
                 .templateParameter("completionSize","3")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                        .setProperty("Aggregate-Type").constant("{{aggregateType}}")
                        .aggregate(constant(true), new AggregateStrategy())
                            .completionSize("{{completionSize}}")
                         .to("{{out}}");

    }

}
