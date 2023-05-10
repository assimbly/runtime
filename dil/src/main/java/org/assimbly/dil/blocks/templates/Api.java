package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Api extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("get-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:get:{{path}}?{{options}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("{{out}}");

         routeTemplate("get-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("rest:get:{{path}}?{{options}}")
                     .to("{{out}}");

         routeTemplate("get-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .to("rest:get:{{path}}?{{options}}");

         routeTemplate("post-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:post:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("post-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:post:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("post-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:post:{{path}}?{{options}}");

         routeTemplate("put-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:put:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("put-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:put:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("put-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:put:{{path}}?{{options}}");

         routeTemplate("delete-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:delete:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("delete-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:delete:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("delete-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:delete:{{path}}?{{options}}");

         routeTemplate("patch-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:patch:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("patch-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:patch:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("patch-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:patch:{{path}}?{{options}}");

         routeTemplate("head-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:head:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("head-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:head:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("head-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:head:{{path}}?{{options}}");

         routeTemplate("trace-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:trace:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("trace-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:trace:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("trace-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:trace:{{path}}?{{options}}");

         routeTemplate("options-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:options:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("options-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:options:{{path}}?{{options}}")
                 .to("{{out}}");

         routeTemplate("options-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:options:{{path}}?{{options}}");

         routeTemplate("connect-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("connect")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rest:connect:{{path}}?{{connect}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("connect-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("connect")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:connect:{{path}}?{{connect}}")
                 .to("{{out}}");

         routeTemplate("connect-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("connect")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("rest:connect:{{path}}?{{connect}}");

     }

}
