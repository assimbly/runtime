package org.assimbly.dil.blocks.beans.enrich.override;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.util.exception.EnrichException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class OverrideEnrichStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        Exchange result;

        if (original == null) {
            result = resource;
        } else if (resource == null) {
            boolean errorRoute = original.getProperty("Error-Route", boolean.class);
            boolean ignoreNullResource = original.getProperty("AssimblyAggregateNoExceptionOnNull", boolean.class);

            if (errorRoute && !ignoreNullResource) {
                throw new EnrichException("Can't override body - nothing in the bottom route");
            }

            original.getIn().setBody(null);
            result = original;
        } else {
            original.getIn().setBody(resource.getIn().getBody());
            Map<String, Object> headers = resource.getIn().getHeaders();
            headers.putAll(original.getIn().getHeaders());
            original.getIn().setHeaders(headers);

            result = original;
        }

        return result;
    }
}
