package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class RemoveCookie extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("removecookie-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("in")
                 .templateParameter("out")
                 .templateParameter("domain","org.assimbly")
                 .templateParameter("path","AssimblyCookie")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("bean://flowCookieStore?method=removeStringAsCookie(${exchange},'{{path}}','{{domain}}')")
                     .to("{{out}}");

         routeTemplate("removecookie-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("path","org.assimbly")
                 .templateParameter("name","AssimblyCookie")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("bean://flowCookieStore?method=removeStringAsCookie(${exchange},'{{path}}','{{domain}}')");

    }

}
