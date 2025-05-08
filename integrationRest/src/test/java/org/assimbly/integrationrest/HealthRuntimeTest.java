package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HealthRuntimeTest {

    private final Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

    private static boolean schedulerFlowInstalled = false;

    private static AssimblyGatewayHeadlessContainer container;

    @BeforeAll
    static void setUp() {
        container = new AssimblyGatewayHeadlessContainer();
        container.init();
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled") && !schedulerFlowInstalled) {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/install"), (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowHealthById() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/health"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulHealthResponse(flowJson, (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowStepHealthById() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/step/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())+"/health"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode stepJson = responseJson.get("step");

            // asserts contents
            AssertUtils.assertSuccessfulHealthResponse(stepJson, String.format("%s-%s", schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetHealth() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/health"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulHealthResponse(flowJson, (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetHealthByFlowIds() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/healthbyflowids"), (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulHealthResponse(flowJson, (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
