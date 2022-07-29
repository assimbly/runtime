package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

import static org.apache.camel.language.groovy.GroovyLanguage.groovy;

public class SetBody extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setbodybyconstant-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(constant("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyconstant-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setBody(constant("{{path}}"));

         routeTemplate("setbodybysimple-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(simple("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybysimple-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .setBody(simple("{{path}}"));

         routeTemplate("setbodybygroovy-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(groovy("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybygroovy-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setBody(groovy("{{path}}"));

         routeTemplate("setbodybyxpath-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(xpath("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyxpath-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setBody(xpath("{{path}}"));

         routeTemplate("setbodybyjsonpath-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(jsonpath("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyjsonpath-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setBody(jsonpath("{{path}}"));

         routeTemplate("setbodybyheader-action")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setBody(header("{{path}}"))
                     .to("{{out}}");

         routeTemplate("setbodybyheader-sink")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .setBody(header("{{path}}"));

     }

}
