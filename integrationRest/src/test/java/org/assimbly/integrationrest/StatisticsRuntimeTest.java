package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.Utils;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.commons.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatisticsRuntimeTest {

    private Properties inboundHttpsCamelContextProp = TestApplicationContext.buildInboundHttpsExample();
    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

    private static boolean schedulerFlowInstalled = false;

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

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled") && !schedulerFlowInstalled) {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/install"), (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/messages"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            assertThat(flowJson.get("total").asInt()).isZero();
            assertThat(flowJson.get("pending").asInt()).isZero();
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("completed").asInt()).isZero();
            assertThat(flowJson.get("failed").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetCompleteFlowMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/messages/completed"), null, headers);

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
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFailedFlowMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/messages/failed"), null, headers);

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
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetPendingFlowMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/messages/pending"), null, headers);

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
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetTotalFlowMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/messages/total"), null, headers);

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
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowMessagesByStepId() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/step/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())+"/messages"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode stepJson = responseJson.get("step");

            // asserts contents
            assertThat(stepJson.get("total").asInt()).isZero();
            assertThat(stepJson.get("pending").asInt()).isZero();
            assertThat(stepJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(stepJson.get("completed").asInt()).isZero();
            assertThat(stepJson.get("failed").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowStatsByStepId() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/step/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())+"/stats"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode stepJson = responseJson.get("step");
            JsonNode statsJson = stepJson.get("stats");

            // asserts contents
            assertThat(stepJson.get("id").asText()).isEqualTo(String.format("%s-%s",
                    schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()),
                    schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())
                    )
            );
            assertThat(stepJson.get("status").asText()).isEqualTo("started");
            assertThat(statsJson.get("externalRedeliveries").asInt()).isZero();
            assertThat(statsJson.get("idleSince").asInt()).isNegative();
            assertThat(statsJson.get("maxProcessingTime").asInt()).isZero();
            assertThat(statsJson.get("exchangesFailed").asInt()).isZero();
            assertThat(statsJson.get("redeliveries").asInt()).isZero();
            assertThat(statsJson.get("minProcessingTime").asInt()).isZero();
            assertThat(statsJson.get("lastProcessingTime").asInt()).isNegative();
            assertThat(statsJson.get("meanProcessingTime").asInt()).isNegative();
            assertThat(statsJson.get("failuresHandled").asInt()).isZero();
            assertThat(statsJson.get("totalProcessingTime").asInt()).isZero();
            assertThat(statsJson.get("exchangesCompleted").asInt()).isZero();
            assertThat(statsJson.get("deltaProcessingTime").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetHistoryMetrics() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/historymetrics"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());

            // asserts contents
            assertThat(fieldNames).contains("version", "gauges", "counters", "histograms", "meters", "timers");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetMessages() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/messages"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            // asserts contents
            assertThat(responseJson).isNotNull();
            assertThat(responseJson.isArray()).isTrue();
            assertThat(flowJson.get("total").asInt()).isNotNegative();
            assertThat(flowJson.get("pending").asInt()).isNotNegative();
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("completed").asInt()).isNotNegative();
            assertThat(flowJson.get("failed").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetMetrics() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/metrics"), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());

            // asserts contents
            assertThat(fieldNames).contains("version", "gauges", "counters", "histograms", "meters", "timers");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStepsStats() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/stats/steps"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode camelContextStatJson = responseJson.get("camelContextStat");
            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(camelContextStatJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());

            // asserts contents
            assertThat(fieldNames).contains("id", "state", "exchangesInflight", "exchangesCompleted", "exchangesFailed",
                    "failuresHandled", "redeliveries", "externalRedeliveries", "minProcessingTime", "maxProcessingTime",
                    "totalProcessingTime", "lastProcessingTime", "deltaProcessingTime", "meanProcessingTime", "idleSince",
                    "startTimestamp", "resetTimestamp", "firstExchangeCompletedTimestamp", "firstExchangeCompletedExchangeId",
                    "firstExchangeFailureTimestamp", "firstExchangeFailureExchangeId", "lastExchangeCreatedTimestamp",
                    "lastExchangeCompletedTimestamp", "lastExchangeCompletedExchangeId", "lastExchangeFailureTimestamp",
                    "lastExchangeFailureExchangeId", "routeStats"
            );

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetEmptyFlowStats() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/stats"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            assertThat(flowJson.get("total").asInt()).isZero();
            assertThat(flowJson.get("pending").asInt()).isZero();
            assertThat(flowJson.get("id").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("completed").asInt()).isZero();
            assertThat(flowJson.get("failed").asInt()).isZero();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetStats() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/stats"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            assertThat(responseJson.get("uptimeMillis").asInt()).isPositive();
            assertThat(responseJson.get("startedSteps").isInt()).isTrue();
            assertThat(responseJson.get("memoryUsage").isDouble()).isTrue();
            assertThat(responseJson.get("exchangesInflight").isInt()).isTrue();
            assertThat(responseJson.get("camelVersion")).isNotNull();
            assertThat(responseJson.get("exchangesCompleted").isInt()).isTrue();
            assertThat(responseJson.get("camelId")).isNotNull();
            assertThat(responseJson.get("uptime")).isNotNull();
            assertThat(responseJson.get("startedFlows")).isNotNull();
            assertThat(responseJson.get("totalThreads").isInt()).isTrue();
            assertThat(responseJson.get("cpuLoadLastMinute")).isNotNull();
            assertThat(responseJson.get("cpuLoadLast15Minutes")).isNotNull();
            assertThat(responseJson.get("exchangesTotal").isInt()).isTrue();
            assertThat(responseJson.get("exchangesFailed").isInt()).isTrue();
            assertThat(responseJson.get("cpuLoadLast5Minutes")).isNotNull();
            assertThat(responseJson.get("status").asText()).isEqualTo("Started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStatsFlows() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/stats/flows"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            // asserts contents
            assertThat(responseJson.size()).isNotNegative();
            assertThat(flowJson.get("uptimeMillis").asInt()).isPositive();
            assertThat(flowJson.get("pending").isInt()).isTrue();
            assertThat(flowJson.get("completed").isInt()).isTrue();
            assertThat(flowJson.get("failed").isInt()).isTrue();
            assertThat(flowJson.get("lastFailed")).isNotNull();
            assertThat(flowJson.get("timeout").isInt()).isTrue();
            assertThat(flowJson.get("uptime")).isNotNull();
            assertThat(flowJson.get("total").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLastMinute").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLast15Minutes").isInt()).isTrue();
            assertThat(flowJson.get("lastCompleted")).isNotNull();
            assertThat(flowJson.get("cpuLoadLast5Minutes").isInt()).isTrue();
            assertThat(flowJson.get("id")).isNotNull();
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStatsByFlowIds() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name());

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/statsbyflowids"), body, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            // asserts contents
            assertThat(flowJson.get("uptimeMillis").asInt()).isPositive();
            assertThat(flowJson.get("pending").isInt()).isTrue();
            assertThat(flowJson.get("completed").isInt()).isTrue();
            assertThat(flowJson.get("failed").isInt()).isTrue();
            assertThat(flowJson.get("lastFailed")).isNotNull();
            assertThat(flowJson.get("timeout").isInt()).isTrue();
            assertThat(flowJson.get("uptime")).isNotNull();
            assertThat(flowJson.get("total").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLastMinute").isInt()).isTrue();
            assertThat(flowJson.get("cpuLoadLast15Minutes").isInt()).isTrue();
            assertThat(flowJson.get("lastCompleted")).isNotNull();
            assertThat(flowJson.get("cpuLoadLast5Minutes").isInt()).isTrue();
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
