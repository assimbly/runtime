package org.assimbly.dil.blocks.beans;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;
import org.jspecify.annotations.NonNull;


public class AggregateStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String aggregateType = newExchange != null ? newExchange.getProperty("Aggregate-Type", "", String.class) : null;
        if (oldExchange != null) {
            aggregateType = oldExchange.getProperty("Aggregate-Type", "",String.class);
        }

        AggregationStrategy aggregationStrategy = getAggregationStrategy(aggregateType);

        return aggregationStrategy.aggregate(oldExchange, newExchange);
        
    }

    private static @NonNull AggregationStrategy getAggregationStrategy(String aggregateType) {
        AggregationStrategy aggregateStrategy;

        if ("xml".equals(aggregateType)
                || "text/xml".equals(aggregateType)
                || "application/xml".equals(aggregateType)) {
            aggregateStrategy = new XmlAggregateStrategy();
        } else if ("json".equals(aggregateType)
                || "application/json".equals(aggregateType)) {
            aggregateStrategy = new JsonAggregateStrategy();
        } else {
            throw new UnsupportedOperationException("Unknown aggregateType");
        }
        return aggregateStrategy;
    }
}