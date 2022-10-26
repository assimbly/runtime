package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Archive extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("zip-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("usingIterator", "true")
                 .templateParameter("preservePathElements", "true")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("dataformat:zipFileDataFormat:marshal?usingIterator={{usingIterator}}&amp;preservePathElements={{preservePathElements}}")
                 .to("{{out}}");

         routeTemplate("zip-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("usingIterator", "true")
                 .templateParameter("preservePathElements", "true")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .split().simple("${body}").streaming(true)
                 .to("dataformat:zipFileDataFormat:marshal?usingIterator={{usingIterator}}&amp;preservePathElements={{preservePathElements}}");

         routeTemplate("unzip-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("usingIterator", "true")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("dataformat:checkedZipFileDataFormat:unmarshal?usingIterator={{usingIterator}}")
                 .to("{{out}}");

         routeTemplate("unzip-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("usingIterator", "true")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .split().simple("${body}").streaming(true)
                 .to("dataformat:checkedZipFileDataFormat:unmarshal?usingIterator={{usingIterator}}");

    }

}
