package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.commons.utils.AssertUtils;
import org.assimbly.commons.utils.HttpUtil;
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

    private static boolean schedulerFlowInstalled = false;
    private static boolean inboundHttpsFlowInstalled = false;

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

            // endpoint call
            HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/install"), (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Order(1)
    void shouldInstallFlow() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/install"), (String) inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/isstarted"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/lasterror"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/alerts"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/alerts/count"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/events"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/pause"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "pause", "Paused flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/resume"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "resume", "Resumed flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/stop"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "stop", "Stopped flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/start"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");
            JsonNode stepsLoadedJson = flowJson.get("stepsLoaded");

            // asserts contents
            AssertUtils.assertStepsLoadedResponse(stepsLoadedJson, 5, 5, 0);
            AssertUtils.assertSuccessfulHealthResponse(flowJson, 5, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "start", "Started flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/restart"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");
            JsonNode stepsLoadedJson = flowJson.get("stepsLoaded");

            // asserts contents
            AssertUtils.assertStepsLoadedResponse(stepsLoadedJson, 5, 5, 0);
            AssertUtils.assertSuccessfulHealthResponse(flowJson, 5, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "start", "Started flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/status"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/uptime"), null, headers);

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/uninstall"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertSuccessfulEventResponse(flowJson, (String)inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "stop", "Stopped flow successfully");

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
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.getRequest(container.buildBrokerApiPath("/api/integration/flow/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/info"), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");

            // asserts contents
            AssertUtils.assertFlowInfoResponse(flowJson, (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()), "started", "true");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    @Order(31)
    void shouldInstallRoute() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/route/"+schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name())+"/install"), (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_1.name()), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get("flow");
            JsonNode stepsLoadedJson = flowJson.get("stepsLoaded");
            JsonNode stepsJson = flowJson.get("steps");
            JsonNode stepJson = stepsJson.get(0);

            // asserts contents
            AssertUtils.assertStepsLoadedResponse(stepsLoadedJson);
            AssertUtils.assertSuccessfulEventResponseWithoutVersion(flowJson, (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ROUTE_ID_1.name()), "start", "Started flow successfully");
            AssertUtils.assertStepResponse(stepJson, "route", "success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(40)
    void shouldInstallFlowByFile() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/install/file"), (String) inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, String.format("flow %s saved in the deploy directory", inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(41)
    void shouldSetMaintenanceTime() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONArray jsonArray = new JSONArray();
            jsonArray.put(inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // endpoint call
            HttpResponse<String> response = HttpUtil.postRequest(container.buildBrokerApiPath("/api/integration/flow/maintenance/60000"), jsonArray.toString() , null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, "Set flows into maintenance mode for 60000 miliseconds");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(42)
    void shouldUninstallFlowByFile() {
        try {
            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.deleteRequest(container.buildBrokerApiPath("/api/integration/flow/"+inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())+"/uninstall/file"), null, null, headers);

            // assert http status
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            // asserts contents
            AssertUtils.assertSuccessfulGenericResponse(responseJson, String.format("flow %s deleted from deploy directory", inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name())));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
