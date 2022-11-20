package org.assimbly.dil.blocks.beans.enrich.json;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.json.JSONArray;
import org.json.JSONObject;
import org.assimbly.dil.blocks.beans.json.JsonAggregateStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class JsonEnrichStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        if(resource == null) {
            return original;
        }

        JSONArray array;

        if (original == null) {
            array = wrapInArray(new JSONArray(), resource.getIn().getBody(String.class));
            resource.getIn().setBody(array.toString(2));

            return resource;
        }

        array = wrapInArray(new JSONArray(), original.getIn().getBody(String.class));
        array = wrapInArray(array, resource.getIn().getBody(String.class));

        original.getIn().setBody(array.toString(2));

        return original;
    }

    private JSONArray wrapInArray(JSONArray array, String json){
        if(json.substring(0, 1).equals("[")) {
            return new JSONArray(json);
        } else {
            return array.put(new JSONObject(json));
        }

    }
}
