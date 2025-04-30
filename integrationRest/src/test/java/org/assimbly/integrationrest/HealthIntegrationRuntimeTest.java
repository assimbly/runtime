package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class HealthIntegrationRuntimeTest {

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

    @Test
    void shouldGetBackendHealthInfo() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/health/backend/jvm"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode jvmJson = responseJson.get("jvm");
            JsonNode memoryJson = responseJson.get("memory");
            JsonNode threadsJson = responseJson.get("threads");

            // asserts contents
            assertThat(jvmJson.get("openFileDescriptors").asInt()).isPositive();
            assertThat(jvmJson.get("maxFileDescriptors").asInt()).isPositive();
            assertThat(memoryJson.get("current").asInt()).isPositive();
            assertThat(memoryJson.get("committed").asInt()).isPositive();
            assertThat(memoryJson.get("max").asInt()).isPositive();
            assertThat(memoryJson.get("cached").asInt()).isPositive();
            assertThat(memoryJson.get("currentUsedPercentage").asInt()).isBetween(0, 100);
            assertThat(threadsJson.get("threadCount").asInt()).isPositive();
            assertThat(threadsJson.get("peakThreadCount").asInt()).isPositive();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetBackendFlowsInfo() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/health/backend/flows"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("uptimeMillis").asInt()).isPositive();
            assertThat(responseJson.get("startedSteps").asInt()).isNotNegative();
            assertThat(responseJson.get("memoryUsage").asDouble()).isPositive();
            assertThat(responseJson.get("exchangesInflight").asInt()).isNotNegative();
            assertThat(responseJson.get("camelVersion").asText()).isNotEmpty();
            assertThat(responseJson.get("exchangesCompleted").asInt()).isNotNegative();
            assertThat(responseJson.get("camelId").asText()).isNotEmpty();
            assertThat(responseJson.get("uptime").asText()).isNotEmpty();
            assertThat(responseJson.get("startedFlows").asText()).isNotEmpty();
            assertThat(responseJson.get("totalThreads").asInt()).isPositive();
            assertThat(responseJson.get("cpuLoadLastMinute").asText()).isNotEmpty();
            assertThat(responseJson.get("cpuLoadLast15Minutes").asText()).isNotEmpty();
            assertThat(responseJson.get("exchangesTotal").asInt()).isNotNegative();
            assertThat(responseJson.get("exchangesFailed").asInt()).isNotNegative();
            assertThat(responseJson.get("cpuLoadLast5Minutes").asText()).isNotEmpty();
            assertThat(responseJson.get("status").asText()).isEqualTo("Started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
