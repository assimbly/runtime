package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.Utils;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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

            // endpoint call
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

            // endpoint call
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

    @Test
    void shouldValidateCertificate() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/certificate";

            // params
            HashMap<String, String> params = new HashMap();
            params.put("httpsUrl", "https://authenticationtest.com/HTTPAuth/");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get("validationResultStatus").asText()).isEqualTo("VALID");
            assertThat(responseJson.get("message").asText()).isEqualTo("null");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateConnection() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/validation/connection/%s/%d/%d", baseUrl, "google.com", 443, 5000);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("Connection successful");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateXsltWithSuccess() {
        try {
            // URL
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/xslt";

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("StopTest", "false");
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("xsltUrl", "https://www.w3schools.com/xml/cdcatalog_client.xsl");

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", bodyJson.toString(), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateScriptWithSuccess() {
        try {
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/script";

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // script
            JSONObject scriptJson = new JSONObject();
            scriptJson.put("language", "groovy");
            scriptJson.put("script", "return 1 + 1;");

            // Exchange
            JSONObject exchangeJson = new JSONObject();
            exchangeJson.put("body", "");

            // headers
            JSONObject headersJson = new JSONObject();
            exchangeJson.put("headers", headersJson);

            //properties
            JSONObject propertiesJson = new JSONObject();
            exchangeJson.put("properties", propertiesJson);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("script", scriptJson);
            bodyJson.put("exchange", exchangeJson);

            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", bodyJson.toString(), null, headers);

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("code").asInt()).isEqualTo(1);
            assertThat(responseJson.get("result").asText()).isEqualTo("2");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateRegexWithSuccess() {
        try {
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/regex";

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("expression", "^[a-zA-Z0-9]+$");

            //send to API
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", bodyJson.toString(), null, headers);

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            //read and replied
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get("details").asText()).isEqualTo("successful");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }



}
