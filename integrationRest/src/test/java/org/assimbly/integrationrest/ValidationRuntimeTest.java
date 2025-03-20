package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.*;

import java.net.http.HttpResponse;
import java.util.HashMap;

class ValidationRuntimeTest {

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
    void shouldValidateCronWithSuccess() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/cron";

            // params
            HashMap<String, String> params = new HashMap();
            params.put("expression", "0 0/5 * * * ?");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - validate cron expression
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);
            assertThat(response.body()).isEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateCronWithError() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/cron";

            // params
            HashMap<String, String> params = new HashMap();
            params.put("expression", "0 0/5 * * *");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - validate cron expression
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get("error").asText()).isEqualTo("Cron Validation error: Unexpected end of expression.");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }
}
