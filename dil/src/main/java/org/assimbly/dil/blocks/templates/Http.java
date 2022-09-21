package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Http extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("http-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty-nossl:http:{{path}}?jettyHttpBinding=#customHttpBinding&matchOnUriPrefix=false")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .removeHeaders("CamelHttp*")
                     .to("{{out}}");

         routeTemplate("https-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty:https:{{path}}?jettyHttpBinding=#customHttpBinding&matchOnUriPrefix=false")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .removeHeaders("CamelHttp*")
                     .to("{{out}}");

     }

}
