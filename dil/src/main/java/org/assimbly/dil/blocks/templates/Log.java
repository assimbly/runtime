package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Log extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("log-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("loggerName","org.assimbly.runtime")
                 .templateParameter("skipBodyLineSeparator","false")
                 .templateParameter("multiline","true")
                 .templateParameter("showHeaders","true")
                 .templateParameter("showBody","true")
                 .templateParameter("showBodyType","true")
                 .templateParameter("showProperties","false")
                 .templateParameter("showFiles","true")
                 .templateParameter("showException","true")
                 .templateParameter("showStackTrace","true")
                 .templateParameter("style","Tab")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("log:{{loggerName}}?skipBodyLineSeparator={{skipBodyLineSeparator}}&multiline={{multiline}}&showHeaders={{showHeaders}}&showBody={{showBody}}&showBodyType={{showBodyType}}&showProperties={{showProperties}}&showFiles={{showFiles}}&showException={{showException}}&showStackTrace={{showStackTrace}}&style={{style}}")
                     .to("{{out}}");

         routeTemplate("log-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("loggerName","org.assimbly.runtime")
                 .templateParameter("skipBodyLineSeparator","false")
                 .templateParameter("multiline","true")
                 .templateParameter("showHeaders","true")
                 .templateParameter("showBody","true")
                 .templateParameter("showBodyType","true")
                 .templateParameter("showProperties","false")
                 .templateParameter("showFiles","true")
                 .templateParameter("showException","true")
                 .templateParameter("showStackTrace","true")
                 .templateParameter("style","Tab")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("log:{{loggerName}}?skipBodyLineSeparator={{skipBodyLineSeparator}}&multiline={{multiline}}&showHeaders={{showHeaders}}&showBody={{showBody}}&showBodyType={{showBodyType}}&showProperties={{showProperties}}&showFiles={{showFiles}}&showException={{showException}}&showStackTrace={{showStackTrace}}&style={{style}}");

    }

}
