package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.commons.utils.Utils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TopicManagerRuntimeTest {

    private static final String BROKER_TYPE = "classic";

    private static final String TOPIC_TEST = "topic_test";

    private static boolean topicCreated = false;
    private static boolean messageSentToTopic = false;

    private static AssimblyGatewayBrokerContainer container;

    @BeforeAll
    static void init() {
        container = new AssimblyGatewayBrokerContainer();
        container.init();
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        try {

            if (testInfo.getTags().contains("NeedsMessageOnTopic") && !messageSentToTopic) {
                // params
                HashMap<String, String> params = new HashMap();
                params.put("messageHeaders", "{\"test\":\"1234\"}");

                // headers
                HashMap<String, String> headers = new HashMap();
                headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
                headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

                // endpoint call
                HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/message/"+TOPIC_TEST+"/send"), "Hello World!", params, headers);

                messageSentToTopic = true;
            }

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(1)
    void shouldCreateTopic() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topic/"+TOPIC_TEST), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");

            topicCreated = true;

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetTopic() {
        try {
            // check for necessary data before continue
            assumeTrue(topicCreated, "Skipping shouldGetTopic test because shouldCreateTopic test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topic/"+TOPIC_TEST), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode topicJson = responseJson.get("topic");

            // asserts contents
            AssertUtils.assertBrokerQueueResponse(topicJson, "false", TOPIC_TEST, TOPIC_TEST);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetTopics() {
        try {
            // check for necessary data before continue
            assumeTrue(topicCreated, "Skipping shouldGetTopics test because shouldCreateTopic test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topics"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode topicsJson = responseJson.get("topics").get("topic");
            JsonNode topicJson = topicsJson.get(0);

            // asserts contents
            AssertUtils.assertBrokerQueueResponse(topicJson, "false");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void shouldClearTopic() {
        try {
            // check for necessary data before continue
            assumeTrue(topicCreated, "Skipping shouldClearTopic test because shouldCreateTopic test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topic/"+TOPIC_TEST+"/clear"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void shouldClearTopics() {
        try {
            // check for necessary data before continue
            assumeTrue(topicCreated, "Skipping shouldClearTopics test because shouldCreateTopic test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topics/clear"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(20)
    void shouldDeleteTopic() {
        try {
            // check for necessary data before continue
            assumeTrue(topicCreated, "Skipping shouldDeleteTopic test because shouldCreateTopic test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/topic/"+TOPIC_TEST), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponseWithoutMsg(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
