package org.assimbly.integrationrest;

import org.assimbly.integrationrest.config.IntegrationConfig;
import org.assimbly.integrationrest.event.FailureCollector;
import org.assimbly.integrationrest.utils.MockMvcRequestBuildersUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ValidationRuntime.class)
@ComponentScan(basePackageClasses = {
        IntegrationConfig.class,
        IntegrationRuntime.class,
        FailureCollector.class,
        SimpMessageSendingOperations.class
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ValidationRuntimeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldValidateCronWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/cron",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("expression", "0 0/5 * * * ?")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    void shouldValidateCronWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/cron",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("expression", "0 0/5 * * *")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("error").value("Cron Validation error: Unexpected end of expression."))
        ;
    }

    @Test
    void shouldValidateCertificateWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/certificate",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpsUrl", "https://authenticationtest.com/HTTPAuth/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("validationResultStatus").value("VALID"))
                .andExpect(jsonPath("message").isEmpty())
        ;
    }

    @Test
    void shouldValidateCertificateWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/certificate",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpsUrl", "https://authenticationtest2.com/HTTPAuth/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("validationResultStatus").value("UNKNOWN"))
                .andExpect(jsonPath("message").value("authenticationtest2.com"))
        ;
    }

    @Test
    void shouldValidateUrlWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/url",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpUrl", "https://integrationmadeeasy.com/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(status().isNoContent());
    }

    @Test
    void shouldValidateUrlWithError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/url",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                Map.of("httpUrl", "https://integrationmadeasy.com/")
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("error").value("Url is not reachable from the server!"))
        ;
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/expression",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonArray.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(status().isNoContent());
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/expression",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonArray.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].error").value(hasItem(matchesRegex(
                        ".*nested exception is groovy.lang.MissingPropertyException: No such property: testing.*"))))
        ;
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/ftp",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(status().isNoContent());
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/ftp",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("error").value("Cannot login into FTP Server!"))
        ;
    }

    @Test
    void shouldValidateRegexWithSuccess() throws Exception {
        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("expression", "(.*) (.*)"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/regex",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("message").value("2"))
        ;
    }

    @Test
    void shouldValidateRegexWithError() throws Exception {
        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("expression", "(.*) (.*"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/regex",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string("Unclosed group near index 8\n(.*) (.*"))
        ;
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/script",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("result").value("5"))
        ;
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

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/script",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("message").value(matchesRegex("Invalid groovy script.*")))
        ;
    }

    @Test
    void shouldValidateXsltWithSuccess() throws Exception {
        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("xsltUrl","https://www.w3schools.com/xml/cdcatalog.xsl"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/xslt",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", is(empty())))
        ;
    }

    @Test
    void shouldValidateXsltWithError() throws Exception {
        // build body in json format
        JSONObject bodyJsonObject = new JSONObject(Map.of("xsltUrl","https://www.w3schools.com/xml/cdcatalog.xml"));

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/xslt",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_JSON_VALUE,
                bodyJsonObject.toJSONString()
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[*].error")
                        .value(hasItem(matchesRegex("The supplied file does not appear to be a stylesheet.*"))))
        ;
    }

    @Test
    void shouldValidateUriWithSuccess() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildGetMockHttpServletRequestBuilder(
                String.format("/api/validation/%d/uri",1),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE, "Uri", "2342424"),
                null
        );

        ResultActions resultActions = this.mockMvc.perform(requestBuilder);

        resultActions.andExpect(status().isOk());
    }

}