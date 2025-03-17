package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IntegrationRuntimeTest {

    private Properties inboundHttpsCamelContextProp = TestApplicationContext.buildInboundHttpsExample();
    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();
    private Properties collectorProp = TestApplicationContext.buildCollectorExample();

    private static String flowIdStep;
    private static String flowIdRoute;
    private static String flowIdLog;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled")) {
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
        }
    }

    //////////////////////////////////////////
    // collector

    @Test
    @Order(1)
    void shouldAddCollector() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/collectors/add", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = (String)collectorProp.get(TestApplicationContext.CollectorField.collector.name());

            // set ids to be used on other unit tests
            flowIdStep = (String)collectorProp.get(TestApplicationContext.CollectorField.flowIdStep.name());
            flowIdRoute = (String)collectorProp.get(TestApplicationContext.CollectorField.flowIdRoute.name());
            flowIdLog = (String)collectorProp.get(TestApplicationContext.CollectorField.flowIdLog.name());

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"path\":\"/integration/collectors/add\",\"details\":\"successful\",\"id\":\"1\",\"message\":\"configured\",\"status\":200}");
            assertThatJson(response.body()).whenIgnoringPaths("timestamp").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(2)
    void shouldRemoveStepCollector() {
        try {
            // check for necessary data before continue
            assumeTrue(flowIdStep != null, "Skipping shouldRemoveStepCollector test because shouldAddCollector test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/collector/%s/remove", baseUrl, flowIdStep);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"path\":\"/integration/collector/{collectorId}/remove\",\"details\":\"successful\",\"id\":\"1\",\"message\":\"removed\",\"status\":200}");
            assertThatJson(response.body()).whenIgnoringPaths("timestamp").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(3)
    void shouldRemoveRouteCollector() {
        try {
            // check for necessary data before continue
            assumeTrue(flowIdRoute != null, "Skipping shouldRemoveRouteCollector test because shouldAddCollector test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/collector/%s/remove", baseUrl, flowIdRoute);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"path\":\"/integration/collector/{collectorId}/remove\",\"details\":\"successful\",\"id\":\"1\",\"message\":\"removed\",\"status\":200}");
            assertThatJson(response.body()).whenIgnoringPaths("timestamp").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    void shouldRemoveLogCollector() {
        try {
            // check for necessary data before continue
            assumeTrue(flowIdLog != null, "Skipping shouldRemoveLogCollector test because shouldAddCollector test did not run.");

            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/collector/%s/remove", baseUrl, flowIdLog);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "DELETE", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"path\":\"/integration/collector/{collectorId}/remove\",\"details\":\"successful\",\"id\":\"1\",\"message\":\"removed\",\"status\":200}");
            assertThatJson(response.body()).whenIgnoringPaths("timestamp").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    //////////////////////////////////////////
    // flow interaction

    @Test
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

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
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

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetListOfFlows() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/list/flows", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get flow info
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get(0).size()).isEqualTo(1);
            assertThat(responseJson.get(0).get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    //////////////////////////////////////////
    // stats

    @Test
    void shouldGetEmptyFlowStats() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/stats", baseUrl, inboundHttpsCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.body()).isEqualTo("{\"flow\":{\"total\":0,\"pending\":0,\"id\":\"67921474ecaafe0007000000\",\"completed\":0,\"failed\":0}}");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetStats() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/stats", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("uptimeMillis").asInt()).isGreaterThan(0);
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
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/stats/flows", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.size()).isGreaterThanOrEqualTo(0);

            JsonNode flowJson = responseJson.get(0).get("flow");

            assertThat(flowJson.get("uptimeMillis").asInt()).isGreaterThan(0);
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
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetStatsByFlowIds() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/statsbyflowids", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            String body = (String)schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name());

            // endpoint call - get stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode flowJson = responseJson.get(0).get("flow");

            assertThat(flowJson.get("uptimeMillis").asInt()).isGreaterThan(0);
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
            assertThat(flowJson.get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.id.name()));
            assertThat(flowJson.get("status").asText()).isEqualTo("started");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    //////////////////////////////////////////
    // others

    @Test
    void shouldBeStarted() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = baseUrl + "/api/integration/isstarted";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"path\":\"/integration/isstarted\",\"details\":\"successful\",\"id\":\"1\",\"message\":\"true\",\"status\":200}");
            assertThatJson(response.body()).whenIgnoringPaths("timestamp").isEqualTo(expectedBodyJson);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldCountFlows() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = baseUrl + "/api/integration/count/flows";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThatJson(responseJson)
                    .whenIgnoringPaths("timestamp", "message")
                    .isEqualTo(objectMapper.readTree("{\"path\":\"/integration/count/flows\",\"details\":\"successful\",\"id\":\"1\",\"status\":200}"));

            JsonNode messageNode = responseJson.get("message");
            assertThat(messageNode).isNotNull();
            assertThat(messageNode.isTextual()).isTrue();

            int messageValue = Integer.parseInt(messageNode.asText());
            assertThat(messageValue).isGreaterThan(0);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldListSoapActions() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = baseUrl + "/api/integration/list/soap/action";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // body
            JSONObject body = new JSONObject();
            body.put("url", "http://www.dneonline.com/calculator.asmx?wsdl");

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", body.toString(), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.size()).isEqualTo(4);

            assertThat(responseJson).allMatch(element -> {
                boolean hasValidName = element.has("name") && (
                        element.get("name").asText().equals("Add") ||
                        element.get("name").asText().equals("Subtract") ||
                        element.get("name").asText().equals("Multiply") ||
                        element.get("name").asText().equals("Divide")
                );
                boolean hasEmptyHeaders = element.has("headers") && element.get("headers").isArray() && element.get("headers").isEmpty();
                return hasValidName && hasEmptyHeaders;
            });

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
