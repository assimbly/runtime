package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class RabbitMQ extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("rabbitmq-source")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("rabbitmq:{{path}}?{{options}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .to("{{out}}");

         routeTemplate("rabbitmq-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("excludedHeader_breadcrumbId").simple("${header.breadcrumbId}")
                     .removeHeader("breadcrumbId")
                     .setHeader("CamelRabbitmqDeliveryMode").constant("2")
                     .toD("rabbitmq:{{path}}?{{options}}")
                     .setHeader("breadcrumbId").simple("${exchangeProperty.excludedHeader_breadcrumbId}")
                     .removeProperty("excludedHeader_breadcrumbId")
                     .to("{{out}}");

         routeTemplate("rabbitmq-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateParameter("path")
                 .templateParameter("options")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("excludedHeader_breadcrumbId").simple("${header.breadcrumbId}")
                     .removeHeader("breadcrumbId")
                     .setHeader("CamelRabbitmqDeliveryMode").constant("2")
                     .toD("rabbitmq:{{path}}?{{options}}")
                     .setHeader("breadcrumbId").simple("${exchangeProperty.excludedHeader_breadcrumbId}")
                     .removeProperty("excludedHeader_breadcrumbId");

    }

}
