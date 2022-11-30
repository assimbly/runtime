package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class SetMessage extends RouteBuilder {

     @Override
     //@BindToRegistry
     public void configure() throws Exception {

         routeTemplate("setmessage-action")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("headers","0")
                 .templateParameter("language", "constant")
                 .templateParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("assimbly.body").constant("{{path}}")
                 .setProperty("assimbly.language").constant("{{language}}")
                 .setProperty("assimbly.headers").constant("{{headers}}")
                 .process("SetBodyProcessor")
                 .process("SetHeadersProcessor")
                 .to("{{out}}");

         routeTemplate("setmessage-sink")
                 .templateParameter("routeconfiguration_id", "0")
                 .templateParameter("headers","0")
                 .templateParameter("language", "constant")
                 .templateParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("assimbly.body").constant("{{path}}")
                 .setProperty("assimbly.language").constant("{{language}}")
                 .setProperty("assimbly.headers").constant("{{headers}}")
                 .process("SetBodyProcessor")
                 .process("SetHeadersProcessor");

     }
}
