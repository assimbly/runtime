package org.assimbly.commons.utils;

import com.fasterxml.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertUtils {

    public static void assertSuccessfulGenericResponse(JsonNode responseJson, String msg) {
        assertThat(responseJson.get("details").asText()).isEqualTo("successful");
        if(msg != null) {
            assertThat(responseJson.get("message").asText()).isEqualTo(msg);
        } else {
            assertThat(responseJson.get("message").asText()).isNotEmpty();
        }
        assertThat(responseJson.get("status").asInt()).isEqualTo(200);
        assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(isValid).as("Check if timestamp is a valid date").isTrue();
    }

    public static void assertSuccessfulGenericResponse(JsonNode responseJson) {
        assertSuccessfulGenericResponse(responseJson, null);
    }

    public static void assertSuccessfulGenericResponse(JsonNode responseJson, String startsWithMsg, String endsWithMsg) {
        assertSuccessfulGenericResponse(responseJson, null);
        assertThat(responseJson.get("message").asText()).startsWith(startsWithMsg).endsWith(endsWithMsg);
    }

    public static void assertErrorGenericResponse(JsonNode responseJson, String msg) {
        assertThat(responseJson.get("details").asText()).isEqualTo("failed. See the log for a complete stack trace");
        if(msg != null) {
            assertThat(responseJson.get("message").asText()).isEqualTo(msg);
        } else {
            assertThat(responseJson.get("message").asText()).isNotEmpty();
        }
        assertThat(responseJson.get("status").asInt()).isEqualTo(400);
        assertThat(responseJson.get("timestamp").asText()).isNotEmpty();
        boolean isValid = Utils.isValidDate(responseJson.get("timestamp").asText(), "yyyy-MM-dd HH:mm:ss.SSS");
        assertThat(isValid).as("Check if timestamp is a valid date").isTrue();
    }

    public static void assertErrorGenericResponse(JsonNode responseJson) {
        assertErrorGenericResponse(responseJson, null);
    }

    public static void assertSuccessfulEventResponse(JsonNode flowJson, String id, String event, String message) {
        assertThat(flowJson.get("name").asText()).isEqualTo(id);
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get("time").asText()).matches("\\d+ milliseconds");
        assertThat(flowJson.get("event").asText()).isEqualTo(event);
        assertThat(flowJson.get("message").asText()).isEqualTo(message);
        assertThat(flowJson.get("version").asText()).matches("\\d+");
    }

    public static void assertSuccessfulHealthResponse(JsonNode flowJson, String id) {
        assertThat(flowJson.get("id").asText()).isEqualTo(id);
        assertThat(flowJson.get("state").asText()).isEqualTo("UP");
    }

}
