package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
import org.assimbly.integrationrest.utils.CamelContextUtil;
import org.assimbly.integrationrest.utils.FlowUtil;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;
import java.util.Properties;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FlowManagerRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationRuntime.class,
        IntegrationConfig.class,
        FlowManagerRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FlowManagerRuntimeTest {

    @Autowired
    private IntegrationRuntime integrationRuntime;

    @Autowired
    private MockMvc mockMvc;

    private Properties camelContextProp = CamelContextUtil.buildExample();

    @BeforeEach
    void beforeEach() throws Exception{
        integrationRuntime.getIntegration().getContext().init();
        integrationRuntime.getIntegration().getContext().start();
    }

    @AfterEach
    void afterEach() throws Exception{
        integrationRuntime.getIntegration().getContext().stop();
    }

    @Test
    void shouldStartFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/start", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;
    }

    @Test
    void shouldStopFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stop", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("stop"))
                .andExpect(jsonPath("flow.message").value("Stopped flow successfully"))
        ;
    }

    @Test
    void shouldRestartFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/restart", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;
    }

    @Test
    void shouldPauseFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/pause", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("pause"))
                .andExpect(jsonPath("flow.message").value("Paused flow successfully"))
        ;
    }

    @Test
    void shouldResumeFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/resume", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("resume"))
                .andExpect(jsonPath("flow.message").value("Resumed flow successfully"))
        ;
    }

    @Test
    void shouldFlowRoutes() throws Exception {
        // TODO
    }

    @Test
    void shouldInstallFlow() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/install", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;

    }

    @Test
    void shouldUninstallFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uninstall", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.event").value("stop"))
                .andExpect(jsonPath("flow.message").value("Stopped flow successfully"))
        ;
    }

    @Test
    void shouldFileInstallFlow() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/install/file", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value(
                        String.format("flow %s saved in the deploy directory", camelContextProp.get(CamelContextUtil.Field.id.name())))
                )
        ;
    }

    @Test
    void shouldFileUninstallFlow() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uninstall/file", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value(
                        String.format("flow %s deleted from deploy directory", camelContextProp.get(CamelContextUtil.Field.id.name())))
                )
        ;
    }

    @Test
    void shouldBeFlowStarted() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/isstarted", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("true"))
        ;
    }

    @Test
    void shouldGetFlowInfo() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/info", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.isRunning").value(true))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("flow.status").value("started"))
        ;
    }

    @Test
    void shouldGetFlowStatus() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/status", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("started"))
        ;
    }

    @Test
    void shouldGetFlowUptime() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uptime", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("0s"))
        ;
    }

    @Test
    void shouldGetFlowLastError() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/lasterror", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("0"))
        ;
    }

    @Test
    void shouldGetFlowAlertsLog() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/alerts", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("0"))
        ;
    }

    @Test
    void shouldGetFlowNumberOfAlerts() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/alerts/count", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("0"))
        ;
    }

    @Test
    void shouldGetFlowEvents() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/events", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("0"))
        ;
    }

    @Test
    void shouldSetMaintenance() throws Exception {
        // TODO
    }

}