package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class EdiToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("editoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edi:marshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("editoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edi:marshal?{{options}}");
    }

}
