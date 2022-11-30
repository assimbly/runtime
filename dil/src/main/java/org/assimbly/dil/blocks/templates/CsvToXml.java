package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.dataformat.csv.CsvDataFormat;

public class CsvToXml extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         CsvDataFormat csv = new CsvDataFormat();
         csv.setDelimiter(Character.valueOf(','));
         csv.setUseMaps(true);

         routeTemplate("csvtoxml-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("encoding","UTF-8")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .unmarshal(csv)
                     .to("csvtoxml://?encoding={{encoding}}")
                     .to("{{out}}");

         routeTemplate("csvtoxml-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("encoding","UTF-8")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .unmarshal(csv)
                     .to("csvtoxml://?encoding={{encoding}}");

    }

}
