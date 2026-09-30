package org.assimbly.integrationrest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.assimbly.util.api.AssertUtils;
import org.assimbly.util.api.HttpUtil;
import org.assimbly.util.api.ApiUtils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.eclipse.jetty.http.HttpStatus;
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
    void shouldGenerateIdentity() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("cn", "example.com");
            headers.put("keystoreName", "myKeystore.p12");
            headers.put("keystorePassword", "changeit");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildGatewayHeadlessApiPath("/api/certificates/identity/generate"), "", null, headers);

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
    void shouldImportTrustedCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "myKeystore.p12");
            headers.put("keystorePassword", "changeit");

            // body
            String certificate = ApiUtils.readFileAsStringFromResources("certificates/certificate.pem");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildGatewayHeadlessApiPath("/api/certificates/example-com"), certificate, null, headers);

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
    void shouldImportIdentity() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "myKeystore.p12");
            headers.put("keystorePassword", "changeit");
            headers.put("password", "changeit");

            // body
            byte[] certificateBytes = ApiUtils.readFileAsBytesFromResources("certificates/keystore.p12");
            String base64Encoded = Base64.getEncoder().encodeToString(certificateBytes);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildGatewayHeadlessApiPath("/api/certificates/identity/example"), base64Encoded, null, headers);

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
    void shouldAddRenewAndDeleteDomainCertificates() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            String path = container.buildGatewayHeadlessApiPath("/api/certificates/domain/www.google.com");
            ObjectMapper objectMapper = new ObjectMapper();

            // add: downloads and stores the root certificate
            HttpResponse<String> response = HttpUtil.postRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(objectMapper.readTree(response.body()));

            // add again: the domain already has certificates
            response = HttpUtil.postRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT_409);

            // renew: replaces them
            response = HttpUtil.putRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // get: the certificates of the domain
            response = HttpUtil.getRequest(path, null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode certificates = objectMapper.readTree(response.body());
            assertThat(certificates.isArray()).isTrue();
            assertThat(certificates).isNotEmpty();
            assertThat(certificates.get(0).get("name").asString()).isEqualTo("google-com-root-ca");

            // delete: removes them
            response = HttpUtil.deleteRequest(path, null, null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            response = HttpUtil.getRequest(path, null, headers);
            assertThat(objectMapper.readTree(response.body())).isEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(6)
    void shouldReplaceCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            // body
            String certificate = ApiUtils.readFileAsStringFromResources("certificates/certificate.pem");

            // endpoint call
            HttpResponse<String> response = HttpUtil.putRequest(container.buildGatewayHeadlessApiPath("/api/certificates/example-com"), certificate, null, headers);

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
    @Order(7)
    void shouldDeleteCertificate() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("keystoreName", "myKeystore.p12");
            headers.put("keystorePassword", "changeit");

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildGatewayHeadlessApiPath("/api/certificates/example"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThat(response.body()).isEqualTo("success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(8)
    void shouldGetCertificatesExpiry() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("keystoreName", "keystore");
            headers.put("keystorePassword", "supersecret");

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/certificates"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson).isNotEmpty();
            assertThat(responseJson.get(0).has("daysUntilExpiry")).isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }


    @Test
    @Order(9)
    void shouldListCertificatesOfDefaultTruststore() {
        try {
            // headers: no keystoreName or keystorePassword, so outbound-truststore.p12 with the runtime's password
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/certificates"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.isArray()).isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
