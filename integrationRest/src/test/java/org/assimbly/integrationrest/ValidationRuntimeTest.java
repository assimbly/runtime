package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.Utils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
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
            // params
            HashMap<String, String> params = new HashMap();
            params.put("expression", "0 0/5 * * * ?");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/cron"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

            // asserts contents
            assertThat(response.body()).isEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateCronWithError() {
        try {
            // params
            HashMap<String, String> params = new HashMap();
            params.put("expression", "0 0/5 * * *");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/cron"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("error").asText()).isEqualTo("Cron Validation error: Unexpected end of expression.");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateCertificate() {
        try {
            // params
            HashMap<String, String> params = new HashMap();
            params.put("httpsUrl", "https://authenticationtest.com/HTTPAuth/");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/certificate"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertCertificateResponseWithoutMessage(responseJson, "VALID");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateCertificateError() {
        try {
            // params
            HashMap<String, String> params = new HashMap<>();
            params.put("httpsUrl", "https://expired.badssl.com/");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // call endpoint
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/certificate"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertCertificateResponse(responseJson, "INVALID", "certification path");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateConnection() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/connection/google.com/443/5000"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "Connection successful");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateConnectionError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/connection/192.0.2.1/666/2000"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertConnectionResponse(responseJson, "successful", "Connection error: IOException");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }


    @Test
    void shouldValidateXsltWithSuccess() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("StopTest", "false");
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("xsltUrl", "https://www.w3schools.com/xml/cdcatalog_client.xsl");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/xslt"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateXsltError() {
        try {
            // body
            JSONObject body = new JSONObject();
            body.put("xsltUrl", "http://url-invalid-nonexistent.com/fake.xsl");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/xslt"), body.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJson = mapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertXsltErrorResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }


    @Test
    void shouldValidateScriptWithSuccess() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // script
            JSONObject scriptJson = new JSONObject();
            scriptJson.put("language", "groovy");
            scriptJson.put("script", "return 1 + 1;");

            // exchange
            JSONObject exchangeJson = new JSONObject();
            exchangeJson.put("body", "");

            // headers
            JSONObject headersJson = new JSONObject();
            exchangeJson.put("headers", headersJson);

            // properties
            JSONObject propertiesJson = new JSONObject();
            exchangeJson.put("properties", propertiesJson);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("script", scriptJson);
            bodyJson.put("exchange", exchangeJson);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/script"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertScriptResponse(responseJson, 1, "2");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateScriptWithError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // invalid script
            JSONObject scriptJson = new JSONObject();
            scriptJson.put("language", "groovy");
            scriptJson.put("script", "return 1 + ;");

            // exchange
            JSONObject exchangeJson = new JSONObject();
            exchangeJson.put("body", "");

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("script", scriptJson);
            bodyJson.put("exchange", exchangeJson);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/script"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            String msg = responseJson.get("message").asText().toLowerCase();

            // asserts contents
            AssertUtils.assertScriptErrorResponse(msg);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateRegexWithSuccess() {
        try {
            //headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("expression", "^[a-zA-Z0-9]+$");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/regex"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "0");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateRegexWithError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("expression", "(a-z");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/regex"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST_400);

            String actual = response.body();
            String expected = "Unclosed group near index 4\n" +
                    "(a-z";

            // asserts contents
            assertThat(actual).isEqualTo(expected);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateFtpWithSuccess() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("host", "test.rebex.net");
            bodyJson.put("port", 21);
            bodyJson.put("user", "demo");
            bodyJson.put("pwd", "password");
            bodyJson.put("protocol", "ftp");
            bodyJson.put("explicitTLS", false);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/ftp"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateFtpWithError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("host", "invalid.ftp.test.server"); // invalid host
            bodyJson.put("port", 9999); // invalid port
            bodyJson.put("user", "error"); // invalid user
            bodyJson.put("pwd", "testerror"); // invalid password
            bodyJson.put("protocol", "ftp");
            bodyJson.put("explicitTLS", false);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/ftp"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("error").asText().toLowerCase()).contains("host name could not be resolved");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateExpressionWithSuccess() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("IsPredicate", "false");

            // body
            JSONObject expression = new JSONObject();
            expression.put("name", "CheckInvoice");
            expression.put("expression", "1 + 1");
            expression.put("expressionType", "groovy");
            expression.put("nextNode", "nextStep");

            JSONArray expressions = new JSONArray();
            expressions.put(expression);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/expression"), expressions.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateExpressionWithError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("IsPredicate", "false");

            // body
            JSONObject expression = new JSONObject();
            expression.put("name", "CheckInvoice");
            expression.put("expression", "1 + "); // error
            expression.put("expressionType", "groovy");
            expression.put("nextNode", "nextStep");

            JSONArray expressions = new JSONArray();
            expressions.put(expression);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/validation/expression"), expressions.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertExpressionErrorResponse(json);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUrlWithSuccess() {
        try {
            // params
            HashMap<String, String> params = new HashMap<>();
            params.put("httpUrl", "https://www.google.com"); // Uma URL válida

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/url"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT_204);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUrlWithError() {
        try {
            // params
            HashMap<String, String> params = new HashMap<>();
            params.put("httpUrl", "http://url-error-test.com");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/url"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("error").asText()).isEqualTo("Url is not reachable from the server!");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUriWithSuccess() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Uri", "direct:teste"); // uri valid on Camel

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/uri"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.readTree(response.body());

            // asserts contents
            assertThat(body.get("message").asText().toLowerCase()).contains("valid");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateUriWithError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Uri", "::::uri-invalid::::"); // invalid uri

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/uri"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode body = mapper.readTree(response.body());

            // asserts contents
            assertThat(body.get("message").asText().toLowerCase()).contains("invalid");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
