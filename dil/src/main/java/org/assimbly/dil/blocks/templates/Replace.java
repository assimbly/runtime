package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Replace extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("replace-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("replace://?{{options}}")
                     .to("{{out}}");

         routeTemplate("replace-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("replace://?{{options}}");

    }

}
