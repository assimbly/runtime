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
class IntegrationRuntimeTest {

    private Properties schedulerCamelContextProp = TestApplicationContext.buildSchedulerExample();
    private Properties collectorProp = TestApplicationContext.buildCollectorExample();

    private static String flowIdStep;
    private static String flowIdRoute;
    private static String flowIdLog;

    private static boolean schedulerFlowInstalled = false;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTags().contains("NeedsSchedulerFlowInstalled") && !schedulerFlowInstalled) {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

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
            String body = (String)collectorProp.get(TestApplicationContext.CollectorField.COLLECTOR.name());

            // set ids to be used on other unit tests
            flowIdStep = (String)collectorProp.get(TestApplicationContext.CollectorField.FLOW_ID_STEP.name());
            flowIdRoute = (String)collectorProp.get(TestApplicationContext.CollectorField.FLOW_ID_ROUTE.name());
            flowIdLog = (String)collectorProp.get(TestApplicationContext.CollectorField.FLOW_ID_LOG.name());

            // endpoint call - add collector
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

            // endpoint call - remove step collector
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

            // endpoint call - remove route collector
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

            // endpoint call - remove log collector
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
    @Tag("NeedsSchedulerFlowInstalled")
    @Order(5)
    void shouldGetListOfFlows() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/list/flows", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - get list of flows
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson.get(0).size()).isEqualTo(1);
            assertThat(responseJson.get(0).get("id").asText()).isEqualTo(schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

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

            // endpoint call - count flows
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
            assertThat(messageValue).isPositive();

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

            // endpoint call - list soap actions
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

    @Test
    void shouldGetBaseDirectory() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = baseUrl + "/api/integration/basedirectory";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("/data//.assimbly");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldSetBaseDirectory() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = baseUrl + "/api/integration/basedirectory";

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.TEXT_PLAIN_VALUE);

            // endpoint call - check if backend is started
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", "/data/.assimbly", null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("success");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
