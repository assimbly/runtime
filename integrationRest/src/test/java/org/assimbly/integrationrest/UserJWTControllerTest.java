package org.assimbly.integrationrest;

import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


class UserJWTControllerTest {

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
    void shouldAuthenticateAndGetToken() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("db", TestApplicationContext.db);

            // body
            JSONObject bodyJson = new JSONObject();
            bodyJson.put("username", "admin");
            bodyJson.put("password", "admin");
            bodyJson.put("rememberMe", "false");

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/authenticate"), bodyJson.toString(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThatJson(response.body()).inPath("id_token");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
