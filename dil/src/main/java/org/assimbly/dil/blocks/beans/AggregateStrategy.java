package org.assimbly.dil.blocks.beans;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.assimbly.dil.blocks.beans.xml.XmlAggregateStrategy;

public class AggregateStrategy implements AggregationStrategy {

    final static Logger logger = Logger.getLogger(AggregateStrategy.class);

    private AggregationStrategy AggregateStrategy;

    @Override
    public Exchange aggregate(Exchange firstExchange, Exchange newExchange) {

        String aggregateType = newExchange.getProperty("Aggregate-Type", String.class);

        if (firstExchange != null)
            aggregateType = firstExchange.getProperty("Aggregate-Type", String.class);

        switch(aggregateType) {
            case "xml":
            case "text/xml":
                AggregateStrategy = new XmlAggregateStrategy();
                break;
            case "json":
            case "application/json":
                AggregateStrategy = new JsonAggregateStrategy();
        }

        return AggregateStrategy.aggregate(firstExchange, newExchange);
    }

}
