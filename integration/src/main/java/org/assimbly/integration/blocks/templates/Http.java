package org.assimbly.integration.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Http extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("http-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty:http:{{path}}?httpBinding=#customHttpBinding&amp;matchOnUriPrefix=false&amp;sslContextParameters=sslContext")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .removeHeaders("CamelHttp*")
                     .to("{{out}}");

         routeTemplate("https-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty:https:{{path}}?httpBinding=#customHttpBinding&amp;matchOnUriPrefix=false&amp;sslContextParameters=sslContext")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .removeHeaders("CamelHttp*")
                     .to("{{out}}");

     }

}
