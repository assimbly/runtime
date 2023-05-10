package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetOAuth2Token extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setoauth2token-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("setoauth2token://?{{options}}")
                    .to("{{out}}");

         routeTemplate("setoauth2token-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                    .routeConfigurationId("{{routeconfiguration_id}}")
                    .to("oauth2token://?{{options}}");
    }

}
