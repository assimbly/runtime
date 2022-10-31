package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class QueueThrottle extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("queuethrottle-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("options")
                 .templateParameter("timePeriod","5000")
                 .templateParameter("maxRequests","1")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("quartz://1e7dff01-d069-11ec-83f5-3747809ef661_timer?trigger.repeatCount=-1&trigger.repeatInterval=5000&trigger.timeZone=Europe/Amsterdam")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("DovetailQueueName").constant("{{path}}")
                     .process("QueueMessageChecker")
                     .filter().simple("${exchangeProperty.DovetailQueueHasMessages} == true")
                     .loop().constant("{{maxRequests}}")
                        .copy()
                        .setProperty("Enrich-Type").constant("application/override")
                        .setProperty("DovetailAggregateNoExceptionOnNull").simple("true",Boolean.class)
                        .pollEnrich().constant("activemq:{{path}}").timeout("{{timePeriod}}").aggregationStrategy("CurrentEnrichStrategy")
                        .filter()
                            .simple("${body} != null")
                            .removeHeaders("fireTime|jobRunTime|nextFireTime|previousFireTime|refireCount|scheduledFireTime|triggerGroup|triggerName|jobDetail|jobInstance|mergedJobDataMap|result|scheduler|trigger","breadcrumbId")
                            .to("{{out}}");

     }

}
