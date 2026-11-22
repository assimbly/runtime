package org.assimbly.dil.kamelets;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the mock-action DIL Kamelet for testing outbound connectors. */
class MockKameletTest {

    private static final String KAMELET_NAME = "mock-action";
    private static final String KAMELET_FILE = KAMELET_NAME + ".kamelet.yaml";

    private DefaultCamelContext context;

    @BeforeEach
    void startContext() throws Exception {
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
    void loadsAsDilRouteTemplate() {
        RouteTemplateDefinition template = context.getRouteTemplateDefinition(KAMELET_NAME);
        assertNotNull(template);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Set<String> params = template.getTemplateParameters().stream()
                .map(RouteTemplateParameterDefinition::getName)
                .collect(Collectors.toSet());

        assertTrue(params.containsAll(List.of(
                "path", "log", "reportGroup", "expectedCount", "retainFirst", "retainLast",
                "copyOnExchange", "failFast", "browseLimit",
                "in", "out", "sink", "routeId", "stepId", "routeConfigurationId")),
                "Missing parameters: " + params);
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
    void transpilesFromDilAsMockActionForSinkStep() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Orders</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>sink</type><uri>mock:outbound-test</uri>\
                <options><log>true</log><retainLast>5</retainLast></options>\
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
            template = properties.get("sink.5998.routetemplate");
        } finally {
            CustomKameletCatalog.removeName(KAMELET_FILE);
        }

        assertNotNull(template);
        assertTrue(template.contains("routeTemplateRef=\"mock-action\""), template);
        assertTrue(template.contains("name=\"path\" value=\"outbound-test\""), template);
        assertTrue(template.contains("name=\"log\" value=\"true\""), template);
        assertTrue(template.contains("name=\"retainLast\" value=\"5\""), template);
        assertTrue(template.contains("name=\"sink\" value=\"true\""), template);
    }

    @Test
    void actsAsSinkWhenSinkIsTrue() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("mock-sink-route")
                .parameter("routeId", "mock-sink-route")
                .parameter("stepId", "1")
                .parameter("in", "direct:start")
                .parameter("out", "mock:downstream")
                .parameter("sink", "true")
                .parameter("path", "outboundTest")
                .parameter("log", "true")
                .parameter("retainLast", "2")
                .add();

        context.start();

        MockEndpoint mockTarget = context.getEndpoints().stream()
                .filter(MockEndpoint.class::isInstance)
                .map(MockEndpoint.class::cast)
                .filter(ep -> ep.getEndpointUri().startsWith("mock://outboundTest"))
                .findFirst()
                .orElseThrow();
        MockEndpoint downstream = context.getEndpoint("mock:downstream", MockEndpoint.class);
        downstream.expectedMessageCount(0);

        try (ProducerTemplate producer = context.createProducerTemplate()) {
            producer.sendBody("direct:start", "one");
            producer.sendBody("direct:start", "two");
            producer.sendBody("direct:start", "three");
        }

        assertEquals(3, mockTarget.getReceivedCounter());
        assertEquals(2, mockTarget.getExchanges().size());
        assertEquals("two", mockTarget.getExchanges().get(0).getMessage().getBody(String.class));
        assertEquals("three", mockTarget.getExchanges().get(1).getMessage().getBody(String.class));
        assertTrue(mockTarget.isLog());
        assertEquals(2, mockTarget.getRetainLast());
        MockEndpoint.assertIsSatisfied(2, TimeUnit.SECONDS, downstream);
    }

    @Test
    void forwardsDownstreamWhenSinkIsFalse() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("mock-action-route")
                .parameter("routeId", "mock-action-route")
                .parameter("stepId", "2")
                .parameter("in", "direct:startAction")
                .parameter("out", "mock:afterMock")
                .parameter("sink", "false")
                .parameter("path", "tapEndpoint")
                .add();

        context.start();

        MockEndpoint tap = context.getEndpoint("mock:tapEndpoint", MockEndpoint.class);
        MockEndpoint after = context.getEndpoint("mock:afterMock", MockEndpoint.class);
        tap.expectedBodiesReceived("hello");
        after.expectedBodiesReceived("hello");

        try (ProducerTemplate producer = context.createProducerTemplate()) {
            producer.sendBody("direct:startAction", "hello");
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, tap, after);
    }
}
