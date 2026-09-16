package org.assimbly.dil.blocks.beans.enrich.override;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.util.exception.EnrichException;

import java.util.Map;

public class OverrideEnrichStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        if (original == null) {
            return resource;
        }

        boolean errorRoute = original.getProperty("Error-Route", false, boolean.class);
        boolean ignoreNullResource = original.getProperty("AssimblyAggregateNoExceptionOnNull", false, boolean.class);

        if (resource == null) {
            return handleMissingResource(original, errorRoute, ignoreNullResource);
        }

        // Nested enrich routes (e.g. sftpenrich) may have already caught the failure via DLC.
        // Re-raise on the enrich-router exchange so the HTTP consumer can format a 500 JSON body.
        Exception resourceException = resource.getException();
        if (resourceException == null) {
            resourceException = resource.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        }
        if (errorRoute && !ignoreNullResource && resourceException != null) {
            throw new EnrichException("Can't override body");
        }

        Object resourceBody = resource.getMessage().getBody();
        if (resourceBody == null) {
            return handleMissingResource(original, errorRoute, ignoreNullResource);
        }

        original.getIn().setBody(resourceBody);
        Map<String, Object> headers = resource.getIn().getHeaders();
        headers.putAll(original.getIn().getHeaders());
        original.getIn().setHeaders(headers);

        return original;
    }

    private Exchange handleMissingResource(Exchange original, boolean errorRoute, boolean ignoreNullResource) {
        if (errorRoute && !ignoreNullResource) {
            throw new EnrichException("Can't override body");
        }

        original.getIn().setBody(null, String.class);
        return original;
    }
}
