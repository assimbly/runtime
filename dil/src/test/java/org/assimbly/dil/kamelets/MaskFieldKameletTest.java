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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the mask-field-action DIL Kamelet and MaskFieldProcessor. */
class MaskFieldKameletTest {

    private static final String KAMELET_NAME = "mask-field-action";
    private static final String KAMELET_FILE = KAMELET_NAME + ".kamelet.yaml";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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
                "fields", "replacement", "in", "out", "sink", "routeId", "stepId", "routeConfigurationId")),
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
    void transpilesFromDilAsMaskFieldActionTemplate() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Privacy</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>action</type><uri>mask-field</uri>\
                <options><fields>password,ssn</fields><replacement>REDACTED</replacement></options>\
                <links><link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link>\
                <link><id>5914-5997</id><bound>out</bound><transport>sync</transport></link></links></step>\
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
            template = properties.get("action.5998.routetemplate");
        } finally {
            CustomKameletCatalog.removeName(KAMELET_FILE);
        }

        assertNotNull(template);
        assertTrue(template.contains("routeTemplateRef=\"mask-field-action\""), template);
        assertTrue(template.contains("name=\"fields\" value=\"password,ssn\""), template);
        assertTrue(template.contains("name=\"replacement\" value=\"REDACTED\""), template);
    }

    @Test
    void masksTopLevelAndNestedFieldsAndArrays() throws Exception {
        addRoute("mask-1", "direct:mask1", "mock:out1", false, "password, creditCard", "****");
        context.start();

        MockEndpoint out = context.getEndpoint("mock:out1", MockEndpoint.class);
        out.expectedMessageCount(1);

        String input = """
                {
                  "username": "alice",
                  "password": "secret",
                  "nullField": null,
                  "billing": {
                    "creditCard": "4111-1111",
                    "city": "Amsterdam"
                  },
                  "accounts": [
                    {"id": 1, "password": "p1"},
                    {"id": 2, "password": null}
                  ]
                }""";

        try (ProducerTemplate producer = context.createProducerTemplate()) {
            producer.sendBodyAndHeader("direct:mask1", input, "Content-Type", "application/json");
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, out);
        String resultBody = out.getExchanges().getFirst().getMessage().getBody(String.class);
        assertEquals("application/json", out.getExchanges().getFirst().getMessage().getHeader("Content-Type"));

        JsonNode result = MAPPER.readTree(resultBody);
        assertEquals("alice", result.get("username").asString());
        assertEquals("****", result.get("password").asString());
        assertTrue(result.get("nullField").isNull());
        assertEquals("****", result.get("billing").get("creditCard").asString());
        assertEquals("Amsterdam", result.get("billing").get("city").asString());
        assertEquals("****", result.get("accounts").get(0).get("password").asString());
        assertTrue(result.get("accounts").get(1).get("password").isNull());
    }

    @Test
    void supportsDottedPathsAndTypedNumericMasking() throws Exception {
        addRoute("mask-2", "direct:mask2", "mock:out2", true, "customer.ssn, pin", "0");
        context.start();

        String input = """
                {
                  "customer": {"ssn": "123-45-6789"},
                  "other": {"ssn": "keep-me"},
                  "pin": 9876
                }""";

        String resultBody;
        try (ProducerTemplate producer = context.createProducerTemplate()) {
            resultBody = producer.requestBody("direct:mask2", input, String.class);
        }

        JsonNode result = MAPPER.readTree(resultBody);
        assertEquals("0", result.get("customer").get("ssn").asString());
        assertEquals("keep-me", result.get("other").get("ssn").asString());
        assertTrue(result.get("pin").isInt());
        assertEquals(0, result.get("pin").asInt());
    }

    @Test
    void masksNumericAndBooleanWithStringReplacementAndPreservesMapBody() throws Exception {
        addRoute("mask-3", "direct:mask3", "mock:out3", true, "age, active", "REDACTED");
        context.start();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", "bob");
        payload.put("age", 42);
        payload.put("active", true);

        Object result;
        try (ProducerTemplate producer = context.createProducerTemplate()) {
            result = producer.requestBody("direct:mask3", payload);
        }

        Map<?, ?> resultMap = assertInstanceOf(Map.class, result);
        assertEquals("bob", resultMap.get("name"));
        assertEquals("REDACTED", resultMap.get("age"));
        assertEquals("REDACTED", resultMap.get("active"));
    }

    @Test
    void preservesByteArrayBodyAndHandlesSinkTrue() throws Exception {
        addRoute("mask-4", "direct:mask4", "mock:unused", true, "secret", "XXX");
        context.start();

        MockEndpoint unused = context.getEndpoint("mock:unused", MockEndpoint.class);
        unused.expectedMessageCount(0);

        byte[] input = "{\"secret\":\"top\",\"public\":\"ok\"}".getBytes(StandardCharsets.UTF_8);
        Object result;
        try (ProducerTemplate producer = context.createProducerTemplate()) {
            result = producer.requestBody("direct:mask4", input);
        }

        byte[] outBytes = assertInstanceOf(byte[].class, result);
        JsonNode parsed = MAPPER.readTree(outBytes);
        assertEquals("XXX", parsed.get("secret").asString());
        assertEquals("ok", parsed.get("public").asString());
        assertFalse(parsed.get("secret").asString().contains("top"));
        MockEndpoint.assertIsSatisfied(2, TimeUnit.SECONDS, unused);
    }

    private void addRoute(String routeId, String in, String out, boolean sink, String fields, String replacement) throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId(routeId)
                .parameter("routeId", routeId)
                .parameter("stepId", "1")
                .parameter("in", in)
                .parameter("out", out)
                .parameter("sink", String.valueOf(sink))
                .parameter("fields", fields)
                .parameter("replacement", replacement)
                .add();
    }
}
