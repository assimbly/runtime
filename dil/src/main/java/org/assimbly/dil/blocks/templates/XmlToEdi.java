package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToEdi extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltoedi-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edi:unmarshal")
                     .to("{{out}}");

         routeTemplate("xmltoedi-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:edi:unmarshal");
    }

}
