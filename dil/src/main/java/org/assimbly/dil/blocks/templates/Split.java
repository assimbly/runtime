package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class Split extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         //XPathBuilder myXpath = new XPathBuilder("").saxon().threadSafety(true);

         routeTemplate("split-xpath-router")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("expression")
                 .templateParameter("in")
                 .templateParameter("out","mock:0")
                 .templateParameter("out_rule")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                         .split().xpath("{{expression}}")
                         .setHeader("CamelSplitIndex").simple("${exchangeProperty.CamelSplitIndex}")
                         .setHeader("CamelSplitSize").simple("${exchangeProperty.CamelSplitSize}")
                         .setHeader("CamelSplitComplete").simple("${exchangeProperty.CamelSplitComplete.toString().trim()}")
                         .to("{{out_rule}}")
                    .end()
                    .choice()
                        .when(simple("{{out}}").not().isEqualTo("mock:0"))
                            .to("{{out}}");


    }

}
