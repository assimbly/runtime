package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class FormToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("formtoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("options","encoding=utf-8")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("formtoxml://?{{options}}")
                     .to("{{out}}");

         routeTemplate("formtoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("options","encoding=utf-8")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("formtoxml://?{{options}}");

    }

}
