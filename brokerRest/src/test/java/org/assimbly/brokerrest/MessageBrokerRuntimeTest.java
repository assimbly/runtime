package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.brokerrest.utils.HttpUtil;
import org.assimbly.brokerrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class MessageBrokerRuntimeTest {

    private Properties schedulerHttpRetryCamelContextProp = TestApplicationContext.buildSchedulerEnrichExample();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerHttpRetryFlowInstalled")) {
            // url
            String baseUrl = AssimblyGatewayBrokerContainer.getHeadlessBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerHttpRetryCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerHttpRetryCamelContextProp.get(TestApplicationContext.CamelContextField.camelContext.name()), null, headers);
        }
    }

    @Test
    void shouldGetMessagesCount() {
        try {
            // url
            String baseUrl = AssimblyGatewayBrokerContainer.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/count", baseUrl, "classic");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = "67c740bc349ced00070004a9,67c740bc349ced00070004a9_BottomCenter";

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asInt()).isEqualTo(0);
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Disabled
    @Test
    @Tag("NeedsSchedulerHttpRetryFlowInstalled")
    void shouldGetDelayedMessagesCount() {
        try {
            // url
            String baseUrl = AssimblyGatewayBrokerContainer.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/delayedmessages/%s/count", baseUrl, "classic", schedulerHttpRetryCamelContextProp.get(TestApplicationContext.CamelContextField.httpRetryQueue.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // waiting
            AssimblyGatewayBrokerContainer.waitFor(".*Timeout exception: org.apache.hc.client5.http.ConnectTimeoutException.*", 7);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asInt()).isEqualTo(0);
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
