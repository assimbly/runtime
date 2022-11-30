package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class ExcelToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("exceltoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("exceltoxml://?{{options}}")
                     .to("{{out}}");

         routeTemplate("exceltoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("exceltoxml://?{{options}}");

    }

}
