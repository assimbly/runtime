package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.brokerrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BrokerManagerRuntimeTest {

    private static final String BROKER_TYPE = "classic";
    private static final String BROKER_CONFIGURATION_TYPE = "file";

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
    @Order(1)
    void shouldGetBrokerConnections() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/connections", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("connections")).isNotNull();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(1)
    void shouldGetBrokerConsumers() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/consumers", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("n/a");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(1)
    void shouldGetBrokerInfo() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/info", baseUrl, 1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            assertThat(response.body()).isNotNull();

            Map<String, String> outputMap = Arrays.stream(response.body().split(","))
                    .map(s -> s.split("=", 2))
                    .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.valueToTree(outputMap);

            assertThat(responseJson.get("uptime").asText().trim()).matches("\\d+(\\.\\d+)? seconds");
            assertThat(responseJson.get("totalConnections").asInt()).isZero();
            assertThat(responseJson.get("currentConnections").asInt()).isZero();
            assertThat(responseJson.get("totalConsumers").asInt()).isZero();
            assertThat(responseJson.get("totalMessages").asInt()).isZero();
            assertThat(responseJson.get("nodeId").asText()).isNotNull();
            assertThat(responseJson.get("state").asBoolean()).isTrue();
            assertThat(responseJson.get("version").asText()).isNotNull();
            assertThat(responseJson.get("type").asText()).isEqualTo("ActiveMQ Classic");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(1)
    void shouldGetBrokerStatus() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/status", baseUrl, 1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldStopBroker() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/stop", baseUrl, 1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("stopped");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldStartBroker() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/start", baseUrl, 1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", BROKER_TYPE);
            params.put("brokerConfigurationType", BROKER_CONFIGURATION_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void shouldRestartBroker() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/restart", baseUrl, 1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("brokerType", BROKER_TYPE);
            params.put("brokerConfigurationType", BROKER_CONFIGURATION_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
