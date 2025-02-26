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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = FlowConfigurerRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationRuntime.class,
        IntegrationConfig.class,
        FlowConfigurerRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FlowConfigurerRuntimeTest {

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
    void shouldSetFlowConfiguration() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/configure", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_XML_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null,
                MediaType.APPLICATION_XML_VALUE,
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.APPLICATION_XML_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(xpath("//response/details").string("successful"))
                .andExpect(xpath("//response/message").string("Flow configuration set"))
        ;
    }

    @Test
    void shouldGetFlowConfiguration() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/configure", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_XML_VALUE, "charset", StandardCharsets.ISO_8859_1.displayName()),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.CONTENT_TYPE,
                        String.format("%s;charset=%s", MediaType.APPLICATION_XML_VALUE, StandardCharsets.ISO_8859_1.displayName())))
                .andExpect(xpath("//camelContext/@id").string((String)camelContextProp.get(CamelContextUtil.Field.id.name())))
        ;
    }

    @Test
    void shouldCheckFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/isconfigured", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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
    void shouldGetDocumentationVersion() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/documentation/version", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("details").value("successful"))
                .andExpect(jsonPath("message").isNotEmpty())
        ;
    }

    @Test
    void shouldGetDocumentation() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/documentation/%s", 1, "velocity"),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("component").isNotEmpty())
                .andExpect(jsonPath("component.kind").value("component"))
                .andExpect(jsonPath("component.name").value("velocity"))
                .andExpect(jsonPath("component.label").value("transformation"))
                .andExpect(jsonPath("componentProperties").isNotEmpty())
                .andExpect(jsonPath("headers").isNotEmpty())
                .andExpect(jsonPath("properties").isNotEmpty())
        ;
    }

    @Test
    void shouldGetComponents() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/components", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE, "includeCustomComponents", "false"),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[?(@.name == 'velocity')].kind").value("component"))
                .andExpect(jsonPath("$[?(@.name == 'velocity')].label").value("transformation"))
        ;

    }

    @Test
    void shouldGetCustomComponents() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/components", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE, "includeCustomComponents", "true"),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$[?(@.name == 'xmltojson')].kind").value("component"))
                .andExpect(jsonPath("$[?(@.name == 'xmltojson')].firstVersion").value("3.18.0"))
        ;
    }

    @Test
    void shouldGetComponentSchema() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/schema/%s", 1, "velocity"),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("component").isNotEmpty())
                .andExpect(jsonPath("component.kind").value("component"))
                .andExpect(jsonPath("component.name").value("velocity"))
                .andExpect(jsonPath("component.label").value("transformation"))
                .andExpect(jsonPath("componentProperties").isNotEmpty())
                .andExpect(jsonPath("headers").isNotEmpty())
                .andExpect(jsonPath("properties").isNotEmpty())
        ;
    }

    @Test
    void shouldGetComponentOptions() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/options/%s", 1, "velocity"),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("component").isNotEmpty())
                .andExpect(jsonPath("component.kind").value("component"))
                .andExpect(jsonPath("component.name").value("velocity"))
                .andExpect(jsonPath("component.label").value("transformation"))
                .andExpect(jsonPath("componentProperties").isNotEmpty())
                .andExpect(jsonPath("headers").isNotEmpty())
                .andExpect(jsonPath("properties").isNotEmpty())
        ;
    }

    @Test
    void shouldGetCamelRoute() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/route", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("routes").isNotEmpty())
                .andExpect(jsonPath("routes.route[*].id", hasItem(
                        String.format("%s-%s",
                                camelContextProp.get(CamelContextUtil.Field.id.name()),
                                camelContextProp.get(CamelContextUtil.Field.routeId1.name())))))
        ;
    }

    @Test
    void shouldGetAllCamelRoutes() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/routes", 1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("routes").isNotEmpty())
                .andExpect(jsonPath("routes.route[*].id", hasItem(
                        String.format("%s-%s",
                                camelContextProp.get(CamelContextUtil.Field.id.name()),
                                camelContextProp.get(CamelContextUtil.Field.routeId1.name())))))
        ;
    }

    @Test
    void shouldRemoveFlow() throws Exception {
        FlowUtil.installFlow(
                this.mockMvc,
                (String)camelContextProp.get(CamelContextUtil.Field.id.name()),
                (String)camelContextProp.get(CamelContextUtil.Field.camelContext.name())
        );

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/remove", 1, camelContextProp.get(CamelContextUtil.Field.id.name())),
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


}