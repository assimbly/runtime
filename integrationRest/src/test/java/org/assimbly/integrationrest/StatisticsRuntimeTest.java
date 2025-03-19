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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class StatisticsRuntimeTest {

    private Properties inboundHttpsCamelContextProp = TestApplicationContext.buildInboundHttpsExample();
    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

    private static boolean schedulerFlowInstalled = false;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled") && !schedulerFlowInstalled) {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.camelContext.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    void shouldGetEmptyFlowStats() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/stats", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.body()).isEqualTo("{\"flow\":{\"total\":0,\"pending\":0,\"id\":\"67921474ecaafe0007000000\",\"completed\":0,\"failed\":0}}");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetStats() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/stats", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("uptimeMillis").asInt()).isGreaterThan(0);
            assertThat(responseJson.get("startedSteps").isInt()).isTrue();
            assertThat(responseJson.get("memoryUsage").isDouble()).isTrue();
            assertThat(responseJson.get("exchangesInflight").isInt()).isTrue();
            assertThat(responseJson.get("camelVersion")).isNotNull();
            assertThat(responseJson.get("exchangesCompleted").isInt()).isTrue();
            assertThat(responseJson.get("camelId")).isNotNull();
            assertThat(responseJson.get("uptime")).isNotNull();
            assertThat(responseJson.get("startedFlows")).isNotNull();
            assertThat(responseJson.get("totalThreads").isInt()).isTrue();
            assertThat(responseJson.get("cpuLoadLastMinute")).isNotNull();
            assertThat(responseJson.get("cpuLoadLast15Minutes")).isNotNull();
            assertThat(responseJson.get("exchangesTotal").isInt()).isTrue();
            assertThat(responseJson.get("exchangesFailed").isInt()).isTrue();
            assertThat(responseJson.get("cpuLoadLast5Minutes")).isNotNull();
            assertThat(responseJson.get("status").asText()).isEqualTo("Started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStatsFlows() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/stats/flows", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get stats flows
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.size()).isGreaterThanOrEqualTo(0);

            JsonNode flowJson = responseJson.get(0).get("flow");

            assertThat(flowJson.get("uptimeMillis").asInt()).isGreaterThan(0);
            assertThat(flowJson.get("pending").isInt()).isTrue();
            assertThat(flowJson.get("completed").isInt()).isTrue();
            assertThat(flowJson.get("failed").isInt()).isTrue();
            assertThat(flowJson.get("lastFailed")).isNotNull();
            assertThat(flowJson.get("timeout").isInt()).isTrue();
            assertThat(flowJson.get("uptime")).isNotNull();
            assertThat(flowJson.get("total").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLastMinute").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLast15Minutes").isInt()).isTrue();
            assertThat(flowJson.get("lastCompleted")).isNotNull();
            assertThat(flowJson.get("cpuLoadLast5Minutes").isInt()).isTrue();
            assertThat(flowJson.get("id")).isNotNull();
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStatsByFlowIds() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/statsbyflowids", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name());

            // endpoint call - get stats by flows ids
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            assertThat(flowJson.get("uptimeMillis").asInt()).isGreaterThan(0);
            assertThat(flowJson.get("pending").isInt()).isTrue();
            assertThat(flowJson.get("completed").isInt()).isTrue();
            assertThat(flowJson.get("failed").isInt()).isTrue();
            assertThat(flowJson.get("lastFailed")).isNotNull();
            assertThat(flowJson.get("timeout").isInt()).isTrue();
            assertThat(flowJson.get("uptime")).isNotNull();
            assertThat(flowJson.get("total").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLastMinute").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLast15Minutes").isInt()).isTrue();
            assertThat(flowJson.get("lastCompleted")).isNotNull();
            assertThat(flowJson.get("cpuLoadLast5Minutes").isInt()).isTrue();
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
