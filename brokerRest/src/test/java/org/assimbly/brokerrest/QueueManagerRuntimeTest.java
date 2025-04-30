package org.assimbly.brokerrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.HttpUtil;
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
                // params
                HashMap<String, String> params = new HashMap();
                params.put("messageHeaders", "{\"test\":\"1234\"}");

                // headers
                HashMap<String, String> headers = new HashMap();
                headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
                headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

                // endpoint call
                HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/message/"+QUEUE_TEST+"/send"), "Hello World!", params, headers);

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
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");

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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queues"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode queuesJson = responseJson.get("queues").get("queue");
            JsonNode queueJson = queuesJson.get(0);

            // asserts contents
            assertThat(queuesJson.isArray()).isTrue();
            assertThat(queuesJson.size()).isPositive();
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

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST+"/clear"), null, null, headers);

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
    @Tag("NeedsMessageOnQueue")
    void shouldClearQueues() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldClearQueues test because shouldCreateQueue test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queues/clear"), null, null, headers);

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
    void shouldDeleteQueue() {
        try {
            // check for necessary data before continue
            assumeTrue(queueCreated, "Skipping shouldDeleteQueue test because shouldCreateQueue test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST), null, null, headers);

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
