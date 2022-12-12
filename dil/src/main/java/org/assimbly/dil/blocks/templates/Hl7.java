package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Hl7 extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("hl7xml-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("mllp:{{path}}")
                 .unmarshal().hl7(true)
                 .to("bean:Hl7ToXmlConverter")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("hl7xml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("bean:XmlToHl7Converter")
                 .to("mllp:{{path}}")
                 .to("{{out}}");

         routeTemplate("hl7xml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("bean:XmlToHl7Converter")
                 .to("mllp:{{path}}");

         routeTemplate("hl7er7-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("out")
                 .from("mllp:{{path}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("hl7er7-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("mllp:{{path}}")
                 .to("{{out}}");

         routeTemplate("hl7er7-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("mllp:{{path}}");

     }

}
