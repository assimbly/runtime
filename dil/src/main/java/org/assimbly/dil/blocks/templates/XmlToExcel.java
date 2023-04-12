package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class XmlToExcel extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("xmltoexcel-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("includeHeader","true")
                 .templateParameter("includeIndexColumn","false")
                 .templateParameter("indexColumnName","line")
                 .templateParameter("orderHeaders","UNORDERED")
                 .templateParameter("excelFormat","XLSX")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xmltoexcel://?includeHeader={{includeHeader}}&includeIndexColumn={{includeIndexColumn}}&indexColumnName={{indexColumnName}}&orderHeaders={{orderHeaders}}&excelFormat={{excelFormat}}")
                     .to("{{out}}");

         routeTemplate("xmltoexcel-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("includeHeader","true")
                 .templateParameter("includeIndexColumn","false")
                 .templateParameter("indexColumnName","line")
                 .templateParameter("orderHeaders","UNORDERED")
                 .templateParameter("excelFormat","XLSX")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("xmltoexcel://?includeHeader={{includeHeader}}&includeIndexColumn={{includeIndexColumn}}&indexColumnName={{indexColumnName}}&orderHeaders={{orderHeaders}}&excelFormat={{excelFormat}}");
    }

}
