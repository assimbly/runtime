package org.assimbly.integration.routes.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToJson extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltojson-action")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .to("dataformat:custom-xmljson:marshal?{{options}}")
                 .to("{{out}}");

         routeTemplate("xmltojson-sink")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .to("dataformat:custom-xmljson:marshal?{{options}}");

    }

}
