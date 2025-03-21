package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
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

    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

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
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowHealthById() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/health", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("state").asText()).isEqualTo("UP");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowStepHealthById() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/step/%s/health", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode stepJson = responseJson.get("step");

            assertThat(stepJson.get("id").asText()).isEqualTo(String.format("%s-%s", schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())));
            assertThat(stepJson.get("state").asText()).isEqualTo("UP");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetHealth() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/health", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isPositive();

            JsonNode flowJson = responseJson.get(0).get("flow");
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("state").asText()).isEqualTo("UP");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetHealthByFlowIds() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/healthbyflowids", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isPositive();

            JsonNode flowJson = responseJson.get(0).get("flow");
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("state").asText()).isEqualTo("UP");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
