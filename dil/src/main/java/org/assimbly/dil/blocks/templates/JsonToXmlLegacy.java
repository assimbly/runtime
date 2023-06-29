package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class JsonToXmlLegacy extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("jsontoxmllegacy-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljsonlegacy:unmarshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("jsontoxmllegacy-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljsonlegacy:unmarshal?{{options}}");

    }

}
