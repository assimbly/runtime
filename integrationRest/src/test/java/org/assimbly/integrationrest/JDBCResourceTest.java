package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JDBCResourceTest {

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
    void shouldValidateJdbcConnection() {
        try {
            // params
            HashMap<String, String> params = new HashMap();
            params.put("type", "MYSQL");
            params.put("user", "rfamro");
            params.put("host", "mysql-rfam-public.ebi.ac.uk");
            params.put("instance", "");
            params.put("pwd", "");
            params.put("port", "4497");
            params.put("useSSL", "false");
            params.put("enabledTLSProtocols", "false");
            params.put("escapeChars", "false");
            params.put("database", "Rfam");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/jdbc"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThat(response.body()).isEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldValidateJdbcConnectionWithError() {
        try {
            // params
            HashMap<String, String> params = new HashMap();
            params.put("type", "MYSQL");
            params.put("user", "rfamro");
            params.put("host", "mysql-rfam-public.ebi.ac.uk");
            params.put("instance", "");
            params.put("pwd", "123");
            params.put("port", "4497");
            params.put("useSSL", "false");
            params.put("enabledTLSProtocols", "false");
            params.put("escapeChars", "false");
            params.put("database", "Rfam");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/validation/jdbc"), params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("error").asText())
                    .startsWith("Access denied for user 'rfamro'@")
                    .endsWith("(using password: YES)");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
