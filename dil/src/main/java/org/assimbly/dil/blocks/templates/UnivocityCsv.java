package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.UniVocityHeader;

public class UnivocityCsv extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("univocity-csv-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("dataformat:univocityCsv:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("univocity-csv-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("dataformat:univocityCsv:{{path}}?{{options}}");
     }

}
