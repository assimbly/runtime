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
class MessageBrokerRuntimeTest {

    private static final String BROKER_TYPE = "classic";

    private static final String QUEUE_TEST_1 = "queue_test_1";
    private static final String QUEUE_TEST_2 = "queue_test_2";

    private static final String BODY = "Hello world!";

    private static boolean messageSentOnQueue1 = false;
    private static boolean messageMovedFromQueue1 = false;

    private static String messageIdOnQueue1;

    private static AssimblyGatewayBrokerContainer container;

    @BeforeAll
    static void init() {
        container = new AssimblyGatewayBrokerContainer();
        container.init();
        createQueue(QUEUE_TEST_1);
        createQueue(QUEUE_TEST_2);
    }

    @AfterAll
    static void tearDown() {
        container.stop();
    }

    @BeforeEach
    void setUp(TestInfo testInfo) {
        try {

            if (testInfo.getTags().contains("NeedsMessageOnQueueTest1") && (!messageSentOnQueue1 || messageMovedFromQueue1)) {
                // params
                HashMap<String, String> params = new HashMap<>();
                params.put("messageHeaders", "{\"test\":\"1234\"}");

                // headers
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
                headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

                // endpoint call
                HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/message/" + QUEUE_TEST_1 + "/send"), BODY, params, headers);

                // get messageId from queue
                messageIdOnQueue1 = getMessageIdFromQueueName(QUEUE_TEST_1);

                setMessageFlags(true, false);
            }

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    private static void createQueue(String queueName) {
        // headers
        HashMap<String, String> headers = new HashMap();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/queue/" + queueName), null, null, headers);
    }

    private static String getMessageIdFromQueueName(String queueName) {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + queueName + "/browse"), null, headers);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode messageJson = responseJson.get("messages").get("message").get(0);

            return messageJson.get("messageid").asText();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
            return null;
        }
    }

    private void setMessageFlags(boolean messageSentOnQueue1, boolean messageMovedFromQueue1) {
        MessageBrokerRuntimeTest.messageSentOnQueue1 = messageSentOnQueue1;
        MessageBrokerRuntimeTest.messageMovedFromQueue1 = messageMovedFromQueue1;
    }

    @Test
    @Order(1)
    void shouldSendMessageToEndpoint() {
        try {
            // params
            HashMap<String, String> params = new HashMap<>();
            params.put("messageHeaders", "{\"test\":\"1234\"}");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/message/" + QUEUE_TEST_1 + "/send"), BODY, params, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "success");

            // set message flags
            setMessageFlags(true, false);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetDelayedMessagesCount() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldGetDelayedMessagesCount test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/delayedmessages/" + QUEUE_TEST_1 + "/count"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "0");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetFlowsMessageCount() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldGetFlowsMessageCount test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/flows/message/count"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "{}");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetMessagesCountByEndpoint() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldGetMessagesCountByEndpoint test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + QUEUE_TEST_1 + "/count"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "1");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldBrowseMessagesByEndpoint() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldBrowseMessagesByEndpoint test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + QUEUE_TEST_1 + "/browse"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode messagesJson = responseJson.get("messages").get("message");
            JsonNode messageJson = messagesJson.get(0);

            // asserts contents
            AssertUtils.assertBrokerMessagesResponse(messageJson, BODY);

            messageIdOnQueue1 = messageJson.get("messageid").asText();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldFilterMessagesByEndpoint() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldFilterMessagesByEndpoint test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + QUEUE_TEST_1), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode messagesJson = responseJson.get("messages").get("message");
            JsonNode messageJson = messagesJson.get(0);

            // asserts contents
            AssertUtils.assertBrokerMessagesResponse(messageJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldGetMessagesCount() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldGetMessagesCount test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/count"), QUEUE_TEST_1, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "1");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldBrowseMessageByEndpointAndMessageId() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldBrowseMessageByEndpointAndMessageId test because shouldSendMessageToEndpoint test did not run.");
            assumeTrue(messageIdOnQueue1 != null, "Skipping shouldBrowseMessageByEndpointAndMessageId test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/message/" + QUEUE_TEST_1 + "/browse/" + messageIdOnQueue1), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode messagesJson = responseJson.get("messages").get("message");
            JsonNode messageJson = messagesJson.get(0);

            // asserts contents
            AssertUtils.assertBrokerMessagesResponse(messageJson, BODY);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void shouldDeleteMessageByEndpointAndMessageId() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldBrowseMessageByEndpointAndMessageId test because shouldSendMessageToEndpoint test did not run.");
            assumeTrue(messageIdOnQueue1 != null, "Skipping shouldBrowseMessageByEndpointAndMessageId test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/message/" + QUEUE_TEST_1 + "/"+messageIdOnQueue1), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "true");

            // set message flags
            setMessageFlags(false, false);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsMessageOnQueueTest1")
    @Order(11)
    void shouldDeleteMessagesByEndpoint() {
        try {
            // check for necessary data before continue
            assumeTrue(messageSentOnQueue1, "Skipping shouldGetDelayedMessagesCount test because shouldSendMessageToEndpoint test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + QUEUE_TEST_1), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "1");

            // set message flags
            setMessageFlags(false, false);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsMessageOnQueueTest1")
    @Order(20)
    void shouldMoveMessageFromQueue1ToQueue2() {
        try {

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/message/" + QUEUE_TEST_1 + "/" + QUEUE_TEST_2 + "/" + messageIdOnQueue1), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "true");

            // set message flags
            setMessageFlags(false, true);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsMessageOnQueueTest1")
    @Order(21)
    void shouldMoveMessagesFromQueue1ToQueue2() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/brokers/" + BROKER_TYPE + "/messages/" + QUEUE_TEST_1 + "/" + QUEUE_TEST_2), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

            // set message flags
            setMessageFlags(false, true);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
