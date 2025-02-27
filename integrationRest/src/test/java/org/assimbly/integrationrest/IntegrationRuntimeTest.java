package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.utils.CamelContextUtil;
import org.assimbly.integrationrest.utils.FlowUtil;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.json.JSONArray;
import org.json.JSONObject;
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
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IntegrationRuntimeTest {

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
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
                .andExpect(jsonPath("$[0].id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
        ;
    }

    @Test
    void shouldGetRunningFlowsDetails() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
                .andExpect(jsonPath("$[0].flow.name").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("$[0].flow.id").value(camelContextProp.get(CamelContextUtil.Field.id.name())))
                .andExpect(jsonPath("$[0].flow.status").value("started"))
        ;
    }

    @Test
    void shouldGetListOfSoapActions() throws Exception {
        // discarded test - needs access to SoapActionsService from custom-components
    }

    @Test
    void shouldCountFlows() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
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
                .andExpect(content().string(String.format("{%s=%d}", camelContextProp.get(CamelContextUtil.Field.id.name()), 0)))
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
                bodyJsonObject.toString()
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

}