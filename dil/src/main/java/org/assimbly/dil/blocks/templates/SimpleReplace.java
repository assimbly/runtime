package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SimpleReplace extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("simplereplace-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:simple-replace:unmarshal")
                     .to("{{out}}");

         routeTemplate("simplereplace-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:simple-replace:unmarshal");

    }

}
