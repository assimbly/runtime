package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Amazon extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("amazon-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("accessKey")
                 .templateOptionalParameter("secretKey")
                 .templateOptionalParameter("authToken")
                 .templateOptionalParameter("sellerId")
                 .templateOptionalParameter("marketplace")
                 .templateOptionalParameter("parameters")
                 .templateOptionalParameter("settings")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("amazon://SUBMIT_FEED?{{options}}")
                 .to("{{out}}");

         routeTemplate("amazon-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("accessKey")
                 .templateOptionalParameter("secretKey")
                 .templateOptionalParameter("authToken")
                 .templateOptionalParameter("sellerId")
                 .templateOptionalParameter("marketplace")
                 .templateOptionalParameter("parameters")
                 .templateOptionalParameter("settings")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("amazon://SUBMIT_FEED?{{options}}");

    }

}
