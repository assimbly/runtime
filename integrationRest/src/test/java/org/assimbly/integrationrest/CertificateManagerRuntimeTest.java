package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.Utils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CertificateManagerRuntimeTest {

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
    @Order(1)
    @Disabled("to be tested on future releases")
    void shouldGenerateCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("cn", "example.com");
            headers.put("keystoreName", "myKeystore.jks");
            headers.put("keystorePassword", "changeit");

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/certificates/generate"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldUploadCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("FileType", "pem");
            headers.put("keystoreName", "myKeystore.jks");
            headers.put("keystorePassword", "changeit");

            // body
            String certificate = Utils.readFileAsStringFromResources("certificates/certificate.pem");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/certificates/upload"), certificate, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldUploadP12Certificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("FileType", "pem");
            headers.put("keystoreName", "myKeystore.jks");
            headers.put("keystorePassword", "changeit");
            headers.put("password", "changeit");

            // body
            byte[] certificateBytes = Utils.readFileAsBytesFromResources("certificates/keystore.p12");
            String base64Encoded = Base64.getEncoder().encodeToString(certificateBytes);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/certificates/uploadp12"), base64Encoded, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void shouldImportCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/certificates/import"), "https://www.google.com", null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(5)
    void shouldSetCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/certificates/set"), "https://www.google.com", null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "Certificates set");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(6)
    void shouldUpdateCertificate() {
        try {
            // params
            HashMap<String, String> params = new HashMap();
            params.put("url", "https://www.google.com");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            // body
            JSONObject certificate = new JSONObject();
            certificate.put("certificateName", "example.com");
            certificate.put("certificateFile", Utils.readFileAsStringFromResources("certificates/certificate.pem"));
            certificate.put("certificateExpiry", "2000-12-31T12:00:00.123Z");
            JSONArray certificateArray = new JSONArray();
            certificateArray.put(certificate);
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("certificate", certificateArray);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/certificates/update"), bodyJson.toString(), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThat(response.body()).isEqualTo("truststore updated");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(7)
    void shouldDeleteCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("keystoreName", "myKeystore.jks");
            headers.put("keystorePassword", "changeit");

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/certificates/delete/example"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThat(response.body()).isEqualTo("success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
