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
import org.json.JSONArray;

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
    void shouldValidateCertificateError() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/certificate";

            //params
            HashMap<String, String> params = new HashMap<>();
            params.put("httpsUrl", "https://expired.badssl.com/");

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            //call endpoint
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            //asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response.body());

            assertThat(responseJson.get("validationResultStatus").asText()).isEqualTo("INVALID");
            assertThat(responseJson.get("message").asText().toLowerCase()).contains("certification path");

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

    @Test
    void shouldValidateFtpWithSuccess() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/ftp";

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            //body (i use a test rebex - https://ftptest.net/)
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("host", "test.rebex.net");
            bodyJson.put("port", 21);
            bodyJson.put("user", "demo");
            bodyJson.put("pwd", "password");
            bodyJson.put("protocol", "ftp");
            bodyJson.put("explicitTLS", false);

            //endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", bodyJson.toString(), null, headers);

            //asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateExpressionWithSuccess() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/expression";

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("IsPredicate", "false"); // ou "true" se quiseres testar como predicado

            //body (expression array)
            JSONObject expression = new JSONObject();
            expression.put("name", "CheckInvoice");
            expression.put("expression", "1 + 1");
            expression.put("expressionType", "groovy"); // <-- importante!
            expression.put("nextNode", "nextStep");

            JSONArray expressions = new JSONArray();
            expressions.put(expression);

            //call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", expressions.toString(), null, headers);

            //assert
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUrlWithSuccess() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/url";

            //params
            HashMap<String, String> params = new HashMap<>();
            params.put("httpUrl", "https://www.google.com"); // Uma URL válida

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            //call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUrlWithError() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/url";

            //param
            HashMap<String, String> params = new HashMap<>();
            params.put("httpUrl", "http://url-error-test.com");

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            //call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, params, headers);

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get("error").asText()).isEqualTo("Url is not reachable from the server!");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUriWithSuccess() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/uri";

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Uri", "direct:teste"); //exemplo de URI válida no Camel

            //call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            //asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.readTree(response.body());

            assertThat(body.get("message").asText().toLowerCase()).contains("valid");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUriWithError() {
        try {
            //url
            String baseUrl = container.getBaseUrl();
            String url = baseUrl + "/api/validation/uri";

            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Uri", "::::uri-invalida::::");

            //call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            //asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.readTree(response.body());

            assertThat(body.get("message").asText().toLowerCase()).contains("invalid");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }




}
