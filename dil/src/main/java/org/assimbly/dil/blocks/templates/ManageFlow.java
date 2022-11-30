package org.assimbly.dil.blocks.templates;

import org.apache.camel.builder.RouteBuilder;

public class ManageFlow extends RouteBuilder {

     @Override
     public void configure() throws Exception {

         routeTemplate("manageflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateOptionalParameter("action","startflow")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("{{action}}")
                     .process("ManageFlowProcessor")
                     .to("{{out}}");

         routeTemplate("manageflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateOptionalParameter("action","startflow")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("{{action}}")
                     .process("ManageFlowProcessor");

         routeTemplate("startflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("startflow")
                     .process("ManageFlowProcessor")
                     .to("{{out}}");

         routeTemplate("startflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("startflow")
                     .process("ManageFlowProcessor");

         routeTemplate("stopflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("stopflow")
                     .process("ManageFlowProcessor")
                     .to("{{out}}");

         routeTemplate("stopflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("stopflow")
                     .process("ManageFlowProcessor");

         routeTemplate("suspendflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                     .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("suspendflow")
                     .process("ManageFlowProcessor")
                     .to("{{out}}");

         routeTemplate("suspendflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                     .setProperty("flowId").constant("{{path}}")
                     .setProperty("action").constant("suspendflow")
                     .process("ManageFlowProcessor");

         routeTemplate("pauseflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("pauseflow")
                 .process("ManageFlowProcessor")
                 .to("{{out}}");

         routeTemplate("pauseflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("pauseflow")
                 .process("ManageFlowProcessor");

         routeTemplate("resumeflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("resumeflow")
                 .process("ManageFlowProcessor")
                 .to("{{out}}");

         routeTemplate("resumeflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("resumeflow")
                 .process("ManageFlowProcessor");

         routeTemplate("continueflow-action")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .templateParameter("out")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("continueflow")
                 .process("ManageFlowProcessor")
                 .to("{{out}}");

         routeTemplate("continueflow-sink")
                 .templateParameter("routeconfiguration_id","0")
                 .templateOptionalParameter("path")
                 .templateParameter("in")
                 .from("{{in}}")
                 .routeConfigurationId("{{routeconfiguration_id}}")
                 .setProperty("flowId").constant("{{path}}")
                 .setProperty("action").constant("continueflow")
                 .process("ManageFlowProcessor");

    }

}
