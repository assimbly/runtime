package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FlowManagerRuntimeTest {

    private Properties inboundHttpsCamelContextProp = TestApplicationContext.buildInboundHttpsExample();
    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

    private static boolean schedulerFlowInstalled = false;
    private static boolean inboundHttpsFlowInstalled = false;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled") && !schedulerFlowInstalled) {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.camelContext.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Order(1)
    void shouldInstallFlow() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", (String) inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.camelContext.name()), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            inboundHttpsFlowInstalled = true;

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldPauseFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldPauseFlow test because shouldInstallFlow test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/pause", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - pause flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            assertThat(flowJson.get("name").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("id").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("time").asText()).matches("\\d+ milliseconds");
            assertThat(flowJson.get("event").asText()).isEqualTo("pause");
            assertThat(flowJson.get("message").asText()).isEqualTo("Paused flow successfully");
            assertThat(flowJson.get("version").asText()).matches("\\d+");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldResumeFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldResumeFlow test because shouldInstallFlow test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/resume", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - resume flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            assertThat(flowJson.get("name").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("id").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("time").asText()).matches("\\d+ milliseconds");
            assertThat(flowJson.get("event").asText()).isEqualTo("resume");
            assertThat(flowJson.get("message").asText()).isEqualTo("Resumed flow successfully");
            assertThat(flowJson.get("version").asText()).matches("\\d+");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void shouldUninstallFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldUninstallFlow test because shouldInstallFlow test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/uninstall", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - resume flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            assertThat(flowJson.get("name").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("id").asText()).isEqualTo(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("time").asText()).matches("\\d+ milliseconds");
            assertThat(flowJson.get("event").asText()).isEqualTo("stop");
            assertThat(flowJson.get("message").asText()).isEqualTo("Stopped flow successfully");
            assertThat(flowJson.get("version").asText()).matches("\\d+");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    @Order(5)
    void shouldGetSchedulerFlowInfo() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/info", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow info
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"flow\":{\"isRunning\":true,\"name\":\"67c740bc349ced00070004a9\",\"id\":\"67c740bc349ced00070004a9\",\"status\":\"started\"}}");
            assertThatJson(response.body()).whenIgnoringPaths("flow.uptime").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
