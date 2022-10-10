package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToEdifact extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltoedifact-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edifact:unmarshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("xmltoedifact-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edifact:unmarshal?{{options}}");
    }

}
