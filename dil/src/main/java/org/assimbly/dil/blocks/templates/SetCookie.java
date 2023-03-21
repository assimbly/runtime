package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetCookie extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("setcookie-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("domain","org.assimbly")
                 .templateParameter("path","AssimblyCookie")
                 .templateParameter("value")
                 .templateParameter("cookiePath","")
                 .templateParameter("isSecure","false")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("bean://flowCookieStore?method=addStringAsCookie(${exchange},'{{path}}','{{value}}','{{domain}}','{{cookiePath}}',{{isSecure}})")
                     .to("{{out}}");

         routeTemplate("setcookie-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("domain","org.assimbly")
                 .templateParameter("path","AssimblyCookie")
                 .templateParameter("value")
                 .templateParameter("cookiePath","")
                 .templateParameter("isSecure","false")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("bean://flowCookieStore?method=addStringAsCookie(${exchange},'{{path}}','{{value}}','{{domain}}','{{cookiePath}}',{{isSecure}})");
    }

}
