package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class JsonToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("jsontoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljson:unmarshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("jsontoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljson:unmarshal?{{options}}");

    }

}
