package org.assimbly.integrationrest.utils;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FlowUtil {

    public static void installFlow(MockMvc mockMvc, String id, String camelContext) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/1/flow/%s/install", id),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                camelContext
        );
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        resultActions.andExpect(status().isOk());
    }

    public static void uninstallFlow(MockMvc mockMvc, String id, String camelContext) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildDeleteMockHttpServletRequestBuilder(
                String.format("/api/integration/%d/flow/%s/uninstall", 1, id),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null
        );
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        resultActions.andExpect(status().isOk());
    }

    public static void stopFlow(MockMvc mockMvc, String id, String camelContext) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuildersUtil.buildPostMockHttpServletRequestBuilder(
                String.format("/api/integration/1/flow/%s/stop", id),
                Map.of("Accept", MediaType.APPLICATION_JSON_VALUE),
                null,
                MediaType.APPLICATION_XML_VALUE,
                camelContext
        );
        ResultActions resultActions = mockMvc.perform(requestBuilder);
        resultActions.andExpect(status().isOk());
    }
}
