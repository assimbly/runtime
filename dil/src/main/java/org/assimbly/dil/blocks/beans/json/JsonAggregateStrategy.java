package org.assimbly.dil.blocks.beans.json;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Objects;

public class JsonAggregateStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        JSONArray array;

        if (oldExchange == null) {

            String body = newExchange.getIn().getBody(String.class);
            array = new JSONArray();

            // Wrap the first item immediately
            wrapInArray(array, body);

            newExchange.getIn().setBody(array.toString(2));
            newExchange.setProperty("hasBeenAggregated", true);

            return newExchange;
        }
        
        if(Boolean.TRUE.equals(oldExchange.getProperty("hasBeenAggregated", Boolean.class))) {
            array = new JSONArray(Objects.requireNonNull(oldExchange.getIn().getBody(String.class)));
        }else{
            array = wrapInArray(new JSONArray(), oldExchange.getIn().getBody(String.class));
        }

        array = wrapInArray(array, newExchange.getIn().getBody(String.class));

        oldExchange.setProperty("hasBeenAggregated", true);

        oldExchange.getIn().setBody(array.toString(2));

        return oldExchange;
    }

    private JSONArray wrapInArray(JSONArray array, String json) {
        if (json == null || json.isBlank()) {
            return array;
        }

        if (json.trim().charAt(0) == '[') {
            return array.put(new JSONArray(json));
        }

        return array.put(new JSONObject(json));
    }

}
