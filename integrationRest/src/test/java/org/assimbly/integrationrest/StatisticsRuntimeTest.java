package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.utils.CamelContextUtil;
import org.assimbly.integrationrest.utils.FlowUtil;
import org.assimbly.integrationrest.utils.MavenUtil;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.annotation.DirtiesContext;
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
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StatisticsRuntimeTest {

    @Autowired
    private IntegrationRuntime integrationRuntime;

    @Autowired
    private MockMvc mockMvc;

    Properties camelContextProp = CamelContextUtil.buildExample();

    @BeforeEach
    void beforeEach() throws Exception{
        integrationRuntime.getIntegration().getContext().init();
        integrationRuntime.getIntegration().getContext().start();
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );
    }

    @AfterEach
    void afterEach() throws Exception{
        integrationRuntime.getIntegration().getContext().stop();
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
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
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
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("$[0].flow.completed").value(0))
                .andExpect(jsonPath("$[0].flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetStatsByFlowIds() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/statsbyflowids", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,
                        "flowIds", (String)camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("$[0].flow.completed").value(0))
                .andExpect(jsonPath("$[0].flow.failed").value(0))
                .andExpect(jsonPath("$[0].flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("$[0].flow.status").value("Started"))
        ;
    }

    @Test
    void shouldGetStatsByFlowId() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.getProperty(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.getProperty(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetStatsByFlowIdWithFullStats() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
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
                String.format("/api/integration/%d/flow/%s/stats", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
                .andExpect(jsonPath("flow.timeout").value(greaterThan(0)))
                .andExpect(jsonPath("flow.status").value("Started"))
                .andExpect(jsonPath("flow.steps").value(notNullValue()))
                .andExpect(jsonPath("flow.steps[*].step.id").value(hasItem(String.format("%s-%s",
                        camelContextProp.get(CamelContextUtil.Field.id.name()),
                        camelContextProp.get(CamelContextUtil.Field.routeId1.name())))))
        ;
    }

    @Test
    void shouldGetFlowStepStats() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/step/%s/stats", 1,
                        camelContextProp.get(CamelContextUtil.Field.id.name()),
                        camelContextProp.get(CamelContextUtil.Field.routeId1.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE,"FullStats",
                        "true","includeSteps","true"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("step.id").value(String.format("%s-%s",
                        camelContextProp.get(CamelContextUtil.Field.id.name()),
                        camelContextProp.get(CamelContextUtil.Field.routeId1.name()))))
                .andExpect(jsonPath("step.status").value("started"))
        ;
    }

    @Test
    void shouldGetFlowMessages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.total").value(0))
                .andExpect(jsonPath("flow.pending").value(0))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.completed").value(0))
                .andExpect(jsonPath("flow.failed").value(0))
        ;
    }

    @Test
    void shouldGetFlowTotalMessages() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/messages/total", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                String.format("/api/integration/%d/flow/%s/messages/completed", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                String.format("/api/integration/%d/flow/%s/messages/failed", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                String.format("/api/integration/%d/flow/%s/messages/pending", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
                        camelContextProp.get(CamelContextUtil.Field.id.name()), camelContextProp.get(CamelContextUtil.Field.routeId1.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("step.total").value(0))
                .andExpect(jsonPath("step.pending").value(0))
                .andExpect(jsonPath("step.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
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
                        camelContextProp.get(CamelContextUtil.Field.id.name()),
                        camelContextProp.get(CamelContextUtil.Field.routeId1.name())))
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

}