package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class EdifactToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("edifacttoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edifact:marshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("edifacttoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edifact:marshal?{{options}}");
    }

}
