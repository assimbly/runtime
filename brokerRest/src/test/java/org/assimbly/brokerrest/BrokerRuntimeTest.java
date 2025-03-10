package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.brokerrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class BrokerRuntimeTest {

    @Test
    void shouldGetEngineDataInfo() {
        try {
            // url
            String baseUrl = AssimblyGatewayBrokerContainer.getBaseUrl();
            String url = baseUrl + "/health/broker/engine";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

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
    void shouldGetMessagesCount() {
        try {
            // url
            String baseUrl = AssimblyGatewayBrokerContainer.getBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/count", baseUrl, "classic");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = "67921474ecaafe0007000000,67921474ecaafe0007000000_BottomCenter";

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

}
