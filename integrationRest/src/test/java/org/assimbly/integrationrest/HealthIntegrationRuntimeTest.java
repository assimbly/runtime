package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HealthIntegrationRuntimeTest {

    @Test
    void shouldGetBackendHealthInfo() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/health/backend/jvm", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode jvmJson = responseJson.get("jvm");
            JsonNode memoryJson = responseJson.get("memory");
            JsonNode threadsJson = responseJson.get("threads");

            assertThat(jvmJson.get("openFileDescriptors").asInt()).isGreaterThan(0);
            assertThat(jvmJson.get("maxFileDescriptors").asInt()).isGreaterThan(0);
            assertThat(memoryJson.get("current").asInt()).isGreaterThan(0);
            assertThat(memoryJson.get("committed").asInt()).isGreaterThan(0);
            assertThat(memoryJson.get("max").asInt()).isGreaterThan(0);
            assertThat(memoryJson.get("cached").asInt()).isGreaterThan(0);
            assertThat(memoryJson.get("currentUsedPercentage").asInt()).isBetween(0, 100);
            assertThat(threadsJson.get("threadCount").asInt()).isGreaterThan(0);
            assertThat(threadsJson.get("peakThreadCount").asInt()).isGreaterThan(0);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetBackendFlowsInfo() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/health/backend/flows", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("uptimeMillis").asInt()).isGreaterThan(0);
            assertThat(responseJson.get("startedSteps").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(responseJson.get("memoryUsage").asDouble()).isGreaterThan(0);
            assertThat(responseJson.get("exchangesInflight").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(responseJson.get("camelVersion").asText()).isNotEmpty();
            assertThat(responseJson.get("exchangesCompleted").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(responseJson.get("camelId").asText()).isNotEmpty();
            assertThat(responseJson.get("uptime").asText()).isNotEmpty();
            assertThat(responseJson.get("startedFlows").asText()).isNotEmpty();
            assertThat(responseJson.get("totalThreads").asInt()).isGreaterThan(0);
            assertThat(responseJson.get("cpuLoadLastMinute").asText()).isNotEmpty();
            assertThat(responseJson.get("cpuLoadLast15Minutes").asText()).isNotEmpty();
            assertThat(responseJson.get("exchangesTotal").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(responseJson.get("exchangesFailed").asInt()).isGreaterThanOrEqualTo(0);
            assertThat(responseJson.get("cpuLoadLast5Minutes").asText()).isNotEmpty();
            assertThat(responseJson.get("status").asText()).isEqualTo("Started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
