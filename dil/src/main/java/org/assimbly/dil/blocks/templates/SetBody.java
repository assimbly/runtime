package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetBody extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setbody-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("language","constant")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("assimbly.body").constant("{{path}}")
                 .setProperty("assimbly.language").constant("{{language}}")
                 .process("SetBodyProcessor")
                 .to("{{out}}");

         routeTemplate("setbody-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("language","constant")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("assimbly.body").constant("{{path}}")
                 .setProperty("assimbly.language").constant("{{language}}")
                 .process("SetBodyProcessor");

         routeTemplate("setbodybyheader-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(header("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyheader-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(header("{{path}}"));

         routeTemplate("setbodyasstring-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .convertBodyTo(String.class)
                 .to("{{out}}");

         routeTemplate("setbodyasbytes-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .convertBodyTo(byte[].class)
                 .to("{{out}}");


         routeTemplate("prependtobody-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .transform(body().prepend("{{path}}"))
                    .to("{{out}}");

         routeTemplate("prependtobody-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .transform(body().prepend("{{path}}"));

         routeTemplate("appendtobody-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .transform(body().append("{{path}}"))
                     .to("{{out}}");

         routeTemplate("appendtobody-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .transform(body().append("{{path}}"));



     }


}
