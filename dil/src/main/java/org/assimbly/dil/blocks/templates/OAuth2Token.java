package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class OAuth2Token extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("oauth2token-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("oauth2token://?{{options}}")
                    .to("{{out}}");

         routeTemplate("oauth2token-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("oauth2token://?{{options}}");
    }

}
