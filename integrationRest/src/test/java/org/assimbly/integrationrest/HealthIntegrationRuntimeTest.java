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

}
