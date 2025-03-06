package org.assimbly.integrationrest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assimbly.integrationrest.testcontainers.AssimblyGatewayHeadlessContainer;
import org.assimbly.integrationrest.utils.CamelContextUtil;
import org.assimbly.integrationrest.utils.HttpUtil;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.http.MediaType;

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class IntegrationRuntimeTest {

    private Properties inboundHttpsCamelContextProp = CamelContextUtil.buildInboundHttpsExample();
    private Properties schedulerCamelContextProp = CamelContextUtil.buildSchedulerExample();

    @BeforeEach
    void setUp(TestInfo testInfo) {
        if (testInfo.getTestMethod().get().getName().contains("SchedulerFlow")) {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, schedulerCamelContextProp.get(CamelContextUtil.Field.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install scheduler flow
            HttpUtil.makeHttpCall(url, "POST", (String) schedulerCamelContextProp.get(CamelContextUtil.Field.camelContext.name()), null, headers);
        }
    }

    //////////////////////////////////////////
    // collector
    // TODO

    //////////////////////////////////////////
    // flow interaction

    @Test
    void shouldInstallFlow() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/install", baseUrl, inboundHttpsCamelContextProp.get(CamelContextUtil.Field.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - install flow
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "POST", (String) inboundHttpsCamelContextProp.get(CamelContextUtil.Field.camelContext.name()), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

        } catch (Exception e) {
            fail("Test failed due to unexpected exception: " + e.getMessage(), e);
        }
    }

    @Test
    void shouldGetSchedulerFlowInfo() {
        try {
            // url
            String baseUrl = AssimblyGatewayHeadlessContainer.getBaseUrl();
            String url = String.format("%s/api/integration/flow/%s/info", baseUrl, schedulerCamelContextProp.get(CamelContextUtil.Field.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow info
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", (String) schedulerCamelContextProp.get(CamelContextUtil.Field.camelContext.name()), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode expectedBodyJson = objectMapper.readTree("{\"flow\":{\"isRunning\":true,\"name\":\"67c740bc349ced00070004a9\",\"id\":\"67c740bc349ced00070004a9\",\"status\":\"started\"}}");
            assertThatJson(response.body()).whenIgnoringPaths("flow.uptime").isEqualTo(expectedBodyJson);

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
            String url = String.format("%s/api/integration/flow/%s/stats", baseUrl, inboundHttpsCamelContextProp.get(CamelContextUtil.Field.id.name()));

            // headers
            HashMap<String, String> headers = new HashMap();
            headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
            headers.put("charset", StandardCharsets.ISO_8859_1.displayName());
            headers.put("Content-type", MediaType.APPLICATION_XML_VALUE);

            // endpoint call - get flow stats
            HttpResponse<String> response = HttpUtil.makeHttpCall(url, "GET", (String) inboundHttpsCamelContextProp.get(CamelContextUtil.Field.camelContext.name()), null, headers);

            // asserts
            assertThat(response.statusCode()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.body()).isEqualTo("{\"flow\":{\"total\":0,\"pending\":0,\"id\":\"67921474ecaafe0007000000\",\"completed\":0,\"failed\":0}}");

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

}
