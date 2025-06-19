package org.assimbly.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertUtils {

    private static final String EXCHANGES_COMPLETED = "exchangesCompleted";
    private static final String EXCHANGES_FAILED = "exchangesFailed";
    private static final String EXCHANGES_IN_FLIGHT = "exchangesInflight";

    private static final String COMPLETED = "completed";
    private static final String FAILED = "failed";
    private static final String PENDING = "pending";

    private static final String DETAILS = "details";
    private static final String HEADERS = "headers";
    private static final String MESSAGE = "message";
    private static final String ROUTE = "route";
    private static final String STATE = "state";
    private static final String STATUS = "status";
    private static final String TIMESTAMP = "timestamp";
    private static final String TOTAL = "total";
    private static final String UPTIME_MILLIS = "uptimeMillis";
    private static final String UPTIME = "uptime";
    private static final String VERSION = "version";

    private static final String TIMESTAMP_IS_VALID = "Check if timestamp is a valid date";


    private AssertUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    // generic

    public static void assertSuccessfulGenericResponse(JsonNode responseJson, String msg, boolean messageFlag) {
        assertThat(responseJson.get(DETAILS).asText()).isEqualTo("successful");
        if(messageFlag) {
            assertMessage(responseJson, msg);
        }
        assertThat(responseJson.get(STATUS).asInt()).isEqualTo(200);
        assertThat(responseJson.get(TIMESTAMP).asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(responseJson.get(TIMESTAMP).asText(), "yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(isValid).as(TIMESTAMP_IS_VALID).isTrue();
    }

    public static void assertSuccessfulGenericResponse(JsonNode responseJson, String msg) {
        assertSuccessfulGenericResponse(responseJson, msg, true);
    }

    public static void assertSuccessfulGenericResponse(JsonNode responseJson) {
        assertSuccessfulGenericResponse(responseJson, null);
    }

    public static void assertSuccessfulGenericResponse(JsonNode responseJson, String startsWithMsg, String endsWithMsg) {
        assertSuccessfulGenericResponse(responseJson, null);
        assertThat(responseJson.get(MESSAGE).asText()).startsWith(startsWithMsg).endsWith(endsWithMsg);
    }

    public static void assertSuccessfulGenericResponseWithoutMsg(JsonNode responseJson) {
        assertSuccessfulGenericResponse(responseJson, null, false);
    }

    private static void assertMessage(JsonNode responseJson, String msg) {
        if(msg != null) {
            assertThat(responseJson.get(MESSAGE).asText()).isEqualTo(msg);
        } else {
            assertThat(responseJson.get(MESSAGE).asText()).isNotEmpty();
        }
    }

    public static void assertErrorGenericResponse(JsonNode responseJson, String msg) {
        assertThat(responseJson.get(DETAILS).asText()).isEqualTo("failed. See the log for a complete stack trace");
        if(msg != null) {
            assertThat(responseJson.get(MESSAGE).asText()).isEqualTo(msg);
        } else {
            assertThat(responseJson.get(MESSAGE).asText()).isNotEmpty();
        }
        assertThat(responseJson.get(STATUS).asInt()).isEqualTo(400);
        assertThat(responseJson.get(TIMESTAMP).asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(responseJson.get(TIMESTAMP).asText(), "yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(isValid).as(TIMESTAMP_IS_VALID).isTrue();
    }

    public static void assertErrorGenericResponse(JsonNode responseJson) {
        assertErrorGenericResponse(responseJson, null);
    }

    // health

    public static void assertSuccessfulHealthResponse(JsonNode flowJson, String id) {
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get(STATE).asText()).isEqualTo("UP");
    }

    public static void assertJvmHealthResponse(JsonNode jvmJson) {
        assertThat(jvmJson.get("openFileDescriptors").asInt()).isPositive();
        assertThat(jvmJson.get("maxFileDescriptors").asInt()).isPositive();
    }

    public static void assertMemoryHealthResponse(JsonNode memoryJson) {
        assertThat(memoryJson.get("current").asInt()).isPositive();
        assertThat(memoryJson.get("committed").asInt()).isPositive();
        assertThat(memoryJson.get("max").asInt()).isPositive();
        assertThat(memoryJson.get("cached").asInt()).isPositive();
        assertThat(memoryJson.get("currentUsedPercentage").asInt()).isBetween(0, 100);
    }

    public static void assertThreadsHealthResponse(JsonNode threadsJson) {
        assertThat(threadsJson.get("threadCount").asInt()).isPositive();
        assertThat(threadsJson.get("peakThreadCount").asInt()).isPositive();
    }

    public static void assertHealthResponse(JsonNode responseJson, String status) {
        assertThat(responseJson.get(UPTIME_MILLIS).asInt()).isPositive();
        assertThat(responseJson.get("startedSteps").asInt()).isNotNegative();
        assertThat(responseJson.get("memoryUsage").asDouble()).isPositive();
        assertThat(responseJson.get(EXCHANGES_IN_FLIGHT).asInt()).isNotNegative();
        assertThat(responseJson.get("camelVersion").asText()).isNotEmpty();
        assertThat(responseJson.get(EXCHANGES_COMPLETED).asInt()).isNotNegative();
        assertThat(responseJson.get("camelId").asText()).isNotEmpty();
        assertThat(responseJson.get(UPTIME).asText()).isNotEmpty();
        assertThat(responseJson.get("startedFlows").asText()).isNotEmpty();
        assertThat(responseJson.get("totalThreads").asInt()).isPositive();
        assertThat(responseJson.get("cpuLoadLastMinute").asText()).isNotEmpty();
        assertThat(responseJson.get("cpuLoadLast15Minutes").asText()).isNotEmpty();
        assertThat(responseJson.get("exchangesTotal").asInt()).isNotNegative();
        assertThat(responseJson.get(EXCHANGES_FAILED).asInt()).isNotNegative();
        assertThat(responseJson.get("cpuLoadLast5Minutes").asText()).isNotEmpty();
        assertThat(responseJson.get(STATUS).asText()).isEqualTo(status);
    }

    // flow manager

    public static void assertSuccessfulHealthResponse(JsonNode flowJson, int steps, String id, String event, String message) {
        assertThat(flowJson.get("steps").size()).isEqualTo(steps);
        assertSuccessfulEventResponse(flowJson, id, event, message);
    }

    public static void assertSuccessfulEventResponseWithoutVersion(JsonNode flowJson, String id, String event, String message) {
        assertThat(flowJson.get("name").asText()).isEqualTo(id);
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get("time").asText()).matches("\\d+ milliseconds");
        assertThat(flowJson.get("event").asText()).isEqualTo(event);
        assertThat(flowJson.get(MESSAGE).asText()).isEqualTo(message);
    }

    public static void assertSuccessfulEventResponse(JsonNode flowJson, String id, String event, String message) {
        assertSuccessfulEventResponseWithoutVersion(flowJson, id, event, message);
        assertThat(flowJson.get(VERSION).asText()).matches("\\d+");
    }

    // flow configurer

    public static void assertFlowRouteResponse(JsonNode routeJson) {
        assertThat(routeJson.get("id").asText()).isNotNull();
        assertThat(routeJson.get("routeConfigurationId").asText()).isNotNull();
        assertThat(routeJson.get("from")).isNotNull();
        assertThat(routeJson.get("step")).isNotNull();
    }

    public static void assertFlowRoutesResponse(JsonNode routesJson) {
        assertThat(routesJson.get(ROUTE)).isNotNull();
        assertThat(routesJson.get(ROUTE).isArray()).isTrue();
        assertThat(routesJson.get(ROUTE).size()).isPositive();
    }

    public static void assertComponentFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains("scheme", "producerOnly", "kind", "deprecated", "groupId", "description",
                "browsable", "label", "supportLevel", "title", "remote", VERSION, "javaType", "async", "firstVersion",
                "lenientProperties", "name", "syntax", "artifactId", "api", "consumerOnly", "extendsScheme"
        );
    }

    public static void assertFlowDocumentationFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains("component", "componentProperties", HEADERS, "properties");
    }

    public static void assertFlowStepFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains("apiVersion", "kind", "metadata", "spec");
    }

    // statistics

    public static void assertStepStatsResponse(JsonNode stepJson, JsonNode statsJson, String id, String status) {
        assertThat(stepJson.get("id").asText()).isEqualTo(id);
        assertThat(stepJson.get(STATUS).asText()).isEqualTo(status);
        assertThat(statsJson.get("externalRedeliveries").asInt()).isZero();
        assertThat(statsJson.get("idleSince").asInt()).isNegative();
        assertThat(statsJson.get("maxProcessingTime").asInt()).isZero();
        assertThat(statsJson.get(EXCHANGES_FAILED).asInt()).isZero();
        assertThat(statsJson.get("redeliveries").asInt()).isZero();
        assertThat(statsJson.get("minProcessingTime").asInt()).isZero();
        assertThat(statsJson.get("lastProcessingTime").asInt()).isNegative();
        assertThat(statsJson.get("meanProcessingTime").asInt()).isNegative();
        assertThat(statsJson.get("failuresHandled").asInt()).isZero();
        assertThat(statsJson.get("totalProcessingTime").asInt()).isZero();
        assertThat(statsJson.get(EXCHANGES_COMPLETED).asInt()).isZero();
        assertThat(statsJson.get("deltaProcessingTime").asInt()).isZero();
    }

    public static void assertStatsResponse(JsonNode responseJson, String status) {
        assertThat(responseJson.get(UPTIME_MILLIS).asInt()).isPositive();
        assertThat(responseJson.get("startedSteps").isInt()).isTrue();
        assertThat(responseJson.get("memoryUsage").isDouble()).isTrue();
        assertThat(responseJson.get(EXCHANGES_IN_FLIGHT).isInt()).isTrue();
        assertThat(responseJson.get("camelVersion")).isNotNull();
        assertThat(responseJson.get(EXCHANGES_COMPLETED).isInt()).isTrue();
        assertThat(responseJson.get("camelId")).isNotNull();
        assertThat(responseJson.get(UPTIME)).isNotNull();
        assertThat(responseJson.get("startedFlows")).isNotNull();
        assertThat(responseJson.get("totalThreads").isInt()).isTrue();
        assertThat(responseJson.get("exchangesTotal").isInt()).isTrue();
        assertThat(responseJson.get(EXCHANGES_FAILED).isInt()).isTrue();
        assertThat(responseJson.get(STATUS).asText()).isEqualTo(status);
        assertCpuLoadStatsResponse(responseJson);
    }

    public static void assertFlowsStatsResponse(JsonNode flowJson, String status) {
        assertThat(flowJson.get(UPTIME_MILLIS).asInt()).isPositive();
        assertThat(flowJson.get(PENDING).isInt()).isTrue();
        assertThat(flowJson.get(COMPLETED).isInt()).isTrue();
        assertThat(flowJson.get(FAILED).isInt()).isTrue();
        assertThat(flowJson.get("lastFailed")).isNotNull();
        assertThat(flowJson.get("timeout").isInt()).isTrue();
        assertThat(flowJson.get(UPTIME)).isNotNull();
        assertThat(flowJson.get(TOTAL).isInt()).isTrue();
        assertThat(flowJson.get("lastCompleted")).isNotNull();
        assertThat(flowJson.get("id")).isNotNull();
        assertThat(flowJson.get(STATUS).asText()).isEqualTo(status);
    }

    public static void assertCpuLoadStatsResponse(JsonNode responseJson) {
        assertThat(responseJson.get("cpuLoadLastMinute")).isNotNull();
        assertThat(responseJson.get("cpuLoadLast5Minutes")).isNotNull();
        assertThat(responseJson.get("cpuLoadLast15Minutes")).isNotNull();
    }

    public static void assertEmptyFlowStatsResponse(JsonNode flowJson, String id) {
        assertThat(flowJson.get(TOTAL).asInt()).isZero();
        assertThat(flowJson.get(PENDING).asInt()).isZero();
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get(COMPLETED).asInt()).isZero();
        assertThat(flowJson.get(FAILED).asInt()).isZero();
    }

    public static void assertMessageStatFieldsResponse(JsonNode flowJson, String id) {
        assertThat(flowJson.get(TOTAL).asInt()).isNotNegative();
        assertThat(flowJson.get(PENDING).asInt()).isNotNegative();
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get(COMPLETED).asInt()).isNotNegative();
        assertThat(flowJson.get(FAILED).asInt()).isZero();
    }

    public static void assertMetricStatFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains(VERSION, "gauges", "counters", "histograms", "meters", "timers");
    }

    public static void assertStepStatFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains("id", STATE, EXCHANGES_IN_FLIGHT, EXCHANGES_COMPLETED, EXCHANGES_FAILED,
                "failuresHandled", "redeliveries", "externalRedeliveries", "minProcessingTime", "maxProcessingTime",
                "totalProcessingTime", "lastProcessingTime", "deltaProcessingTime", "meanProcessingTime", "idleSince",
                "startTimestamp", "resetTimestamp", "firstExchangeCompletedTimestamp", "firstExchangeCompletedExchangeId",
                "firstExchangeFailureTimestamp", "firstExchangeFailureExchangeId", "lastExchangeCreatedTimestamp",
                "lastExchangeCompletedTimestamp", "lastExchangeCompletedExchangeId", "lastExchangeFailureTimestamp",
                "lastExchangeFailureExchangeId", "routeStats"
        );
    }

    public static void assertHistoryMetricStatFieldsResponse(List<String> fieldNames) {
        assertThat(fieldNames).contains(VERSION, "gauges", "counters", "histograms", "meters", "timers");
    }

    // flow manager

    public static void assertFlowInfoResponse(JsonNode flowJson, String id, String status, String isRunning) {
        assertThat(flowJson.get("isRunning").asText()).isEqualTo(isRunning);
        assertThat(flowJson.get("name").asText()).isEqualTo(id);
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get(STATUS).asText()).isEqualTo(status);
    }

    public static void assertStepsLoadedResponse(JsonNode stepsLoadedJson, int total, int successfully, int failed) {
        assertThat(stepsLoadedJson.get(TOTAL).asInt()).isEqualTo(total);
        assertThat(stepsLoadedJson.get("successfully").asInt()).isEqualTo(successfully);
        assertThat(stepsLoadedJson.get(FAILED).asInt()).isEqualTo(failed);
    }

    public static void assertStepsLoadedResponse(JsonNode stepsLoadedJson) {
        assertThat(stepsLoadedJson.get(TOTAL).asInt()).isPositive();
        assertThat(stepsLoadedJson.get("successfully").asInt()).isPositive();
        assertThat(stepsLoadedJson.get(FAILED).asInt()).isZero();
    }

    public static void assertStepResponse(JsonNode stepJson, String type, String status) {
        assertThat(stepJson.get("id").asText()).isNotNull();
        assertThat(stepJson.get("type").asText()).isEqualTo(type);
        assertThat(stepJson.get(STATUS).asText()).isEqualTo(status);
    }

    // integration

    public static void assertIntegrationInfoResponse(JsonNode infoJson, String startupType) {
        assertThat(infoJson.get("numberOfRunningSteps").asInt()).isPositive();
        assertThat(infoJson.get("startupType").asText()).isEqualTo(startupType);
        assertThat(infoJson.get("uptimeMiliseconds").asInt()).isPositive();
        assertThat(infoJson.get("name").asText()).isNotEmpty();
        assertThat(infoJson.get(VERSION).asText()).isNotEmpty();
        assertThat(infoJson.get(UPTIME).asText()).isNotEmpty();
        assertThat(infoJson.get("startDate").asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(infoJson.get("startDate").asText(), "EEE MMM dd HH:mm:ss zzz yyyy");
        assertThat(isValid).as(TIMESTAMP_IS_VALID).isTrue();
    }

    public static void assertFlowsDetailsResponse(JsonNode flowJson, String id, String status, String isRunning) {
        assertFlowInfoResponse(flowJson, id, status, isRunning);
        assertThat(flowJson.get(UPTIME).asText()).matches("\\d+s");
    }

    public static void assertThreadsResponse(JsonNode threadJson) {
        assertThat(threadJson.get("cpuTime").asLong()).isPositive();
        assertThat(threadJson.get("name").asText()).isNotEmpty();
        assertThat(threadJson.get("id").asInt()).isPositive();
        assertThat(threadJson.get(STATUS).asText()).isNotEmpty();
    }

    public static void assertSoapListResponse(JsonNode responseJson) {
        assertThat(responseJson.size()).isEqualTo(4);
        assertThat(responseJson).allMatch(element -> {
            boolean hasValidName = element.has("name") && (
                    element.get("name").asText().equals("Add") ||
                            element.get("name").asText().equals("Subtract") ||
                            element.get("name").asText().equals("Multiply") ||
                            element.get("name").asText().equals("Divide")
            );
            boolean hasEmptyHeaders = element.has(HEADERS) && element.get(HEADERS).isArray() && element.get(HEADERS).isEmpty();
            return hasValidName && hasEmptyHeaders;
        });
    }

    // validations

    public static void assertCertificateResponse(JsonNode responseJson, String status, String containsMsg) {
        assertThat(responseJson.get("validationResultStatus").asText()).isEqualTo(status);
        assertThat(responseJson.get(MESSAGE).asText().toLowerCase()).contains(containsMsg);
    }

    public static void assertCertificateResponseWithoutMessage(JsonNode responseJson, String status) {
        assertThat(responseJson.get("validationResultStatus").asText()).isEqualTo(status);
    }

    public static void assertConnectionResponse(JsonNode responseJson, String details, String msg) {
        assertThat(responseJson.get(DETAILS).asText()).isEqualTo(details);
        assertThat(responseJson.get(MESSAGE).asText()).isEqualTo(msg);
    }

    public static void assertXsltErrorResponse(JsonNode responseJson) {
        assertThat(responseJson.isArray()).isTrue();
        assertThat(responseJson.size()).isPositive();
        for (JsonNode errorNode : responseJson) {
            assertThat(errorNode.get("error").asText().toLowerCase()).contains("i/o error");
        }
    }

    public static void assertScriptResponse(JsonNode responseJson, int code, String result) {
        assertThat(responseJson.get("code").asInt()).isEqualTo(code);
        assertThat(responseJson.get("result").asText()).isEqualTo(result);
    }

    public static void assertScriptErrorResponse(String msg) {
        assertThat(msg).contains("invalid groovy script");
        assertThat(msg).contains("startup failed");
    }

    public static void assertExpressionErrorResponse(JsonNode json) {
        assertThat(json.isArray()).isTrue();
        assertThat(json.size()).isGreaterThan(0);
        for (JsonNode error : json) {
            assertThat(error.get("error").asText().toLowerCase())
                    .contains("could not compile")
                    .contains("checkinvoice");
        }
    }

    // broker manager

    public static void assertBrokerInfoResponse(String response, String type) {
        Map<String, String> outputMap = Arrays.stream(response.split(","))
                .map(s -> s.split("=", 2))
                .collect(Collectors.toMap(arr -> arr[0], arr -> arr[1]));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode responseJson = objectMapper.valueToTree(outputMap);

        assertThat(responseJson.get(UPTIME).asText().trim()).matches("\\d+(\\.\\d+)? seconds");
        assertThat(responseJson.get("totalConnections").asInt()).isZero();
        assertThat(responseJson.get("currentConnections").asInt()).isZero();
        assertThat(responseJson.get("totalConsumers").asInt()).isZero();
        assertThat(responseJson.get("totalMessages").asInt()).isZero();
        assertThat(responseJson.get("nodeId").asText()).isNotNull();
        assertThat(responseJson.get(STATE).asBoolean()).isTrue();
        assertThat(responseJson.get(VERSION).asText()).isNotNull();
        assertThat(responseJson.get("type").asText()).isEqualTo(type);
    }

    // broker health

    public static void assertEngineHealthResponse(JsonNode responseJson) {
        assertThat(responseJson.get("totalNumberOfQueues").isInt()).isTrue();
        assertThat(responseJson.get("openConnections").isInt()).isTrue();
        assertThat(responseJson.get("averageMessageSize").isInt()).isTrue();
        assertThat(responseJson.get("storePercentUsage").isInt()).isTrue();
        assertThat(responseJson.get("memoryPercentUsage").isInt()).isTrue();
        assertThat(responseJson.get("totalNumberOfTemporaryQueues").isInt()).isTrue();
        assertThat(responseJson.get("maxConnections").isInt()).isTrue();
        assertThat(responseJson.get("tmpPercentUsage").isInt()).isTrue();
    }

    // broker message

    public static void assertBrokerMessagesResponse(JsonNode messageJson, String body) {
        assertThat(messageJson.get(HEADERS)).isNotNull();
        assertThat(messageJson.get("body").asText()).isEqualTo(body);
        assertBrokerMessagesResponse(messageJson);
    }

    public static void assertBrokerMessagesResponse(JsonNode messageJson) {
        assertThat(messageJson.get("jmsHeaders")).isNotNull();
        assertThat(messageJson.get("messageid").asText()).isNotNull();
        assertThat(messageJson.get(TIMESTAMP).asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(messageJson.get(TIMESTAMP).asText(), "E MMM dd HH:mm:ss z yyyy");
        assertThat(isValid).as(TIMESTAMP_IS_VALID).isTrue();
    }

    // broker queue manager

    public static void assertBrokerQueueResponse(JsonNode queueJson, String temporary, String address, String name) {
        assertThat(queueJson.get("temporary").asText()).isEqualTo(temporary);
        assertThat(queueJson.get("address").asText()).isEqualTo(address);
        assertThat(queueJson.get("numberOfConsumers").asInt()).isZero();
        assertThat(queueJson.get("name").asText()).isEqualTo(name);
        assertThat(queueJson.get("numberOfMessages").asInt()).isGreaterThanOrEqualTo(0);
    }

    public static void assertBrokerQueueResponse(JsonNode queueJson, String temporary) {
        assertThat(queueJson.get("temporary").asText()).isEqualTo(temporary);
        assertThat(queueJson.get("address").asText()).isNotNull();
        assertThat(queueJson.get("numberOfConsumers").asInt()).isZero();
        assertThat(queueJson.get("name").asText()).isNotNull();
        assertThat(queueJson.get("numberOfMessages").asInt()).isZero();
    }

}
