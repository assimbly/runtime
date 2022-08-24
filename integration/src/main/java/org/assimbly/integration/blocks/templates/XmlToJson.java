package org.assimbly.integration.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToJson extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltojson-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljson:marshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("xmltojson-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljson:marshal?{{options}}");

    }

}
