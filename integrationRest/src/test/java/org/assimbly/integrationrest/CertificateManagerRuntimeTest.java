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

    private static final String API_CERTS_KEYSTORE = "api-certs-test.p12";
    private static final String API_CERTS_PASSWORD = "changeit";
    private static final String GENERATED_IDENTITY_NAME = "api-test-identity";
    private static final String EXPIRED_CERTIFICATE_NAME = "expired-example-com";
    private static final String EXPIRING_CERTIFICATE_NAME = "expiring-example-com";

    private static AssimblyGatewayHeadlessContainer container;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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
    void shouldGenerateIdentity() {
        try {
            HashMap<String, String> headers = apiCertsHeaders();
            headers.put("cn", "api-test.example.com");
            headers.put("certificateName", GENERATED_IDENTITY_NAME);

            HttpResponse<String> response = HttpUtil.postRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/generate"),
                    "",
                    null,
                    headers
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(OBJECT_MAPPER.readTree(response.body()));
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

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());

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

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());

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

            // add: downloads and stores the root certificate
            HttpResponse<String> response = HttpUtil.postRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(OBJECT_MAPPER.readTree(response.body()));

            // add again: the domain already has certificates
            response = HttpUtil.postRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.CONFLICT_409);

            // renew: replaces them
            response = HttpUtil.putRequest(path, "", null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // get: the certificates of the domain
            response = HttpUtil.getRequest(path, null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode certificates = OBJECT_MAPPER.readTree(response.body());
            assertThat(certificates.isArray()).isTrue();
            assertThat(certificates).isNotEmpty();
            assertThat(certificates.get(0).get("name").asString()).isEqualTo("google-com-root-ca");

            // delete: removes them
            response = HttpUtil.deleteRequest(path, null, null, headers);
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            response = HttpUtil.getRequest(path, null, headers);
            assertThat(OBJECT_MAPPER.readTree(response.body())).isEmpty();

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

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());

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

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());

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

            JsonNode responseJson = OBJECT_MAPPER.readTree(response.body());

            // asserts contents
            assertThat(responseJson.isArray()).isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void shouldListIdentities() {
        try {
            HttpResponse<String> response = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity"),
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode identities = OBJECT_MAPPER.readTree(response.body());
            assertThat(identities.isArray()).isTrue();
            assertThat(identities).isNotEmpty();

            JsonNode generated = findByName(identities, GENERATED_IDENTITY_NAME);
            assertThat(generated).isNotNull();
            AssertUtils.assertCertificateExpiry(generated, GENERATED_IDENTITY_NAME);
            assertThat(generated.get("valid").asBoolean()).isTrue();
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(11)
    void shouldGetIdentityByName() {
        try {
            HttpResponse<String> response = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/" + GENERATED_IDENTITY_NAME),
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertCertificateExpiry(OBJECT_MAPPER.readTree(response.body()), GENERATED_IDENTITY_NAME);

            HttpResponse<String> missing = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/does-not-exist"),
                    null,
                    apiCertsHeaders()
            );
            assertThat(missing.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(12)
    void shouldReplaceIdentity() {
        try {
            HashMap<String, String> headers = apiCertsHeaders();
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);
            headers.put("password", "changeit");

            byte[] certificateBytes = ApiUtils.readFileAsBytesFromResources("certificates/keystore.p12");
            String base64Encoded = Base64.getEncoder().encodeToString(certificateBytes);

            HttpResponse<String> response = HttpUtil.putRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/" + GENERATED_IDENTITY_NAME),
                    base64Encoded,
                    null,
                    headers
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(OBJECT_MAPPER.readTree(response.body()));
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(13)
    void shouldDeleteIdentity() {
        try {
            HttpResponse<String> response = HttpUtil.deleteRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/" + GENERATED_IDENTITY_NAME),
                    null,
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("success");

            HttpResponse<String> missing = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/identity/" + GENERATED_IDENTITY_NAME),
                    null,
                    apiCertsHeaders()
            );
            assertThat(missing.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(14)
    void shouldImportExpiredAndExpiringTrustedCertificates() {
        try {
            HashMap<String, String> headers = apiCertsHeaders();
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            HttpResponse<String> expiredResponse = HttpUtil.postRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/" + EXPIRED_CERTIFICATE_NAME),
                    ApiUtils.readFileAsStringFromResources("certificates/expired-certificate.pem"),
                    null,
                    headers
            );
            assertThat(expiredResponse.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(OBJECT_MAPPER.readTree(expiredResponse.body()));

            HttpResponse<String> expiringResponse = HttpUtil.postRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/" + EXPIRING_CERTIFICATE_NAME),
                    ApiUtils.readFileAsStringFromResources("certificates/expiring-certificate.pem"),
                    null,
                    headers
            );
            assertThat(expiringResponse.statusCode()).isEqualTo(HttpStatus.OK_200);
            AssertUtils.assertSuccessfulGenericResponse(OBJECT_MAPPER.readTree(expiringResponse.body()));
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(15)
    void shouldGetCertificateByName() {
        try {
            HttpResponse<String> response = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/" + EXPIRED_CERTIFICATE_NAME),
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode expiry = OBJECT_MAPPER.readTree(response.body());
            AssertUtils.assertCertificateExpiry(expiry, EXPIRED_CERTIFICATE_NAME);
            assertThat(expiry.get("valid").asBoolean()).isFalse();
            assertThat(expiry.get("daysUntilExpiry").asLong()).isZero();

            HttpResponse<String> missing = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/does-not-exist"),
                    null,
                    apiCertsHeaders()
            );
            assertThat(missing.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(16)
    void shouldGetExpiringCertificates() {
        try {
            // only still-valid certificates with daysUntilExpiry < numberOfDays
            HttpResponse<String> response = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/expiring/36500"),
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode expiring = OBJECT_MAPPER.readTree(response.body());
            assertThat(expiring.isArray()).isTrue();

            JsonNode match = findByName(expiring, EXPIRING_CERTIFICATE_NAME);
            assertThat(match).isNotNull();
            AssertUtils.assertCertificateExpiry(match, EXPIRING_CERTIFICATE_NAME);
            assertThat(match.get("valid").asBoolean()).isTrue();
            assertThat(match.get("daysUntilExpiry").asLong()).isPositive();

            assertThat(findByName(expiring, EXPIRED_CERTIFICATE_NAME)).isNull();
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(17)
    void shouldGetExpiredCertificates() {
        try {
            HttpResponse<String> response = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/expired"),
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode expired = OBJECT_MAPPER.readTree(response.body());
            assertThat(expired.isArray()).isTrue();
            assertThat(expired).isNotEmpty();

            JsonNode match = findByName(expired, EXPIRED_CERTIFICATE_NAME);
            assertThat(match).isNotNull();
            AssertUtils.assertExpiredCertificate(match, EXPIRED_CERTIFICATE_NAME);
            assertThat(match.get("daysExpired").asLong()).isPositive();
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(18)
    void shouldDeleteExpiredCertificates() {
        try {
            HttpResponse<String> response = HttpUtil.deleteRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/expired"),
                    null,
                    null,
                    apiCertsHeaders()
            );

            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            JsonNode deleted = OBJECT_MAPPER.readTree(response.body());
            assertThat(deleted.isArray()).isTrue();

            JsonNode match = findByName(deleted, EXPIRED_CERTIFICATE_NAME);
            assertThat(match).isNotNull();
            AssertUtils.assertExpiredCertificate(match, EXPIRED_CERTIFICATE_NAME);

            HttpResponse<String> remaining = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/expired"),
                    null,
                    apiCertsHeaders()
            );
            assertThat(remaining.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(OBJECT_MAPPER.readTree(remaining.body())).isEmpty();

            HttpResponse<String> missing = HttpUtil.getRequest(
                    container.buildGatewayHeadlessApiPath("/api/certificates/" + EXPIRED_CERTIFICATE_NAME),
                    null,
                    apiCertsHeaders()
            );
            assertThat(missing.statusCode()).isEqualTo(HttpStatus.NOT_FOUND_404);
        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    private static HashMap<String, String> apiCertsHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
        headers.put("keystoreName", API_CERTS_KEYSTORE);
        headers.put("keystorePassword", API_CERTS_PASSWORD);
        return headers;
    }

    private static JsonNode findByName(JsonNode array, String name) {
        for (JsonNode element : array) {
            if (name.equals(element.get("name").asString())) {
                return element;
            }
        }
        return null;
    }

}
