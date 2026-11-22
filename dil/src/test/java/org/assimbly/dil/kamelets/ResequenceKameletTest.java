package org.assimbly.dil.kamelets;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ChoiceDefinition;
import org.apache.camel.model.ProcessorDefinition;
import org.apache.camel.model.ResequenceDefinition;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.model.StepDefinition;
import org.apache.camel.model.ToDefinition;
import org.apache.camel.model.config.BatchResequencerConfig;
import org.apache.camel.model.config.StreamResequencerConfig;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the resequence-action Kamelet metadata, DIL transpilation, batch/stream modes, and expression languages. */
class ResequenceKameletTest {

    private static final String KAMELET_NAME = "resequence-action";
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
    void loadsAsDilRouteTemplateWithBatchAndStreamResequenceEips() {
        RouteTemplateDefinition template = context.getRouteTemplateDefinition(KAMELET_NAME);
        assertNotNull(template, "Expected route template with id " + KAMELET_NAME);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Map<String, RouteTemplateParameterDefinition> params = template.getTemplateParameters().stream()
                .collect(Collectors.toMap(RouteTemplateParameterDefinition::getName, p -> p));

        assertTrue(params.keySet().containsAll(List.of(
                "language", "expression", "mode",
                "batchSize", "batchTimeout", "allowDuplicates", "reverse",
                "capacity", "timeout", "deliveryAttemptInterval", "rejectOld",
                "ignoreInvalidExchanges",
                "in", "out", "sink", "routeId", "stepId", "routeConfigurationId")),
                "Missing parameters: " + params.keySet());

        assertEquals("simple", params.get("language").getDefaultValue());
        assertEquals("batch", params.get("mode").getDefaultValue());
        assertEquals("100", params.get("batchSize").getDefaultValue());
        assertEquals("1000", params.get("batchTimeout").getDefaultValue());
        assertEquals("false", params.get("allowDuplicates").getDefaultValue());
        assertEquals("false", params.get("reverse").getDefaultValue());
        assertEquals("1000", params.get("capacity").getDefaultValue());
        assertEquals("1000", params.get("timeout").getDefaultValue());
        assertEquals("1000", params.get("deliveryAttemptInterval").getDefaultValue());
        assertEquals("false", params.get("rejectOld").getDefaultValue());
        assertEquals("false", params.get("ignoreInvalidExchanges").getDefaultValue());

        ProcessorDefinition<?> firstOutput = template.getRoute().getOutputs().getFirst();
        StepDefinition step = assertInstanceOf(StepDefinition.class, firstOutput);
        ChoiceDefinition choice = assertInstanceOf(ChoiceDefinition.class, step.getOutputs().getFirst());
        assertEquals(3, choice.getWhenClauses().size());

        // Stream + xpath branch
        ResequenceDefinition streamXpathReseq = assertInstanceOf(
                ResequenceDefinition.class, choice.getWhenClauses().get(0).getOutputs().getFirst());
        XPathExpression streamXpathExpr = assertInstanceOf(XPathExpression.class, streamXpathReseq.getExpression());
        assertEquals("{{expression}}", streamXpathExpr.getExpression());
        assertEquals("java.lang.String", streamXpathExpr.getResultTypeName());
        assertInstanceOf(StreamResequencerConfig.class, streamXpathReseq.getResequencerConfig());

        // Stream + generic language branch
        ResequenceDefinition streamReseq = assertInstanceOf(
                ResequenceDefinition.class, choice.getWhenClauses().get(1).getOutputs().getFirst());
        LanguageExpression streamExpr = assertInstanceOf(LanguageExpression.class, streamReseq.getExpression());
        assertEquals("{{language}}", streamExpr.getLanguage());
        assertEquals("{{expression}}", streamExpr.getExpression());
        StreamResequencerConfig streamConfig = assertInstanceOf(StreamResequencerConfig.class, streamReseq.getResequencerConfig());
        assertEquals("{{capacity}}", streamConfig.getCapacity());
        assertEquals("{{timeout}}", streamConfig.getTimeout());
        assertEquals("{{deliveryAttemptInterval}}", streamConfig.getDeliveryAttemptInterval());
        assertEquals("{{ignoreInvalidExchanges}}", streamConfig.getIgnoreInvalidExchanges());
        assertEquals("{{rejectOld}}", streamConfig.getRejectOld());
        ToDefinition streamTo = assertInstanceOf(ToDefinition.class, streamReseq.getOutputs().getFirst());
        assertEquals("{{out}}", streamTo.getUri());
        assertEquals("{{sink}}", streamTo.getDisabled());

        // Batch + xpath branch
        ResequenceDefinition batchXpathReseq = assertInstanceOf(
                ResequenceDefinition.class, choice.getWhenClauses().get(2).getOutputs().getFirst());
        XPathExpression batchXpathExpr = assertInstanceOf(XPathExpression.class, batchXpathReseq.getExpression());
        assertEquals("{{expression}}", batchXpathExpr.getExpression());
        assertEquals("java.lang.String", batchXpathExpr.getResultTypeName());
        assertInstanceOf(BatchResequencerConfig.class, batchXpathReseq.getResequencerConfig());

        // Batch + generic language branch (default)
        ResequenceDefinition batchReseq = assertInstanceOf(
                ResequenceDefinition.class, choice.getOtherwise().getOutputs().getFirst());
        LanguageExpression batchExpr = assertInstanceOf(LanguageExpression.class, batchReseq.getExpression());
        assertEquals("{{language}}", batchExpr.getLanguage());
        assertEquals("{{expression}}", batchExpr.getExpression());
        BatchResequencerConfig batchConfig = assertInstanceOf(BatchResequencerConfig.class, batchReseq.getResequencerConfig());
        assertEquals("{{batchSize}}", batchConfig.getBatchSize());
        assertEquals("{{batchTimeout}}", batchConfig.getBatchTimeout());
        assertEquals("{{allowDuplicates}}", batchConfig.getAllowDuplicates());
        assertEquals("{{reverse}}", batchConfig.getReverse());
        assertEquals("{{ignoreInvalidExchanges}}", batchConfig.getIgnoreInvalidExchanges());
        ToDefinition batchTo = assertInstanceOf(ToDefinition.class, batchReseq.getOutputs().getFirst());
        assertEquals("{{out}}", batchTo.getUri());
        assertEquals("{{sink}}", batchTo.getDisabled());
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
    void transpilesFromDilAsResequenceActionTemplate() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Orders</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>action</type><uri>resequence</uri>\
                <options><language>simple</language><expression>${header.seqnum}</expression><mode>stream</mode><capacity>500</capacity><timeout>250</timeout></options>\
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
        assertTrue(template.contains("routeTemplateRef=\"resequence-action\""), template);
        assertTrue(template.contains("name=\"language\" value=\"simple\""), template);
        assertTrue(template.contains("name=\"expression\" value=\"${header.seqnum}\""), template);
        assertTrue(template.contains("name=\"mode\" value=\"stream\""), template);
        assertTrue(template.contains("name=\"capacity\" value=\"500\""), template);
        assertTrue(template.contains("name=\"timeout\" value=\"250\""), template);
    }

    @Test
    void reordersMessagesInDefaultBatchMode() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-batch-default")
                .parameter("routeId", "reseq-batch-default")
                .parameter("stepId", "1")
                .parameter("in", "direct:start")
                .parameter("out", "mock:result")
                .parameter("expression", "${header.seqnum}")
                .parameter("batchSize", "3")
                .parameter("batchTimeout", "500")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("first", "second", "third");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "third", "seqnum", 3);
            template.sendBodyAndHeader("direct:start", "first", "seqnum", 1);
            template.sendBodyAndHeader("direct:start", "second", "seqnum", 2);
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, result);
    }

    @Test
    void batchModeSupportsReverseAndAllowDuplicates() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-batch-opts")
                .parameter("routeId", "reseq-batch-opts")
                .parameter("stepId", "2")
                .parameter("in", "direct:start")
                .parameter("out", "mock:result")
                .parameter("mode", "batch")
                .parameter("expression", "${header.seqnum}")
                .parameter("batchSize", "4")
                .parameter("batchTimeout", "500")
                .parameter("reverse", "true")
                .parameter("allowDuplicates", "true")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("three", "two-a", "two-b", "one");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "one", "seqnum", 1);
            template.sendBodyAndHeader("direct:start", "three", "seqnum", 3);
            template.sendBodyAndHeader("direct:start", "two-a", "seqnum", 2);
            template.sendBodyAndHeader("direct:start", "two-b", "seqnum", 2);
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, result);
    }

    @Test
    void reordersMessagesInStreamMode() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-stream")
                .parameter("routeId", "reseq-stream")
                .parameter("stepId", "3")
                .parameter("in", "direct:start")
                .parameter("out", "mock:result")
                .parameter("mode", "stream")
                .parameter("expression", "${header.seqnum}")
                .parameter("capacity", "100")
                .parameter("timeout", "200")
                .parameter("deliveryAttemptInterval", "50")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("one", "two", "three", "four");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "three", "seqnum", 3L);
            template.sendBodyAndHeader("direct:start", "one", "seqnum", 1L);
            template.sendBodyAndHeader("direct:start", "four", "seqnum", 4L);
            template.sendBodyAndHeader("direct:start", "two", "seqnum", 2L);
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, result);
    }

    @Test
    void supportsGroovyJsonpathAndXpathExpressionsInBatchAndStreamModes() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-groovy")
                .parameter("routeId", "reseq-groovy")
                .parameter("stepId", "4")
                .parameter("in", "direct:groovy")
                .parameter("out", "mock:groovyOut")
                .parameter("language", "groovy")
                .parameter("expression", "request.headers['seqnum']")
                .parameter("batchSize", "3")
                .parameter("batchTimeout", "500")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-jsonpath")
                .parameter("routeId", "reseq-jsonpath")
                .parameter("stepId", "5")
                .parameter("in", "direct:jsonpath")
                .parameter("out", "mock:jsonOut")
                .parameter("language", "jsonpath")
                .parameter("expression", "$.seq")
                .parameter("batchSize", "3")
                .parameter("batchTimeout", "500")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-xpath-batch")
                .parameter("routeId", "reseq-xpath-batch")
                .parameter("stepId", "6")
                .parameter("in", "direct:xpathBatch")
                .parameter("out", "mock:xpathBatchOut")
                .parameter("language", "xpath")
                .parameter("expression", "/msg/seq")
                .parameter("batchSize", "3")
                .parameter("batchTimeout", "500")
                .add();
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-xpath-stream")
                .parameter("routeId", "reseq-xpath-stream")
                .parameter("stepId", "7")
                .parameter("in", "direct:xpathStream")
                .parameter("out", "mock:xpathStreamOut")
                .parameter("language", "xpath")
                .parameter("expression", "/msg/seq")
                .parameter("mode", "stream")
                .parameter("timeout", "200")
                .parameter("deliveryAttemptInterval", "50")
                .add();
        context.start();

        MockEndpoint groovyOut = context.getEndpoint("mock:groovyOut", MockEndpoint.class);
        MockEndpoint jsonOut = context.getEndpoint("mock:jsonOut", MockEndpoint.class);
        MockEndpoint xpathBatchOut = context.getEndpoint("mock:xpathBatchOut", MockEndpoint.class);
        MockEndpoint xpathStreamOut = context.getEndpoint("mock:xpathStreamOut", MockEndpoint.class);
        groovyOut.expectedBodiesReceived("a", "b", "c");
        jsonOut.expectedBodiesReceived("{\"seq\":1}", "{\"seq\":2}", "{\"seq\":3}");
        xpathBatchOut.expectedBodiesReceived("<msg><seq>1</seq></msg>", "<msg><seq>2</seq></msg>", "<msg><seq>3</seq></msg>");
        xpathStreamOut.expectedBodiesReceived("<msg><seq>1</seq></msg>", "<msg><seq>2</seq></msg>", "<msg><seq>3</seq></msg>");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:groovy", "c", "seqnum", 3);
            template.sendBodyAndHeader("direct:groovy", "a", "seqnum", 1);
            template.sendBodyAndHeader("direct:groovy", "b", "seqnum", 2);

            template.sendBody("direct:jsonpath", "{\"seq\":3}");
            template.sendBody("direct:jsonpath", "{\"seq\":1}");
            template.sendBody("direct:jsonpath", "{\"seq\":2}");

            template.sendBody("direct:xpathBatch", "<msg><seq>3</seq></msg>");
            template.sendBody("direct:xpathBatch", "<msg><seq>1</seq></msg>");
            template.sendBody("direct:xpathBatch", "<msg><seq>2</seq></msg>");

            template.sendBody("direct:xpathStream", "<msg><seq>3</seq></msg>");
            template.sendBody("direct:xpathStream", "<msg><seq>1</seq></msg>");
            template.sendBody("direct:xpathStream", "<msg><seq>2</seq></msg>");
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, groovyOut, jsonOut, xpathBatchOut, xpathStreamOut);
    }

    @Test
    void ignoresInvalidExchangesWhenEnabled() throws Exception {
        TemplatedRouteBuilder.builder(context, KAMELET_NAME)
                .routeId("reseq-ignore-invalid")
                .parameter("routeId", "reseq-ignore-invalid")
                .parameter("stepId", "8")
                .parameter("in", "direct:start")
                .parameter("out", "mock:result")
                .parameter("mode", "batch")
                .parameter("expression", "${header.seqnum}")
                .parameter("batchSize", "2")
                .parameter("batchTimeout", "300")
                .parameter("ignoreInvalidExchanges", "true")
                .add();
        context.start();

        MockEndpoint result = context.getEndpoint("mock:result", MockEndpoint.class);
        result.expectedBodiesReceived("one", "two");

        try (ProducerTemplate template = context.createProducerTemplate()) {
            template.sendBodyAndHeader("direct:start", "two", "seqnum", 2);
            template.sendBody("direct:start", "missing-seqnum");
            template.sendBodyAndHeader("direct:start", "one", "seqnum", 1);
        }

        MockEndpoint.assertIsSatisfied(5, TimeUnit.SECONDS, result);
    }
}
