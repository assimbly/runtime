package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
import org.assimbly.integrationrest.utils.MavenUtil;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = StatisticsRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationRuntime.class,
        IntegrationConfig.class,
        FlowManagerRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
class StatisticsRuntimeTest {

    @Autowired
    private IntegrationRuntime integrationRuntime;

    @Autowired
    private MockMvc mockMvc;

    public enum CamelPropertyField {
        id,
        routeId1,
        routeId2,
        camelContext
    }

    Properties camelContextProp = buildCamelContextExample();

    @BeforeEach
    void beforeEach() throws Exception{
        integrationRuntime.getIntegration().getContext().start();

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );
    }

    @Test
    void shouldGetStats() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/stats", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].flow.total").value(0))
                .andExpect(jsonPath("$[0].flow.uptimeMillis").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].flow.tracing").value(false))
                .andExpect(jsonPath("$[0].flow.pending").value(0))
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("$[0].flow.completed").value(0))
                .andExpect(jsonPath("$[0].flow.failed").value(0))
                .andExpect(jsonPath("$[0].flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].flow.status").value("Started"))
        ;
    }

    @Test
    void shouldGetMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/messages", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].flow.total").value(0))
                .andExpect(jsonPath("$[0].flow.pending").value(0))
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("$[0].flow.completed").value(0))
                .andExpect(jsonPath("$[0].flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetStatsByFlowIds() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/statsbyflowids", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,
                        "flowIds", (String)camelContextProp.get(CamelPropertyField.id.name())),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].flow.total").value(0))
                .andExpect(jsonPath("$[0].flow.uptimeMillis").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].flow.tracing").value(false))
                .andExpect(jsonPath("$[0].flow.pending").value(0))
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("$[0].flow.completed").value(0))
                .andExpect(jsonPath("$[0].flow.failed").value(0))
                .andExpect(jsonPath("$[0].flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].flow.status").value("Started"))
        ;
    }

    @Test
    void shouldGetStatsByFlowId() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.getProperty(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.getProperty(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetStatsByFlowIdWithFullStats() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,"FullStats","true"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.uptimeMillis").value(greaterThan(0)))
                .andExpect(jsonPath("flow.tracing").value(false))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
                .andExpect(jsonPath("flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("flow.status").value("Started"))
                .andExpect(jsonPath("flow.steps").doesNotExist())
        ;
    }

    @Test
    void shouldGetStatsByFlowIdWithFullStatsAndIncludeSteps() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,"FullStats","true","includeSteps","true"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.uptimeMillis").value(greaterThan(0)))
                .andExpect(jsonPath("flow.tracing").value(false))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
                .andExpect(jsonPath("flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("flow.status").value("Started"))
                .andExpect(jsonPath("flow.steps").value(notNullValue()))
                .andExpect(jsonPath("flow.steps[*].step.id").value(hasItem(String.format("%s-%s",
                        camelContextProp.get(CamelPropertyField.id.name()),
                        camelContextProp.get(CamelPropertyField.routeId1.name())))))
        ;
    }

    @Test
    void shouldGetFlowStepStats() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/step/%s/stats", 1,
                        camelContextProp.get(CamelPropertyField.id.name()),
                        camelContextProp.get(CamelPropertyField.routeId1.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,"FullStats",
                        "true","includeSteps","true"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("step.id").value(String.format("%s-%s",
                        camelContextProp.get(CamelPropertyField.id.name()),
                        camelContextProp.get(CamelPropertyField.routeId1.name()))))
                .andExpect(jsonPath("step.status").value("started"))
        ;
    }

    @Test
    void shouldGetFlowMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetFlowTotalMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages/total", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("0"))
        ;
    }

    @Test
    void shouldGetFlowCompletedMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages/completed", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("0"))
        ;
    }

    @Test
    void shouldGetFlowFailedMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages/failed", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("0"))
        ;
    }

    @Test
    void shouldGetFlowPendingMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages/pending", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("0"))
        ;
    }

    @Test
    void shouldGetStepMessages() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/step/%s/messages", 1,
                        camelContextProp.get(CamelPropertyField.id.name()), camelContextProp.get(CamelPropertyField.routeId1.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("step.total").value(0))
                .andExpect(jsonPath("step.pending").value(0))
                .andExpect(jsonPath("step.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("step.completed").value(0))
                .andExpect(jsonPath("step.failed").value(0))
        ;
    }

    @Test
    void shouldGetMetrics() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/metrics", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("version").value(MavenUtil.getModelVersion()))
                .andExpect(jsonPath(String.format("timers['%s.%s-%s.responses']",
                        integrationRuntime.getIntegration().getContext().getName(),
                        camelContextProp.get(CamelPropertyField.id.name()),
                        camelContextProp.get(CamelPropertyField.routeId1.name())))
                        .value(notNullValue()))
        ;
    }

    @Test
    void shouldGetHistoryMetrics() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/historymetrics", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("version").value(MavenUtil.getModelVersion()))
                .andExpect(jsonPath("gauges").isEmpty())
                .andExpect(jsonPath("counters").isEmpty())
                .andExpect(jsonPath("histograms").isEmpty())
                .andExpect(jsonPath("meters").isEmpty())
                .andExpect(jsonPath("timers").isEmpty())
        ;
    }


    private void installFlow(String id, String camelContext) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/1/flow/%s/install", id),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                camelContext
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);
        resultActions.andExpect(status().isOk());
    }

    private Properties buildCamelContextExample() {
        Properties props = new Properties();

        StringBuffer camelContextBuf = new StringBuffer();
        camelContextBuf.append("<camelContext id=\"ID_63ee34e25827222b3d000022\" xmlns=\"http://camel.apache.org/schema/blueprint\" useMDCLogging=\"true\" streamCache=\"true\">");
        camelContextBuf.append("<jmxAgent id=\"agent\" loadStatisticsEnabled=\"true\"/>");
        camelContextBuf.append("<streamCaching id=\"streamCacheConfig\" spoolThreshold=\"0\" spoolDirectory=\"tmp/camelcontext-#camelId#\" spoolUsedHeapMemoryThreshold=\"70\"/>");
        camelContextBuf.append("<threadPoolProfile id=\"wiretapProfile\" defaultProfile=\"false\" poolSize=\"0\" maxPoolSize=\"5\" maxQueueSize=\"2000\" rejectedPolicy=\"DiscardOldest\" keepAliveTime=\"10\"/>");
        camelContextBuf.append("<threadPoolProfile id=\"defaultProfile\" defaultProfile=\"true\" poolSize=\"0\" maxPoolSize=\"10\" maxQueueSize=\"1000\" rejectedPolicy=\"CallerRuns\" keepAliveTime=\"30\"/>");
        camelContextBuf.append("<onException>");
        camelContextBuf.append("<exception>java.lang.Exception</exception>");
        camelContextBuf.append("<redeliveryPolicy maximumRedeliveries=\"0\" redeliveryDelay=\"5000\"/>");
        camelContextBuf.append("<setExchangePattern pattern=\"InOnly\"/>");
        camelContextBuf.append("</onException>");
        camelContextBuf.append("<route id=\"0bc12100-ae01-11ed-8f2a-c39ccdb17c7e\">");
        camelContextBuf.append("<from uri=\"jetty:https://0.0.0.0:9001/1/sdfsadgdsagdsfg?httpBinding=#customHttpBinding&amp;matchOnUriPrefix=false&amp;sslContextParameters=sslContext\"/>");
        camelContextBuf.append("<removeHeaders pattern=\"CamelHttp*\"/>");
        camelContextBuf.append("<to uri=\"direct:ID_63ee34e25827222b3d000022_test_0bc12100-ae01-11ed-8f2a-c39ccdb17c7e?exchangePattern=InOut\"/>");
        camelContextBuf.append("</route>");
        camelContextBuf.append("<route id=\"0e3d92b0-ae01-11ed-8f2a-c39ccdb17c7e\">");
        camelContextBuf.append("<from uri=\"direct:ID_63ee34e25827222b3d000022_test_0bc12100-ae01-11ed-8f2a-c39ccdb17c7e\"/>");
        camelContextBuf.append("<setHeader headerName=\"CamelVelocityTemplate\">");
        camelContextBuf.append("<simple>sdfgsdfgdsfg</simple>");
        camelContextBuf.append("</setHeader>");
        camelContextBuf.append("<to uri=\"velocity:generate\"/>");
        camelContextBuf.append("</route>");
        camelContextBuf.append("</camelContext>");

        props.setProperty(CamelPropertyField.id.name(), "ID_63ee34e25827222b3d000022");
        props.setProperty(CamelPropertyField.routeId1.name(), "0bc12100-ae01-11ed-8f2a-c39ccdb17c7e");
        props.setProperty(CamelPropertyField.routeId2.name(), "0e3d92b0-ae01-11ed-8f2a-c39ccdb17c7e");
        props.setProperty(CamelPropertyField.camelContext.name(), camelContextBuf.toString());

        return props;
    }

}