package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
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
import java.util.UUID;

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

    public enum CamelPropertyField {
        id,
        routeId1,
        routeId2,
        camelContext
    }

    private Properties camelContextProp = buildCamelContextExample();

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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/start", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;
    }

    @Test
    void shouldStopFlow() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/stop", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("stop"))
                .andExpect(jsonPath("flow.message").value("Stopped flow successfully"))
        ;
    }

    @Test
    void shouldRestartFlow() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/restart", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;
    }

    @Test
    void shouldPauseFlow() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/pause", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("pause"))
                .andExpect(jsonPath("flow.message").value("Paused flow successfully"))
        ;
    }

    @Test
    void shouldResumeFlow() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/resume", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
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
                String.format("/api/integration/%d/flow/%s/install", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("start"))
                .andExpect(jsonPath("flow.message").value("Started flow successfully"))
        ;

    }

    @Test
    void shouldUninstallFlow() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uninstall", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.event").value("stop"))
                .andExpect(jsonPath("flow.message").value("Stopped flow successfully"))
        ;
    }

    @Test
    void shouldFileInstallFlow() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/install/file", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value(
                        String.format("flow %s saved in the deploy directory", camelContextProp.get(CamelPropertyField.id.name())))
                )
        ;
    }

    @Test
    void shouldFileUninstallFlow() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uninstall/file", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value(
                        String.format("flow %s deleted from deploy directory", camelContextProp.get(CamelPropertyField.id.name())))
                )
        ;
    }

    @Test
    void shouldBeFlowStarted() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/isstarted", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/info", 1, camelContextProp.get(CamelPropertyField.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("flow.isRunning").value(true))
                .andExpect(jsonPath("flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("flow.status").value("started"))
        ;
    }

    @Test
    void shouldGetFlowStatus() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/status", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uptime", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/lasterror", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/alerts", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/alerts/count", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/events", 1, camelContextProp.get(CamelPropertyField.id.name())),
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

    private void stopFlow(String id, String camelContext) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/1/flow/%s/stop", id),
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
        UUID randomContextPath = UUID.randomUUID();

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
        camelContextBuf.append("<from uri=\"jetty:https://0.0.0.0:9001/1/"+randomContextPath.toString()+"?matchOnUriPrefix=false\"/>");
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