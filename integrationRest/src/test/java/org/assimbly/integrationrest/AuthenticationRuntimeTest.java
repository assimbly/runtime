package org.assimbly.integrationrest;

import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.assimbly.integrationrest.utils.GoogleTOTPUtil;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.MongoUtil;
import org.bson.Document;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;

import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationRuntimeTest {

    private static String authToken;
    private static String totpSecret;

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

    @Test
    @Order(1)
    void shouldAuthenticateAndGenerateDBToken() {
        try {
            // create user on mongodb
            MongoUtil.createUser(TestApplicationContext.FIRST_NAME_USER, TestApplicationContext.LAST_NAME_USER, TestApplicationContext.EMAIL_USER, TestApplicationContext.PASSWORD_USER);

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/db/authenticate", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            String data = TestApplicationContext.EMAIL_USER + ":" + TestApplicationContext.PASSWORD_USER;
            String auth = Base64.getEncoder().encodeToString(data.getBytes());
            headers.put("Authorization", auth);
            headers.put("db", TestApplicationContext.DB);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // assertions
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // authToken to be used on other unit tests
            authToken = response.body();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldRegisterAuthentication() {
        try {
            // check for necessary data before continue
            assumeTrue(authToken != null, "Skipping shouldRegisterAuthentication test because shouldAuthenticateAndGenerateDBToken test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/authentication/register", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("db", TestApplicationContext.DB);
            headers.put("domainName", TestApplicationContext.DOMAIN_NAME);
            headers.put("Authorization", authToken);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // assertions
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.headers().map()).containsKey("location");

            // totpSecret to be used on other unit tests
            String location = response.headers().map().get("location").get(0);
            location = URLDecoder.decode(location, "UTF-8");
            totpSecret = HttpUtil.extractSecret(location);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldValidateAuthentication() {
        try {
            // check for necessary data before continue
            assumeTrue(totpSecret != null, "Skipping shouldValidateAuthentication test because shouldRegisterAuthentication test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/authentication/validate", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // body
            Document bodyDoc = new Document();
            bodyDoc.append("email", TestApplicationContext.EMAIL_USER);
            bodyDoc.append("token", GoogleTOTPUtil.generateToken(totpSecret));

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", bodyDoc.toJson(), null, headers);

            // assertions
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("true");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void shouldRemoveAuthentication() {
        try {
            // check for necessary data before continue
            assumeTrue(authToken != null, "Skipping shouldRemoveAuthentication test because shouldAuthenticateAndGenerateDBToken test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/authentication/remove", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // body
            Document bodyDoc = new Document();
            headers.put("Authorization", authToken);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // assertions
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
