package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetHeader extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setheader-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("value","")
                 .templateParameter("language","constant")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .choice()
                        .when(simple("'{{language}}' == 'simple'"))
                             .setHeader("{{path}}").simple("{{value}}")
                        .endChoice()
                        .when(simple("'{{language}}' == 'xpath'"))
                            .setHeader("{{path}}").xpath("{{value}}")
                         .endChoice()
                         .when(simple("'{{language}}' == 'jsonpath'"))
                             .setHeader("{{path}}").jsonpath("{{value}}")
                         .endChoice()
                         .when(simple("'{{language}}' == 'groovy'"))
                           .setHeader("{{path}}").groovy("{{value}}")
                          .endChoice()
                         .otherwise()
                            .setHeader("{{path}}",constant("{{value}}"))
                     .end()
                     .to("{{out}}");

         routeTemplate("setheader-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("value")
                 .templateParameter("language")
                 .templateParameter("in")
                     .from("{{in}}")
                 .choice()
                     .when(simple("'{{language}}' == 'simple'"))
                        .setHeader("{{path}}").simple("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'xpath'"))
                        .setHeader("{{path}}").xpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'jsonpath'"))
                        .setHeader("{{path}}").jsonpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'groovy'"))
                        .setHeader("{{path}}").groovy("{{value}}")
                     .endChoice()
                     .otherwise()
                        .setHeader("{{path}}",constant("{{value}}"))
                 .end();
     }
}
