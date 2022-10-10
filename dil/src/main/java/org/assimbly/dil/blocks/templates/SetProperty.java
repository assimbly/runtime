package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class SetProperty extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setproperty-action")
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
                        .setProperty("{{path}}").simple("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'xpath'"))
                        .setProperty("{{path}}").xpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'jsonpath'"))
                        .setProperty("{{path}}").jsonpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'groovy'"))
                        .setProperty("{{path}}").groovy("{{value}}")
                     .endChoice()
                     .otherwise()
                        .setProperty("{{path}}",constant("{{value}}"))
                 .end()
                 .to("{{out}}");

         routeTemplate("setproperty-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("path")
                 .templateParameter("value")
                 .templateParameter("language")
                 .templateParameter("in")
                 .from("{{in}}")
                 .choice()
                     .when(simple("'{{language}}' == 'simple'"))
                         .setProperty("{{path}}").simple("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'xpath'"))
                        .setProperty("{{path}}").xpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'jsonpath'"))
                        .setProperty("{{path}}").jsonpath("{{value}}")
                     .endChoice()
                     .when(simple("'{{language}}' == 'groovy'"))
                        .setProperty("{{path}}").groovy("{{value}}")
                     .endChoice()
                     .otherwise()
                        .setProperty("{{path}}",constant("{{value}}"))
                 .end();
     }
}
