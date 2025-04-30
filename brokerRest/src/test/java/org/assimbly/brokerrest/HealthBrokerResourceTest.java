package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class HealthBrokerResourceTest {

    private static AssimblyGatewayBrokerContainer container;

    @BeforeAll
    static void setUp() {
        container = new AssimblyGatewayBrokerContainer();
        container.init();
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    @Test
    void shouldGetEngineDataInfo() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = baseUrl + "/health/broker/engine";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/health/broker/engine"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("totalNumberOfQueues").isInt()).isTrue();
            assertThat(responseJson.get("openConnections").isInt()).isTrue();
            assertThat(responseJson.get("averageMessageSize").isInt()).isTrue();
            assertThat(responseJson.get("storePercentUsage").isInt()).isTrue();
            assertThat(responseJson.get("memoryPercentUsage").isInt()).isTrue();
            assertThat(responseJson.get("totalNumberOfTemporaryQueues").isInt()).isTrue();
            assertThat(responseJson.get("maxConnections").isInt()).isTrue();
            assertThat(responseJson.get("tmpPercentUsage").isInt()).isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetJvmBrokerInfo() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = baseUrl + "/health/broker/jvm";

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/health/broker/jvm"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode jvmJson = responseJson.get("jvm");
            JsonNode memoryJson = responseJson.get("memory");
            JsonNode threadsJson = responseJson.get("threads");

            // asserts contents
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
