package org.assimbly.integrationrest;

import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


public class UserJWTControllerTest {

    @Test
    void shouldAuthenticateAndGetToken() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/authenticate", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("db", TestApplicationContext.DB);

            // body
            String body = "{\"username\": \"admin\", \"password\": \"admin\", \"rememberMe\": \"false\"}";

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body, null, headers);

            // assertions
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.body()).inPath("id_token");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
