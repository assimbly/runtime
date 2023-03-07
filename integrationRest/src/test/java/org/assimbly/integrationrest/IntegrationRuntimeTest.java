package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.json.JSONArray;
import org.json.simple.JSONObject;
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

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = IntegrationRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationRuntime.class,
        IntegrationConfig.class,
        FlowManagerRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationRuntimeTest {

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
    void shouldStart() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/start", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("Integration started"))
        ;
    }

    @Test
    void shouldInfo() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/info", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("info.numberOfRunningSteps").value(2))
                .andExpect(jsonPath("info.startupType").value("Default"))
                .andExpect(jsonPath("info.uptimeMiliseconds").value(greaterThan(0)))
                .andExpect(jsonPath("info.name").value(integrationRuntime.getIntegration().getContext().getName()))
        ;
    }

    @Test
    void shouldBeStarted() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/isStarted", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("message").value("true"))
        ;
    }

    @Test
    void shouldGetLastError() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/lasterror", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(result -> notNullValue())
        ;
    }

    @Test
        void shouldResolveDependencyByScheme() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/resolvedependencybyscheme/%s", 1, "component"),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.TEXT_PLAIN_VALUE,
                ""
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
        ;
    }

    @Test
    void shouldGetBaseDirectory() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/basedirectory", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().string(notNullValue()))
        ;
    }

    @Test
    void shouldSetBaseDirectory() throws Exception {
        // do not call/change the base set directory in the integration tests
    }

    @Test
    void shouldGetListOfFlows() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/list/flows", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].id").value(camelContextProp.get(CamelPropertyField.id.name())))
        ;
    }

    @Test
    void shouldGetRunningFlowsDetails() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/list/flows/details", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[0].flow.isRunning").value(true))
                .andExpect(jsonPath("$[0].flow.name").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelPropertyField.id.name())))
                .andExpect(jsonPath("$[0].flow.status").value("started"))
        ;
    }

    @Test
    void shouldGetListOfSoapActions() throws Exception {
        // discarded test - needs access to SoapActionsService from custom-components
    }

    @Test
    void shouldCountFlows() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/count/flows", 1),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("1"))
        ;
    }

    @Test
    void shouldCountSteps() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/count/steps", 1),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string("2"))
        ;
    }

    @Test
    void shouldGetIntegrationNumberOfAlerts() throws Exception {

        installFlow(
                (String)camelContextProp.get(CamelPropertyField.id.name()),
                (String)camelContextProp.get(CamelPropertyField.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/numberofalerts", 1),
                Map.of("Accept", MediaType.TEXT_PLAIN_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.TEXT_PLAIN_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(content().string(String.format("{%s=%d}", camelContextProp.get(CamelPropertyField.id.name()), 0)))
        ;
    }

    @Test
    void shouldAddCollectorConfiguration() throws Exception {

        // build body in json format
        JSONArray eventsJsonObject = new JSONArray()
                .put("RouteReloaded").put("RouteStarted").put("RouteStarting").put("RouteStopped").put("RouteStopping");
        JSONArray storesJsonObject = new JSONArray().put(new JSONObject(Map.of("type", "console")));
        JSONObject bodyJsonObject = new JSONObject(Map.of(
                "id", "3",
                "type", "step",
                "events", eventsJsonObject,
                "stores", storesJsonObject
        ));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/collector/%d/add", 1, 3),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("configured"))
        ;
    }

    @Test
    void shouldRemoveCollectorConfiguration() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/collector/%d/remove", 1, 3),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").value("removed"))
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
        camelContextBuf.append("<from uri=\"jetty:https://0.0.0.0:9001/1/sdfsadgdsagdsfg?matchOnUriPrefix=false\"/>");
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