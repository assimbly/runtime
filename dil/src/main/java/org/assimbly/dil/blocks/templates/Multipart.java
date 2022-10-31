package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Multipart extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("multipart-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path","")
                 .templateParameter("contentType","multipart/form-data")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .choice()
                        .when().simple("${header.Content-Type}")
                            .setHeader("MultipartFieldName").constant("{{path}}")
                        .endChoice()
                        .otherwise()
                             .setHeader("Content-Type").constant("{{contentType}}")
                             .setHeader("MultipartFieldName").constant("{{path}}")
                     .end()
                     .process("multipartProcessor")
                     .to("{{out}}");

         routeTemplate("multipart-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path","")
                 .templateParameter("contentType","multipart/form-data")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .choice()
                         .when().simple("header.Content-Type")
                             .setHeader("MultipartFieldName").constant("{{path}}")
                         .endChoice()
                         .otherwise()
                              .setHeader("Content-Type").constant("{{contentType}}")
                         .setHeader("MultipartFieldName").constant("{{path}}")
                     .end()
                     .process("multipartProcessor");

    }

}
