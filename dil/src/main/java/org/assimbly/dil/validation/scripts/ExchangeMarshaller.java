package org.assimbly.dil.validation.scripts;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.assimbly.dil.validation.beans.script.ExchangeDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ExchangeMarshaller {

    private ExchangeMarshaller() {
        throw new IllegalStateException("Utility class");
    }

    public static Exchange unmarshall(ExchangeDto exchangeDto) {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());

        if (exchangeDto.getBody() != null) {
            exchange.getIn().setBody(exchangeDto.getBody());
        }

        if (exchangeDto.getHeaders() != null) {
            for (Map.Entry<String, String> entry : exchangeDto.getHeaders().entrySet()) {
                exchange.getIn().setHeader(entry.getKey(), entry.getValue());
            }
        }

        if (exchangeDto.getProperties() != null) {
            for (Map.Entry<String, String> entry : exchangeDto.getProperties().entrySet()) {
                exchange.setProperty(entry.getKey(), entry.getValue());
            }
        }

        return exchange;
    }

    public static ExchangeDto marshall(Exchange exchange) {
        String body = null;
        if (exchange.getIn().getBody() != null) {
            body = exchange.getIn().getBody(String.class);
        }

        Map<String, String> headers = new ConcurrentHashMap<>();
        if (exchange.getIn().getHeaders() != null) {
            for (String key : exchange.getIn().getHeaders().keySet()) {
                headers.put(key, exchange.getIn().getHeader(key, String.class));
            }
        }

        Map<String, String> properties = new ConcurrentHashMap<>();
        if (exchange.getProperties() != null) {
            for (String key : exchange.getProperties().keySet()) {
                properties.put(key, exchange.getProperty(key, String.class));
            }
        }

        return new ExchangeDto(properties, headers, body);
    }
}
