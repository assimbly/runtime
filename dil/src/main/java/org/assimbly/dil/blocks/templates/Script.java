package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Script extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("groovy-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setBody().groovy("{{path}}")
                     .to("{{out}}");

         routeTemplate("joor-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setBody().joor("{{path}}")
                 .to("{{out}}");

         routeTemplate("java-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setBody().joor("{{path}}")
                 .to("{{out}}");

         routeTemplate("simple-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setBody().simple("{{path}}")
                 .to("{{out}}");

         routeTemplate("python-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setBody().language("python","{{path}}")
                 .to("{{out}}");


         //not working yet
         routeTemplate("jslt-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setHeader("CamelJsltString").constant("{{path}}")
                 .to("jslt:dummy?allowTemplateFromHeader=true")
                 .to("{{out}}");

         //not working yet
         routeTemplate("xslt-script")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("xslt-saxon:{{path}}?saxonExtensionFunctions=#uuid-function")
                 .to("{{out}}");


    }

}
