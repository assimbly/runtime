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
                // url
                String baseUrl = container.getBrokerBaseUrl();
                String url = String.format("%s/api/brokers/%s/message/%s/send", baseUrl, BROKER_TYPE, QUEUE_TEST_1);

                // params
                HashMap<String, String> params = new HashMap();
                params.put("messageHeaders", "{\"test\":\"1234\"}");

                // headers
                HashMap<String, String> headers = new HashMap();
                headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
                headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

                // endpoint call
                HttpUtil.makeHttpCall(url, "POST", BODY, params, headers);

                // get messageId from queue
                messageIdOnQueue1 = getMessageIdFromQueueName(QUEUE_TEST_1);

                setMessageFlags(true, false);
            }

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    private static void createQueue(String queueName) {
        // url
        String baseUrl = container.getBrokerBaseUrl();
        String url = String.format("%s/api/brokers/%s/queue/%s", baseUrl, BROKER_TYPE, queueName);

        // headers
        HashMap<String, String> headers = new HashMap();
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

        // endpoint call
        HttpUtil.makeHttpCall(url, "POST", null, null, headers);
    }

    private static String getMessageIdFromQueueName(String queueName) {
        try {
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/%s/browse", baseUrl, BROKER_TYPE, queueName);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

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
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/message/%s/send", baseUrl, BROKER_TYPE, QUEUE_TEST_1);

            // params
            HashMap<String, String> params = new HashMap();
            params.put("messageHeaders", "{\"test\":\"1234\"}");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", BODY, params, headers);

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/delayedmessages/%s/count", baseUrl, BROKER_TYPE, QUEUE_TEST_1);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("0");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/flows/message/count", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("{}");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/%s/count", baseUrl, BROKER_TYPE, QUEUE_TEST_1);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("1");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/%s/browse", baseUrl, BROKER_TYPE, QUEUE_TEST_1);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode messagesJson = responseJson.get("messages").get("message");
            assertThat(messagesJson.isArray()).isTrue();
            assertThat(messagesJson.size()).isPositive();

            JsonNode messageJson = messagesJson.get(0);
            assertThat(messageJson.get("headers")).isNotNull();
            assertThat(messageJson.get("jmsHeaders")).isNotNull();
            assertThat(messageJson.get("messageid").asText()).isNotNull();
            assertThat(messageJson.get("body").asText()).isEqualTo(BODY);
            assertThat(messageJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(messageJson.get("timestamp").asText(), "E MMM dd HH:mm:ss z yyyy");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();

            String url = String.format("%s/api/brokers/%s/messages/%s/filter", baseUrl, BROKER_TYPE, QUEUE_TEST_1);
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode messagesJson = responseJson.get("messages").get("message");
            assertThat(messagesJson.isArray()).isTrue();
            assertThat(messagesJson.size()).isPositive();

            JsonNode messageJson = messagesJson.get(0);
            assertThat(messageJson.get("jmsHeaders")).isNotNull();
            assertThat(messageJson.get("messageid").asText()).isNotNull();
            assertThat(messageJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(messageJson.get("timestamp").asText(), "E MMM dd HH:mm:ss z yyyy");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/count", baseUrl, BROKER_TYPE);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", QUEUE_TEST_1, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("1");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/message/%s/browse/%s", baseUrl, BROKER_TYPE, QUEUE_TEST_1, messageIdOnQueue1);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            JsonNode messagesJson = responseJson.get("messages").get("message");
            assertThat(messagesJson.isArray()).isTrue();
            assertThat(messagesJson.size()).isPositive();

            JsonNode messageJson = messagesJson.get(0);
            assertThat(messageJson.get("headers")).isNotNull();
            assertThat(messageJson.get("jmsHeaders")).isNotNull();
            assertThat(messageJson.get("messageid").asText()).isNotNull();
            assertThat(messageJson.get("body").asText()).isEqualTo(BODY);
            assertThat(messageJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(messageJson.get("timestamp").asText(), "E MMM dd HH:mm:ss z yyyy");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/message/%s/%s", baseUrl, BROKER_TYPE, QUEUE_TEST_1, messageIdOnQueue1);

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
            assertThat(responseJson.get("message").asText()).isEqualTo("true");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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

            // url
            String baseUrl = container.getBrokerBaseUrl();

            String url = String.format("%s/api/brokers/%s/messages/%s", baseUrl, BROKER_TYPE, QUEUE_TEST_1);
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
            assertThat(responseJson.get("message").asText()).isEqualTo("1");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/message/%s/%s/%s", baseUrl, BROKER_TYPE, QUEUE_TEST_1, QUEUE_TEST_2, messageIdOnQueue1);

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
            assertThat(responseJson.get("message").asText()).isEqualTo("true");
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

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
            // url
            String baseUrl = container.getBrokerBaseUrl();
            String url = String.format("%s/api/brokers/%s/messages/%s/%s", baseUrl, BROKER_TYPE, QUEUE_TEST_1, QUEUE_TEST_2);

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
            assertThat(responseJson.get("message").asInt()).isPositive();
            assertThat(responseJson.get("status").asInt()).isEqualTo(200);
            assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
            boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
            assertThat(isValid).as("Check if timestamp is a valid date").isTrue();

            setMessageFlags(false, true);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
