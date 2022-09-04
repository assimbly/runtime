package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class SetBody extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setbodybyconstant-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(constant("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyconstant-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(constant("{{path}}"));

         routeTemplate("setbodybysimple-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(simple("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybysimple-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(simple("{{path}}"));

         routeTemplate("setbodybygroovy-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(groovy("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybygroovy-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(groovy("{{path}}"));

         routeTemplate("setbodybyxpath-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(xpath("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyxpath-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(xpath("{{path}}"));

         routeTemplate("setbodybyjsonpath-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(jsonpath("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyjsonpath-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody(jsonpath("{{path}}"));

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
