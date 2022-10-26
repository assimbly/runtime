package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Amazon extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("amazon-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("accessKey")
                 .templateParameter("secretKey")
                 .templateParameter("authToken")
                 .templateParameter("sellerId")
                 .templateParameter("marketplace")
                 .templateParameter("parameters")
                 .templateParameter("settings")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("amazon://SUBMIT_FEED?accessKey=RAW({{accessKey}})&amp;secretKey=RAW({{secretKey}})&amp;authToken=RAW({{authToken}})&amp;sellerId=RAW({{sellerId}})&amp;marketplace=RAW({{marketplace}})&amp;parameters=RAW({{parameters}})&amp;settings=RAW({{settings}})")
                    .to("{{out}}");

         routeTemplate("amazon-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("accessKey")
                 .templateParameter("secretKey")
                 .templateParameter("authToken")
                 .templateParameter("sellerId")
                 .templateParameter("marketplace")
                 .templateParameter("parameters")
                 .templateParameter("settings")
                 .templateParameter("in")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("amazon://SUBMIT_FEED?accessKey=RAW({{accessKey}})&amp;secretKey=RAW({{secretKey}})&amp;authToken=RAW({{authToken}})&amp;sellerId=RAW({{sellerId}})&amp;marketplace=RAW({{marketplace}})&amp;parameters=RAW({{parameters}})&amp;settings=RAW({{settings}})");
    }

}
