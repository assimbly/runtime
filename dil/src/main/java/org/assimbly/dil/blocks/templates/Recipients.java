package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Recipients extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("recipients-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out_list")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("{{out_list}}");

    }

}
