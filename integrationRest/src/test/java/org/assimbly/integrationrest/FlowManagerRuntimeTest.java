package org.assimbly.integrationrest;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.assimbly.util.api.AssertUtils;
import org.assimbly.util.api.HttpUtil;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONArray;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlowManagerRuntimeTest {

    private final Properties inboundHttpsCamelContextProp = TestApplicationContext.buildInboundHttpsExample();
    private final Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();

    private static boolean schedulerFlowInstalled;
    private static boolean inboundHttpsFlowInstalled;

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
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpUtil.postRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/install"), (String) schedulerCamelContextProp.get(TestApplicationContext.DILField.DIL.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Order(1)
    void shouldInstallFlow() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/install"), (String) inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.DIL.name()), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            inboundHttpsFlowInstalled = true;

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void checkIfFlowIsStarted() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/isstarted"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "true");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(10)
    void shouldGetFlowLastError() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/lasterror"), null, headers);

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
    @Order(10)
    void shouldGetFlowAlerts() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/alerts"), null, headers);

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
    @Order(10)
    void shouldCountFlowAlerts() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/alerts/count"), null, headers);

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
    @Order(10)
    void shouldGetFlowEvents() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/events"), null, headers);

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
    @Order(20)
    void shouldPauseFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldPauseFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/pause"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "pause", "Paused flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(21)
    void shouldResumeFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldResumeFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/resume"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "resume", "Resumed flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(22)
    void shouldStopFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldStopFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/stop"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "stop", "Stopped flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(23)
    void shouldStartFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldStartFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/start"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");
            JsonNode installedStepsJson = flowJson.get("installed");

            // asserts contents
            AssertUtils.assertStepsLoadedResponse(installedStepsJson, 4, 4, 0);
            AssertUtils.assertSuccessfulHealthResponse(flowJson, 4, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "start", "Started flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(24)
    void shouldRestartFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldRestartFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/restart"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");
            JsonNode installedStepsJson = flowJson.get("installed");

            // asserts contents
            AssertUtils.assertStepsLoadedResponse(installedStepsJson, 4, 4, 0);
            AssertUtils.assertSuccessfulHealthResponse(flowJson, 4, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "start", "Started flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(25)
    void shouldGetFlowStatus() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldGetFlowStatus test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/status"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(26)
    void shouldGetFlowUptime() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldGetFlowUptime test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/uptime"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(30)
    void shouldUninstallFlow() {
        try {
            assumeTrue(inboundHttpsFlowInstalled, "Skipping shouldUninstallFlow test because shouldInstallFlow test did not run.");

            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/uninstall"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.DILField.ID.name()), "stop", "Stopped flow successfully");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    @Order(30)
    void shouldGetSchedulerFlowInfo() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildGatewayHeadlessApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.DILField.ID.name())+"/info"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            String id = (String)schedulerCamelContextProp.get(TestApplicationContext.DILField.ID.name());
            String name = (String)schedulerCamelContextProp.get(TestApplicationContext.DILField.NAME.name());

            // asserts contents
            AssertUtils.assertFlowInfoResponse(flowJson, id, name, "started", "true");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
