package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Archive extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("archive-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("usingIterator", "true")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("dataformat:checkedZipFileDataFormat:marshal?usingIterator={{usingIterator}}")
                     .to("{{out}}");

         routeTemplate("archive-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("usingIterator", "true")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                         .split().simple("${body}").streaming(true)
                 .to("dataformat:checkedZipFileDataFormat:marshal?usingIterator={{usingIterator}}");

    }

}
