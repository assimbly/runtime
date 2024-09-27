package org.assimbly.dil.blocks.beans.enrich.json;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.assimbly.aggregate.json.JsonAggregateStrategy;


public class JsonEnrichStrategy implements AggregationStrategy {

    final static Logger logger = Logger.getLogger(JsonAggregateStrategy.class);

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        JSONArray array = new JSONArray();

        if(resource == null) {
            return original;
        }else if (original == null || !(original.getIn().getBody(String.class) instanceof String)) {

            array = wrapArray(array,resource.getIn().getBody(String.class));
            resource.getIn().setBody(array.toString(2));

            return resource;
        }else{

            array = wrapArray(array,original.getIn().getBody(String.class));
            array = wrapArray(array, resource.getIn().getBody(String.class));

            original.getIn().setBody(array.toString(2));

            return original;

        }

    }

    private JSONArray wrapArray(JSONArray array, String json){
        if(json.substring(0, 1).equals("[")) {
            return array.put(new JSONArray(json));
        } else {
            return array.put(new JSONObject(json));
        }
    }

}
