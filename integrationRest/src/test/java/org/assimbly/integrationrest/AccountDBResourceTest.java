package org.assimbly.integrationrest;

import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.commons.utils.Utils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.MongoUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountDBResourceTest {

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
    void shouldAuthenticateAndGenerateDBToken() {
        try {
            // create user on mongodb
            MongoUtil.createUser(container.getMongoContainer().getReplicaSetUrl(), TestApplicationContext.firstNameUser, TestApplicationContext.lastNameUser, TestApplicationContext.emailUser, TestApplicationContext.passwordUser);

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Authorization", Utils.buildAuth(TestApplicationContext.emailUser, TestApplicationContext.passwordUser));
            headers.put("db", TestApplicationContext.db);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/db/authenticate"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            // asserts contents
            assertThat(response.body()).isNotEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
