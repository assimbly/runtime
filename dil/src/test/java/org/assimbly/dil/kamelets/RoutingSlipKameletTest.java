package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.model.RoutingSlipDefinition;
import org.apache.camel.model.StepDefinition;
import org.apache.camel.model.language.LanguageExpression;
import org.apache.camel.model.language.XPathExpression;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the routingslip-router Kamelet metadata, DIL transpilation, expression languages, and routing slip options. */
class RoutingSlipKameletTest {

    private static final String KAMELET_NAME = "routingslip-router";
    private static final String KAMELET_FILE = KAMELET_NAME + ".kamelet.yaml";

    private DefaultCamelContext context;

    @BeforeEach
    void createContext() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + KAMELET_FILE));
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void loadsAsDilRouteTemplateWithRoutingSlipEip() {
        RouteTemplateDefinition template = context.getRouteTemplateDefinition(KAMELET_NAME);
        assertNotNull(template, "Expected route template with id " + KAMELET_NAME);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Map<String, RouteTemplateParameterDefinition> params = template.getTemplateParameters().stream()
                .collect(Collectors.toMap(RouteTemplateParameterDefinition::getName, p -> p));

        assertTrue(params.keySet().containsAll(List.of(
                "language", "expression", "uriDelimiter", "ignoreInvalidEndpoints",
                "in", "out", "routeId", "stepId", "routeConfigurationId")),
                "Missing parameters: " + params.keySet());
        assertEquals("simple", params.get("language").getDefaultValue());
        assertEquals(",", params.get("uriDelimiter").getDefaultValue());
        assertEquals("false", params.get("ignoreInvalidEndpoints").getDefaultValue());

        ProcessorDefinition<?> firstOutput = template.getRoute().getOutputs().getFirst();
        StepDefinition step = assertInstanceOf(StepDefinition.class, firstOutput);
        ChoiceDefinition choice = assertInstanceOf(ChoiceDefinition.class, step.getOutputs().getFirst());

        RoutingSlipDefinition xpathSlip = assertInstanceOf(
                RoutingSlipDefinition.class, choice.getWhenClauses().getFirst().getOutputs().getFirst());
        XPathExpression xpathExpr = assertInstanceOf(XPathExpression.class, xpathSlip.getExpression());
        assertEquals("{{expression}}", xpathExpr.getExpression());
        assertEquals("java.lang.String", xpathExpr.getResultTypeName());
        assertEquals("{{uriDelimiter}}", xpathSlip.getUriDelimiter());
        assertEquals("{{ignoreInvalidEndpoints}}", xpathSlip.getIgnoreInvalidEndpoints());

        RoutingSlipDefinition genericSlip = assertInstanceOf(
                RoutingSlipDefinition.class, choice.getOtherwise().getOutputs().getFirst());
        LanguageExpression langExpr = assertInstanceOf(LanguageExpression.class, genericSlip.getExpression());
        assertEquals("{{language}}", langExpr.getLanguage());
        assertEquals("{{expression}}", langExpr.getExpression());
        assertEquals("{{uriDelimiter}}", genericSlip.getUriDelimiter());
        assertEquals("{{ignoreInvalidEndpoints}}", genericSlip.getIgnoreInvalidEndpoints());
    }

    @Test
    void listedInClasspathIndex() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/kamelets/index.txt")) {
            assertNotNull(is, "/kamelets/index.txt should be generated on the classpath");
            Set<String> indexed = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toSet());

            assertTrue(indexed.contains(KAMELET_FILE), "Expected " + KAMELET_FILE + " in /kamelets/index.txt");
        }
    }

    @Test
    void transpilesFromDilAsRoutingSlipRouterTemplate() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Orders</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>router</type><uri>routingslip</uri>\
                <options><language>simple</language><expression>${header.mySlip}</expression><uriDelimiter>;</uriDelimiter><ignoreInvalidEndpoints>true</ignoreInvalidEndpoints></options>\
                <links><link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link></links></step>\
                </steps></flow></flows></integration></integrations><core><connections/><messages/>\
                <routes/><routeConfigurations/></core></dil>""";

        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        CustomKameletCatalog.addName(KAMELET_FILE);
        String template;
        try {
            TreeMap<String, String> properties = new Unmarshall().getProperties(conf, "5914");
            template = properties.get("router.5998.routetemplate");
        } finally {
            CustomKameletCatalog.removeName(KAMELET_FILE);
        }

        assertNotNull(template);
        assertTrue(template.contains("routeTemplateRef=\"routingslip-router\""), template);
        assertTrue(template.contains("name=\"language\" value=\"simple\""), template);
        assertTrue(template.contains("name=\"expression\" value=\"${header.mySlip}\""), template);
        assertTrue(template.contains("name=\"uriDelimiter\" value=\";\""), template);
        assertTrue(template.contains("name=\"ignoreInvalidEndpoints\" value=\"true\""), template);
    }

    @Test
    void routesSequentiallyWithSimpleExpressionAndDefaultDelimiter() throws Exception {
        addPipelineRoutes(context);
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-simple")
                .parameter("routeId", "rs-simple")
                .parameter("stepId", "1")
                .parameter("in", "direct:start")
                .parameter("expression", "${header.mySlip}")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("init-A-B");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "init", "mySlip", "direct:stepA,direct:stepB,mock:result");
        }

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void supportsCustomUriDelimiter() throws Exception {
        addPipelineRoutes(context);
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-delim")
                .parameter("routeId", "rs-delim")
                .parameter("stepId", "2")
                .parameter("in", "direct:start")
                .parameter("expression", "${header.mySlip}")
                .parameter("uriDelimiter", ";")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("init-B-A");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "init", "mySlip", "direct:stepB;direct:stepA;mock:result");
        }

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void honorsIgnoreInvalidEndpointsOption() throws Exception {
        addPipelineRoutes(context);
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-ignore")
                .parameter("routeId", "rs-ignore")
                .parameter("stepId", "3")
                .parameter("in", "direct:ignore")
                .parameter("expression", "${header.mySlip}")
                .parameter("ignoreInvalidEndpoints", "true")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-strict")
                .parameter("routeId", "rs-strict")
                .parameter("stepId", "4")
                .parameter("in", "direct:strict")
                .parameter("expression", "${header.mySlip}")
                .parameter("ignoreInvalidEndpoints", "false")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("init-A");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:ignore", "init", "mySlip", "direct:stepA,nonexistent:foo,mock:result");
            assertThrows(CamelExecutionException.class, () ->
                    template.sendBodyAndHeader("direct:strict", "init", "mySlip", "direct:stepA,nonexistent:foo,mock:result"));
        }

        MockEndpoint.assertIsSatisfied(context);
    }

    @Test
    void supportsGroovyJsonpathAndXpathLanguages() throws Exception {
        addPipelineRoutes(context);
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-groovy")
                .parameter("routeId", "rs-groovy")
                .parameter("stepId", "5")
                .parameter("in", "direct:groovy")
                .parameter("language", "groovy")
                .parameter("expression", "request.headers['mySlip']")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-jsonpath")
                .parameter("routeId", "rs-jsonpath")
                .parameter("stepId", "6")
                .parameter("in", "direct:jsonpath")
                .parameter("language", "jsonpath")
                .parameter("expression", "$.slip")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("rs-xpath")
                .parameter("routeId", "rs-xpath")
                .parameter("stepId", "7")
                .parameter("in", "direct:xpath")
                .parameter("language", "xpath")
                .parameter("expression", "/order/slip")
                .add();
        context.start();

        MockEndpoint groovyOut = context.getEndpoint("mock:groovyOut", MockEndpoint.class);
        MockEndpoint jsonOut = context.getEndpoint("mock:jsonOut", MockEndpoint.class);
        MockEndpoint xpathOut = context.getEndpoint("mock:xpathOut", MockEndpoint.class);
        groovyOut.expectedBodiesReceived("g-A");
        jsonOut.expectedMessageCount(1);
        xpathOut.expectedMessageCount(1);

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:groovy", "g", "mySlip", "direct:stepA,mock:groovyOut");
            template.sendBody("direct:jsonpath", "{\"slip\":\"direct:stepA,mock:jsonOut\"}");
            template.sendBody("direct:xpath", "<order><slip>direct:stepB,mock:xpathOut</slip></order>");
        }

        MockEndpoint.assertIsSatisfied(context);
        assertTrue(jsonOut.getExchanges().getFirst().getMessage().getBody(String.class).endsWith("-A"));
        assertTrue(xpathOut.getExchanges().getFirst().getMessage().getBody(String.class).endsWith("-B"));
    }

    private static void addPipelineRoutes(CamelContext ctx) throws Exception {
        ctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:stepA").setBody(simple("${body}-A"));
                from("direct:stepB").setBody(simple("${body}-B"));
            }
        });
    }
}
