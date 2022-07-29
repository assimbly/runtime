package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

public class Http extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("http-source")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty:http:{{path}}?httpBinding=#customHttpBinding&amp;matchOnUriPrefix=false&amp;sslContextParameters=sslContext")
                     .removeHeaders("CamelHttp*")
                     .to("{{out}}");

         routeTemplate("https-source")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("jetty:https:{{path}}?httpBinding=#customHttpBinding&amp;matchOnUriPrefix=false&amp;sslContextParameters=sslContext")
                 .removeHeaders("CamelHttp*")
                 .to("{{out}}");

     }

}
