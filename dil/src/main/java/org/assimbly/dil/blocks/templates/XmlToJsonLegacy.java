package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToJsonLegacy extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltojsonlegacy-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljsonlegacy:marshal?{{options}}")
                     .to("{{out}}");

         routeTemplate("xmltojsonlegacy-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:custom-xmljsonlegacy:marshal?{{options}}");

    }

}
