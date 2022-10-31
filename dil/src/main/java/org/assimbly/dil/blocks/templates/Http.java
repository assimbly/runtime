package org.assimbly.dil.blocks.templates;

import org.apache.camel.ExchangePattern;
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

         routeTemplate("http-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("http:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("https-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .setHeader("CamelHttpMethod").constant("{{httpMethod}}")
                     .setHeader("user-agent").constant("{{userAgent}}")
                     .to("https:{{path}}?{{options}}")
                     .to("{{out}}");

         routeTemplate("http-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .to("http:{{path}}?{{options}}");

         routeTemplate("https-sink")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .to("https:{{path}}?{{options}}");





     }

}
