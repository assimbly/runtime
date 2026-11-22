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

/** Switch and SwitchByHeader routers dispatch on literal selector values: one out link per case, plus a default. */
class SwitchRouterTest {

    private static final String FLOW_ID = "5914";
    private static final String ROUTE_KEY = "router.5998.route";

    @Test
    void outLinksBecomeCasesAndTheLinkWithoutRuleTheOtherwise() throws Exception {
        String route = transpileSwitch("simple", "${header.department}");

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
        String route = transpileSwitch("simple", "${header.department}");

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

    @Test
    void switchSupportsGroovyJsonpathAndXpathSelectors() throws Exception {
        String groovyRoute = transpileSwitch("groovy", "request.headers['department']");
        assertTrue(groovyRoute.contains("<selector><groovy>request.headers['department']</groovy></selector>"), groovyRoute);

        try (CamelContext context = new DefaultCamelContext()) {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:billing").setBody(constant("billing"));
                    from("direct:technical").setBody(constant("technical"));
                    from("direct:review").setBody(constant("review"));
                }
            });
            PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource(groovyRoute));
            context.start();

            try (ProducerTemplate template = context.createProducerTemplate()) {
                assertEquals("billing", template.requestBodyAndHeader("direct:in", "", "department", "billing"));
                assertEquals("review", template.requestBodyAndHeader("direct:in", "", "department", "sales"));
            }
        }

        String jsonRoute = transpileSwitch("jsonpath", "$.department");
        assertTrue(jsonRoute.contains("<jsonpath resultType=\"java.lang.String\">$.department</jsonpath>"), jsonRoute);

        try (CamelContext context = new DefaultCamelContext()) {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:billing").setBody(constant("billing"));
                    from("direct:technical").setBody(constant("technical"));
                    from("direct:review").setBody(constant("review"));
                }
            });
            PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource(jsonRoute));
            context.start();

            try (ProducerTemplate template = context.createProducerTemplate()) {
                assertEquals("billing", template.requestBody("direct:in", "{\"department\":\"billing\"}"));
                assertEquals("technical", template.requestBody("direct:in", "{\"department\":\"technical\"}"));
                assertEquals("review", template.requestBody("direct:in", "{\"department\":\"sales\"}"));
            }
        }

        String xpathRoute = transpileSwitch("xpath", "/ticket/department");
        assertTrue(xpathRoute.contains("saxon=\"true\""), xpathRoute);
        assertTrue(xpathRoute.contains("resultType=\"java.lang.String\""), xpathRoute);
        assertTrue(xpathRoute.contains("/ticket/department</xpath>"), xpathRoute);

        try (CamelContext context = new DefaultCamelContext()) {
            context.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:billing").setBody(constant("billing"));
                    from("direct:technical").setBody(constant("technical"));
                    from("direct:review").setBody(constant("review"));
                }
            });
            PluginHelper.getRoutesLoader(context).loadRoutes(IntegrationUtil.setResource(xpathRoute));
            context.start();

            try (ProducerTemplate template = context.createProducerTemplate()) {
                assertEquals("billing", template.requestBody("direct:in", "<ticket><department>billing</department></ticket>"));
                assertEquals("review", template.requestBody("direct:in", "<ticket><department>sales</department></ticket>"));
            }
        }
    }

    @Test
    void switchByHeaderGeneratesHeaderSelectorAndRoutesByHeaderValue() throws Exception {
        String route = transpile("""
                <step><id>5998</id><type>router</type><uri>switchbyheader</uri>\
                <options><headerName>department</headerName></options>\
                <links>\
                <link><id>in</id><bound>in</bound><transport>direct</transport></link>\
                <link><id>billing</id><bound>out</bound><transport>direct</transport><rule>billing</rule></link>\
                <link><id>technical</id><bound>out</bound><transport>direct</transport><rule>technical</rule></link>\
                <link><id>review</id><bound>out</bound><transport>direct</transport></link>\
                </links></step>""");

        assertTrue(route.contains("<switch>"), route);
        assertTrue(route.contains("<selector><header>department</header></selector>"), route);
        assertTrue(route.contains("<case uri=\"direct:billing\" value=\"billing\"/>")
                || route.contains("<case value=\"billing\" uri=\"direct:billing\"/>"), route);
        assertTrue(route.contains("value=\"technical\""), route);
        assertTrue(route.contains("<otherwise uri=\"direct:review\"/>"), route);
        assertTrue(route.indexOf("<otherwise") > route.lastIndexOf("<case"), route);

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

    @Test
    void switchByHeaderSupportsPathHeaderNameAndHeaderValueLinkOption() throws Exception {
        String route = transpile("""
                <step><id>5998</id><type>router</type><uri>switchbyheader:department</uri>\
                <links>\
                <link><id>in</id><bound>in</bound><transport>direct</transport></link>\
                <link><id>billing</id><bound>out</bound><transport>direct</transport><options><headerValue>billing</headerValue></options></link>\
                <link><id>technical</id><bound>out</bound><transport>direct</transport><options><headerValue>technical</headerValue></options></link>\
                <link><id>review</id><bound>out</bound><transport>direct</transport></link>\
                </links></step>""");

        assertTrue(route.contains("<selector><header>department</header></selector>"), route);
        assertTrue(route.contains("value=\"billing\""), route);
        assertTrue(route.contains("value=\"technical\""), route);
        assertTrue(route.contains("<otherwise uri=\"direct:review\"/>"), route);
    }

    private String transpileSwitch(String language, String expression) throws Exception {
        return transpile("""
                <step><id>5998</id><type>router</type><uri>switch</uri>\
                <options><language>%s</language><expression>%s</expression></options>\
                <links>\
                <link><id>in</id><bound>in</bound><transport>direct</transport></link>\
                <link><id>billing</id><bound>out</bound><transport>direct</transport><rule>billing</rule></link>\
                <link><id>technical</id><bound>out</bound><transport>direct</transport><rule>technical</rule></link>\
                <link><id>review</id><bound>out</bound><transport>direct</transport></link>\
                </links></step>""".formatted(language, expression));
    }

    private String transpile(String stepXml) throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Tickets</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                %s\
                </steps></flow></flows></integration></integrations><core><connections/><messages/>\
                <routes/><routeConfigurations/></core></dil>""".formatted(stepXml);

        // XMLFileConfiguration does the same, but also validates against the DIL schema
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        TreeMap<String, String> properties = new Unmarshall().getProperties(conf, FLOW_ID);

        return properties.get(ROUTE_KEY);
    }
}
