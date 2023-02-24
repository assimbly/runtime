package org.assimbly.integrationrest.utils;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Map;

public class MockMvcRequestBuildersUtil {

    public static MockHttpServletRequestBuilder buildGetMockHttpServletRequestBuilder(
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

    public static MockHttpServletRequestBuilder buildPostMockHttpServletRequestBuilder(
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
