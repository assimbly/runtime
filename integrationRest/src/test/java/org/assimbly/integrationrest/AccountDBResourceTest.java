package org.assimbly.integrationrest;

import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.MongoUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountDBResourceTest {

    @Test
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
            assertThat(response.body()).isNotEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
