package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.brokerrest.utils.HttpUtil;
import org.assimbly.brokerrest.utils.Utils;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueueManagerRuntimeTest {

    private static final String BROKER_TYPE = "classic";

    private static final String QUEUE_TEST = "queue_test";

    private static boolean queueCreated = false;
    private static boolean messageSentToQueue = false;

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

            if (testInfo.getTags().contains("NeedsMessageOnQueue") && !messageSentToQueue) {
                // url
                String baseUrl = container.getBrokerBaseUrl();
                String url = String.format("%s/api/brokers/%s/message/%s/send", baseUrl, BROKER_TYPE, QUEUE_TEST);

                // params
                HashMap<String, String> params = new HashMap();
                params.put("messageHeaders", "{\"test\":\"1234\"}");

                // headers
                HashMap<String, String> headers = new HashMap();
                headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
                headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

                // endpoint call
                HttpUtil.makeHttpCall(url, "POST", "Hello World!", params, headers);

                messageSentToQueue = true;
            }

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(1)
    void shouldCreateQueue() {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queue/%s", baseUrl, BROKER_TYPE, QUEUE_TEST);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("success");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

            queueCreated = true;

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    @Tag("NeedsMessageOnQueue")
    void shouldGetQueue() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldGetQueue test because shouldCreateQueue test did not run.");

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queue/%s", baseUrl, BROKER_TYPE, QUEUE_TEST);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode queueJson = responseJson.get("queue");
            assertThat(queueJson.get("temporary").asText()).isEqualTo("false");
            assertThat(queueJson.get("address").asText()).isEqualTo(QUEUE_TEST);
            assertThat(queueJson.get("numberOfConsumers").asInt()).isZero();
            assertThat(queueJson.get("name").asText()).isEqualTo(QUEUE_TEST);
            assertThat(queueJson.get("numberOfMessages").asInt()).isPositive();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetQueues() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldGetQueues test because shouldCreateQueue test did not run.");

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queues", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode queuesJson = responseJson.get("queues").get("queue");
            assertThat(queuesJson.isArray()).isTrue();
            assertThat(queuesJson.size()).isPositive();

            JsonNode queueJson = queuesJson.get(0);
            assertThat(queueJson.get("temporary").asText()).isEqualTo("false");
            assertThat(queueJson.get("address").asText()).isNotNull();
            assertThat(queueJson.get("numberOfConsumers").asInt()).isZero();
            assertThat(queueJson.get("name").asText()).isNotNull();
            assertThat(queueJson.get("numberOfMessages").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    @Tag("NeedsMessageOnQueue")
    void shouldClearQueue() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldClearQueue test because shouldCreateQueue test did not run.");

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queue/%s/clear", baseUrl, BROKER_TYPE, QUEUE_TEST);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("success");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    @Tag("NeedsMessageOnQueue")
    void shouldClearQueues() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldClearQueues test because shouldCreateQueue test did not run.");

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queues/clear", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("success");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(20)
    void shouldDeleteQueue() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldDeleteQueue test because shouldCreateQueue test did not run.");

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/queue/%s", baseUrl, BROKER_TYPE, QUEUE_TEST);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
