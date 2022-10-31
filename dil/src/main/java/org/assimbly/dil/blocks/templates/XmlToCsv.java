package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToCsv extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltocsv-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("includeHeader","true")
                 .templateParameter("includeIndexColumn","false")
                 .templateParameter("indexColumnName","line")
                 .templateParameter("orderHeaders","UNORDERED")
                 .templateParameter("quoteFields","ALL_FIELDS")
                 .templateParameter("delimiter","LA==")
                 .templateParameter("lineSeparator","XG4=")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xmltocsv://?includeHeader={{includeHeader}}&includeIndexColumn={{includeIndexColumn}}&indexColumnName={{indexColumnName}}&orderHeaders={{orderHeaders}}&quoteFields={{quoteFields}}&delimiter=RAW({{delimiter}})&lineSeparator=RAW({{lineSeparator}})")
                     .to("{{out}}");




         routeTemplate("xmltocsv-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xmltocsv://?includeHeader={{includeHeader}}&includeIndexColumn={{includeIndexColumn}}&indexColumnName={{indexColumnName}}&orderHeaders={{orderHeaders}}&quoteFields={{quoteFields}}&delimiter=RAW({{delimiter}})&lineSeparator=RAW({{lineSeparator}})");
    }

}
