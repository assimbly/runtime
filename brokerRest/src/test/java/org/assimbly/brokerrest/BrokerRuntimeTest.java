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

}
