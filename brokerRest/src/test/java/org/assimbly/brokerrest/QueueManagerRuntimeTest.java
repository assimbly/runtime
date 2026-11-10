package org.assimbly.brokerrest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.assimbly.brokerrest.testcontainers.AssimblyGatewayBrokerContainer;
import org.assimbly.util.api.AssertUtils;
import org.assimbly.util.api.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QueueManagerRuntimeTest {

    private static final String BROKER_TYPE = "classic";

    private static final String QUEUE_TEST = "queue_test";

    private static boolean queueCreated;
    private static boolean messageSentToQueue;

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

        if (testInfo.getTags().contains("NeedsMessageOnQueue") && !messageSentToQueue) {
            // params
            HashMap<String, String> params = new HashMap<>();
            params.put("messageHeaders", "{\"test\":\"1234\"}");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call
            HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/message/"+QUEUE_TEST+"/send"), "Hello World!", params, headers);

            messageSentToQueue = true;
        }

    }

    @Test
    @Order(1)
    void shouldCreateQueue() {
        // headers
        HashMap<String, String> headers = new HashMap<>();
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
    }

    @Test
    @Order(2)
    @Tag("NeedsMessageOnQueue")
    void shouldGetQueue() {
        // check for necessary data before continue
        assumeTrue(queueCreated, "Skipping shouldGetQueue test because shouldCreateQueue test did not run.");

        // headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST), null, headers);

        // assert http status
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.body());
        JsonNode queueJson = responseJson.get("queue");

        // asserts contents
        AssertUtils.assertBrokerQueueResponse(queueJson, "false", QUEUE_TEST, QUEUE_TEST);
    }

    @Test
    @Order(2)
    void shouldGetQueues() {
        // check for necessary data before continue
        assumeTrue(queueCreated, "Skipping shouldGetQueues test because shouldCreateQueue test did not run.");

        // headers
        HashMap<String, String> headers = new HashMap<>();
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
        AssertUtils.assertBrokerQueueResponse(queueJson, "false");
    }

    @Test
    @Order(10)
    @Tag("NeedsMessageOnQueue")
    void shouldClearQueue() {
        // check for necessary data before continue
        assumeTrue(queueCreated, "Skipping shouldClearQueue test because shouldCreateQueue test did not run.");

        // headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST+"/clear"), null, null, headers);

        // assert http status
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.body());

        // asserts contents
        AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");
    }

    @Test
    @Order(10)
    @Tag("NeedsMessageOnQueue")
    void shouldClearQueues() {
        // check for necessary data before continue
        assumeTrue(queueCreated, "Skipping shouldClearQueues test because shouldCreateQueue test did not run.");

        // headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queues/clear"), null, null, headers);

        // assert http status
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.body());

        // asserts contents
        AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");
    }

    @Test
    @Order(20)
    void shouldDeleteQueue() {
        // check for necessary data before continue
        assumeTrue(queueCreated, "Skipping shouldDeleteQueue test because shouldCreateQueue test did not run.");

        // headers
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/brokers/"+BROKER_TYPE+"/queue/"+QUEUE_TEST), null, null, headers);

        // assert http status
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.readTree(response.body());

        // asserts contents
        AssertUtils.assertSuccessfulGenericResponseWithoutMsg(responseJson);
    }

}
