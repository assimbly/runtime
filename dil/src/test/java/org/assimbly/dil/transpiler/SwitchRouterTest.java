package org.assimbly.dil.transpiler;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.util.IntegrationUtil;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Switch Router dispatches on the literal value of its selector: one out link per value, plus a default. */
class SwitchRouterTest {

    private static final String FLOW_ID = "5914";
    private static final String ROUTE_KEY = "router.5998.route";

    @Test
    void outLinksBecomeCasesAndTheLinkWithoutRuleTheOtherwise() throws Exception {
        String route = transpile();

        assertTrue(route.contains("<switch>"), route);
        assertTrue(route.contains("<selector><simple>${header.department}</simple></selector>"), route);
        assertTrue(route.contains("<case uri=\"direct:billing\" value=\"billing\"/>")
                || route.contains("<case value=\"billing\" uri=\"direct:billing\"/>"), route);
        assertTrue(route.contains("value=\"technical\""), route);
        assertTrue(route.contains("<otherwise uri=\"direct:review\"/>"), route);
        assertTrue(route.indexOf("<otherwise") > route.lastIndexOf("<case"), route);
    }

    @Test
    void routesByValueIgnoringCaseAndFallsBackToOtherwise() throws Exception {
        String route = transpile();

        try (CamelContext context = new DefaultCamelContext()) {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:billing").setBody(constant("billing"));
                    from("direct:technical").setBody(constant("technical"));
                    from("direct:review").setBody(constant("review"));
                }
            });
            PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource(route));
            context.start();

            try (ProducerTemplate template = context.createProducerTemplate()) {
                assertEquals("billing", template.requestBodyAndHeader("direct:in", "", "department", "BILLING"));
                assertEquals("technical", template.requestBodyAndHeader("direct:in", "", "department", "technical"));
                assertEquals("review", template.requestBodyAndHeader("direct:in", "", "department", "sales"));
                assertEquals("review", template.requestBody("direct:in", ""));
            }
        }
    }

    private String transpile() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Tickets</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>router</type><uri>switch</uri>\
                <options><language>simple</language><expression>${header.department}</expression></options>\
                <links>\
                <link><id>in</id><bound>in</bound><transport>direct</transport></link>\
                <link><id>billing</id><bound>out</bound><transport>direct</transport><rule>billing</rule></link>\
                <link><id>technical</id><bound>out</bound><transport>direct</transport><rule>technical</rule></link>\
                <link><id>review</id><bound>out</bound><transport>direct</transport></link>\
                </links></step>\
                </steps></flow></flows></integration></integrations><core><connections/><messages/>\
                <routes/><routeConfigurations/></core></dil>""";

        // XMLFileConfiguration does the same, but also validates against the DIL schema
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        TreeMap<String, String> properties = new Unmarshall().getProperties(conf, FLOW_ID);

        return properties.get(ROUTE_KEY);
    }
}
