package org.assimbly.integrationrest;

import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.commons.utils.Utils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.GoogleTOTPUtil;
import org.assimbly.integrationrest.utils.MongoUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.bson.Document;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.URLDecoder;
import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationResourceTest {

    private static String authToken;
    private static String totpSecret;

    private static AssimblyGatewayHeadlessContainer container;

    @BeforeAll
    static void setUp() {
        initializeContainers();
        generateAuthToken();
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    static void initializeContainers() {
        container = new AssimblyGatewayHeadlessContainer();
        container.init();
    }

    public static void generateAuthToken() {
        try {
            // create user on mongodb
            MongoUtil.createUser(container.getMongoContainer().getReplicaSetUrl(), TestApplicationContext.firstNameUser, TestApplicationContext.lastNameUser, TestApplicationContext.emailUser, TestApplicationContext.passwordUser);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Authorization", Utils.buildAuth(TestApplicationContext.emailUser, TestApplicationContext.passwordUser));
            headers.put("db", TestApplicationContext.db);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/db/authenticate"), null, headers);

            // assert http status
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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("db", TestApplicationContext.db);
            headers.put("domainName", TestApplicationContext.domainName);
            headers.put("Authorization", authToken);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath( "/api/authentication/register"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // body
            Document bodyDoc = new Document();
            bodyDoc.append("email", TestApplicationContext.emailUser);
            bodyDoc.append("token", GoogleTOTPUtil.generateToken(totpSecret));

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/authentication/validate"), bodyDoc.toJson(), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Authorization", authToken);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/authentication/remove"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
