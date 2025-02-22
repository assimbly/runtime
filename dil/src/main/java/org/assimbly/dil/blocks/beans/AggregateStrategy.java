package org.assimbly.dil.blocks.beans;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AggregateStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String aggregateType = newExchange.getProperty("Aggregate-Type", String.class);
        if (oldExchange != null) {
            aggregateType = oldExchange.getProperty("Aggregate-Type", String.class);
        }

        AggregationStrategy aggregateStrategy;

        switch(aggregateType) {
            case "xml":
            case "text/xml":
            case "application/xml":
                aggregateStrategy = new XmlAggregateStrategy();
                break;
            case "json":
            case "application/json":
                aggregateStrategy = new JsonAggregateStrategy();
                break;
            default:
                throw new UnsupportedOperationException("Unknown aggregateType");
        }
        return aggregateStrategy.aggregate(oldExchange, newExchange);
    }
}