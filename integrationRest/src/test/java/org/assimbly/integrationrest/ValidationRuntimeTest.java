package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
import static org.hamcrest.Matchers.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.Map;

@SpringBootTest(classes = ValidationRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationConfig.class,
        IntegrationRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
class ValidationRuntimeTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldValidateCronWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/cron",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("expression", "0 0/5 * * * ?")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void shouldValidateCronWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/cron",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("expression", "0 0/5 * * *")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("error")
                        .value("Cron Validation error: Unexpected end of expression."));
    }

    @Test
    void shouldValidateCertificateWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/certificate",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpsUrl", "https://authenticationtest.com/HTTPAuth/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.
                        jsonPath("validationResultStatus").
                        value("VALID"))
                .andExpect(MockMvcResultMatchers.jsonPath("message").isEmpty());
    }

    @Test
    void shouldValidateCertificateWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/certificate",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpsUrl", "https://authenticationtest2.com/HTTPAuth/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("validationResultStatus")
                        .value("UNKNOWN"))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("message")
                        .value("authenticationtest2.com"));
    }

    @Test
    void shouldValidateUrlWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/url",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpUrl", "https://integrationmadeeasy.com/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void shouldValidateUrlWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/url",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpUrl", "https://integrationmadeasy.com/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("error")
                        .value("Url is not reachable from the server!"));
    }

    @Test
    void shouldValidateExpressionWithSuccess() throws Exception {

        // build body in json format
        JSONArray bodyJsonArray = new JSONArray();
        bodyJsonArray.add(new JSONObject(Map.of(
                "name", "testHeader",
                "expression", "def x = 5;",
                "expressionType", "groovy"
        )));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/expression",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonArray.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void shouldValidateExpressionWithError() throws Exception {

        // build body in json format
        JSONArray bodyJsonArray = new JSONArray();
        bodyJsonArray.add(new JSONObject(Map.of(
                "name", "testHeader",
                "expression", "testing",
                "expressionType", "groovy"
        )));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/expression",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonArray.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("$[*].error")
                        .value(hasItem(matchesRegex(
                                ".*nested exception is groovy.lang.MissingPropertyException: No such property: testing.*"
                        )))
                );
    }

    @Test
    void shouldValidateFtpWithSuccess() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of(
                "protocol", "ftp",
                "host", "ftp.dlptest.com",
                "port", "21",
                "user", "dlpuser",
                "pwd", "rNrKYTX9g7z3RgJRmxWuGHbeu",
                "pkf", "/tmp/",
                "pkfd", ""
        ));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/ftp",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(MockMvcResultMatchers.status().isNoContent());
    }

    @Test
    void shouldValidateFtpWithError() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of(
                "protocol", "ftp",
                "host", "ftp.dlptest.com",
                "port", "21",
                "user", "dlpuser",
                "pwd", "1234567890",
                "pkf", "/tmp/",
                "pkfd", ""
        ));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/ftp",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("error")
                        .value("Cannot login into FTP Server!"));
    }

    @Test
    void shouldValidateRegexWithSuccess() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("expression", "(.*) (.*)"));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/regex",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("message")
                        .value("2"));
    }

    @Test
    void shouldValidateRegexWithError() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("expression", "(.*) (.*"));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/regex",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("message")
                        .value("Unclosed group near index 8\n(.*) (.*"));
    }

    @Test
    void shouldValidateScriptWithSuccess() throws Exception {

        // build body in json format
        JSONObject exchangeJsonObject = new JSONObject(Map.of(
                "properties", new JSONObject(),
                "headers", new JSONObject(),
                "body", ""
        ));
        JSONObject scriptJsonObject = new JSONObject(Map.of(
                "language", "groovy",
                "script", "def x = 5;"
        ));
        JSONObject bodyJsonObject = new JSONObject(Map.of(
                "exchange", exchangeJsonObject,
                "script", scriptJsonObject
        ));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/script",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("result")
                        .value("5"));
    }

    @Test
    void shouldValidateScriptWithError() throws Exception {

        // build body in json format
        JSONObject exchangeJsonObject = new JSONObject(Map.of(
                "properties", new JSONObject(),
                "headers", new JSONObject(),
                "body", ""
        ));
        JSONObject scriptJsonObject = new JSONObject(Map.of(
                "language", "groovy",
                "script", "hello"
        ));
        JSONObject bodyJsonObject = new JSONObject(Map.of(
                "exchange", exchangeJsonObject,
                "script", scriptJsonObject
        ));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/script",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("message")
                        .value(matchesRegex("Invalid groovy script.*")));
    }

    @Test
    void shouldValidateXsltWithSuccess() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("xsltUrl","https://www.w3schools.com/xml/cdcatalog.xsl"));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/xslt",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.jsonPath("$", is(empty())));
    }

    @Test
    void shouldValidateXsltWithError() throws Exception {

        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("xsltUrl","https://www.w3schools.com/xml/cdcatalog.xml"));

        MockHttpServletRequestBuilder requestBuilder = buildPostMockHttpServletRequestBuilder(
                "/api/validation/1/xslt",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers
                        .jsonPath("$[*].error")
                        .value(hasItem(matchesRegex("The supplied file does not appear to be a stylesheet.*"))));
    }

    @Test
    void shouldValidateUriWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildGetMockHttpServletRequestBuilder(
                "/api/validation/1/uri",
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE, "Uri", "2342424"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(MockMvcResultMatchers.status().isOk());
    }


    private MockHttpServletRequestBuilder buildGetMockHttpServletRequestBuilder(
            String endpoint,
            Map<String,String> headers,
            Map<String,String> params
    ) {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.get(endpoint);
        if(headers!=null)
            headers.entrySet().stream().forEach(
                    entry -> mockHttpServletRequestBuilder.header(entry.getKey(), entry.getValue())
            );
        if(params!=null)
            params.entrySet().stream().forEach(
                    entry -> mockHttpServletRequestBuilder.param(entry.getKey(), entry.getValue())
            );
        return mockHttpServletRequestBuilder;
    }

    private MockHttpServletRequestBuilder buildPostMockHttpServletRequestBuilder(
            String endpoint,
            Map<String,String> headers,
            Map<String,String> params,
            String contentType,
            String contentBody
    ) {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders.post(endpoint);
        if(headers!=null)
            headers.entrySet().stream().forEach(
                    entry -> mockHttpServletRequestBuilder.header(entry.getKey(), entry.getValue())
            );
        if(params!=null)
            params.entrySet().stream().forEach(
                    entry -> mockHttpServletRequestBuilder.param(entry.getKey(), entry.getValue())
            );
        mockHttpServletRequestBuilder.contentType(contentType);
        mockHttpServletRequestBuilder.content(contentBody);
        return mockHttpServletRequestBuilder;
    }
}