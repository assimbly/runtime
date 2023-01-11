package org.assimbly.dil.validation.beans.script;

import java.util.Map;

public class ExchangeDto {

    private Map<String, String> properties;
    private Map<String, String> headers;
    private String body;

    public ExchangeDto() {}

    public ExchangeDto(Map<String, String> properties, Map<String, String> headers, String body) {
        this.properties = properties;
        this.headers = headers;
        this.body = body;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @SuppressWarnings("unused")
    protected void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    @SuppressWarnings("unused")
    protected void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    @SuppressWarnings("unused")
    protected void setBody(String body) {
        this.body = body;
    }
}
