package org.assimbly.dil.blocks.beans.enrich.json;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.TypeConversionException;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.assimbly.aggregate.json.JsonAggregateStrategy;

public class JsonEnrichStrategy implements AggregationStrategy {

    final static Logger logger = Logger.getLogger(JsonAggregateStrategy.class);

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        if(resource == null) {
            return original;
        }

        JSONArray array = new JSONArray();

        if (original == null) {

            String resourceBody = convertBodyToString(resource);

            array = wrapArray(array,resourceBody);
            resource.getIn().setBody(array.toString(2));

            return resource;

        }else{

            String originalBody = convertBodyToString(original);
            String resourceBody = convertBodyToString(resource);

            array = wrapArray(array, originalBody);
            array = wrapArray(array, resourceBody);

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

    private String convertBodyToString(Exchange exchange){

        Object body = exchange.getIn().getBody();

        if (body instanceof String) {
            return exchange.getIn().getBody(String.class);
        } else {
           try {
                // Convert Object to String using Camel's typeconverter
               return exchange.getContext().getTypeConverter().convertTo(String.class, body);
            } catch (TypeConversionException e) {
               logger.error("Failed to enrich message body of type: " + body.getClass().getName() + " | Error:" + e.getMessage());
               throw e;
            }
        }

    }

}
