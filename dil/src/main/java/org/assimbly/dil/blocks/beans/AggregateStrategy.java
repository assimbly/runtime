package org.assimbly.dil.blocks.beans;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AggregateStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private AggregationStrategy aggregateStrategy;

    @Override
    public Exchange aggregate(Exchange firstExchange, Exchange newExchange) {

        String aggregateType = newExchange.getProperty("Aggregate-Type", String.class);

        if (firstExchange != null) {
            aggregateType = firstExchange.getProperty("Aggregate-Type", String.class);
        }

        switch(aggregateType) {
            case "xml":
            case "text/xml":
                aggregateStrategy = new XmlAggregateStrategy();
                break;
            case "json":
            case "application/json":
                aggregateStrategy = new JsonAggregateStrategy();
            default:
                throw new UnsupportedOperationException("Unknown aggregateType");
        }

        return aggregateStrategy.aggregate(firstExchange, newExchange);
    }

}
