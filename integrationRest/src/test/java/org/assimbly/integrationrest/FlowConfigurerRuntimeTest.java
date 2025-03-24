package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.assimbly.integrationrest.utils.Utils;
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
class FlowConfigurerRuntimeTest {

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
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.CAMEL_CONTEXT.name()), null, headers);

            schedulerFlowInstalled = true;
        }
    }

    @Test
    @Order(1)
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldSetFlowConfiguration() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/configure", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // body
            String camelContext = TestApplicationContext.readFileAsStringFromResources("InboundHttpsCamelContext.xml");

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", camelContext, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());

            assertThat(responseJson.get("details").asText()).isEqualTo("successful");
            assertThat(responseJson.get("message").asText()).isEqualTo("Flow configuration set");
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
    @Tag("NeedsSchedulerFlowInstalled")
    void checkIfFlowIsConfigured() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/isconfigured", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

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
            assertThat(responseJson.get("message").asText()).isEqualTo("true");
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
    void shouldGetFlowConfiguration() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/configure", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_XML_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isNotEmpty();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(4)
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldGetFlowRoutesByFlowId() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/route", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            JsonNode routesJson = responseJson.get("routes");

            assertThat(routesJson).isNotNull();
            assertThat(routesJson.get("route")).isNotNull();
            assertThat(routesJson.get("route").isArray()).isTrue();
            assertThat(routesJson.get("route").size()).isPositive();

            JsonNode routeJson = routesJson.get("route").get(0);
            assertThat(routeJson.get("id").asText()).isNotNull();
            assertThat(routeJson.get("routeConfigurationId").asText()).isNotNull();
            assertThat(routeJson.get("from")).isNotNull();
            assertThat(routeJson.get("step")).isNotNull();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(5)
    void shouldGetComponents() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/components", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("IncludeCustomComponents", "false");

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isPositive();

            JsonNode componentJson = responseJson.get(0);
            List<String> fieldNames = StreamSupport.stream(
                    Spliterators.spliteratorUnknownSize(componentJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());
            assertThat(fieldNames).contains(
                    "scheme", "producerOnly", "kind", "deprecated", "groupId",
                    "description", "browsable", "label", "supportLevel", "title",
                    "remote", "version", "javaType", "async", "firstVersion",
                    "lenientProperties", "name", "syntax", "artifactId", "api",
                    "consumerOnly", "extendsScheme"
            );

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    @Order(6)
    @Tag("NeedsSchedulerFlowInstalled")
    void shouldRemoveFlowConfiguration() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/remove", baseUrl, schedulerCamelContextProp.get(TestApplicationContext.CamelContextField.ID.name()));

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

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowDocumentationByComponentType() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/documentation/%s", baseUrl, "xslt");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();

            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());
            assertThat(fieldNames).contains("component", "componentProperties", "headers", "properties");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetDocumentationVersion() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/documentation/version", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isNotNull();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowSteps() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/list/steps", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();
            assertThat(responseJson.isArray()).isTrue();
            assertThat(responseJson.size()).isPositive();

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowOptionsByComponentType() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/options/%s", baseUrl, "xslt");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();

            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());
            assertThat(fieldNames).contains("component", "componentProperties", "headers", "properties");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowRoutes() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/routes", baseUrl);

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThat(response.body()).isEqualTo("{not available yet}");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowSchemaByComponentType() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/schema/%s", baseUrl, "xslt");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();

            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());
            assertThat(fieldNames).contains("component", "componentProperties", "headers", "properties");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetFlowStepByTemplateName() {
        try {
            // url
            String baseUrl = container.getBaseUrl();
            String url = String.format("%s/api/integration/flow/step/%s", baseUrl, "xslt-action");

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);

            // endpoint call
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", null, null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode responseJson = objectMapper.readTree(response.body());
            assertThat(responseJson).isNotNull();

            List<String> fieldNames = StreamSupport.stream(
                            Spliterators.spliteratorUnknownSize(responseJson.fieldNames(), 0), false)
                    .collect(Collectors.toList());
            assertThat(fieldNames).contains("apiVersion", "kind", "metadata", "spec");

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

}
