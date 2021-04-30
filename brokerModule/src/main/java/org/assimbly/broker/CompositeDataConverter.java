package org.assimbly.broker;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.*;
public class CompositeDataConverter {

        public static String convertToJSON(CompositeData[] messages) {

            if (messages == null) {
                return null;
            }

            JSONObject messagesAsJSON = new JSONObject();
            JSONArray messagesArray = new JSONArray();

            for(CompositeData message: messages){
                System.out.print("a message");
                JSONObject messageAsJSON = messageToJSON(message);
                messagesArray.put(messageAsJSON);
            }

            messagesAsJSON.put("messages",messagesArray);

            return messagesAsJSON.toString();

        }

        public static JSONObject messageToJSON(CompositeData compositeData){

            Set<String> keys = compositeData.getCompositeType().keySet();

            JSONObject message = new JSONObject();
            JSONObject headers = new JSONObject();

            for(String key : keys){
                Object value = compositeData.get(key);

                if (!(value instanceof TabularData)) {
                    if(key.equals("PropertiesText")){
                        Object PropertiesText = compositeData.get("PropertiesText");
                        if(PropertiesText instanceof String){
                            PropertiesText = ((String) PropertiesText).substring( 1, ((String)PropertiesText).length() - 1);
                            String[] properties = ((String) PropertiesText).split(",");
                            for(String property: properties){
                                String headerKey = property.split("=")[0];
                                String headerValue = property.split("=")[1];
                                headers.put(headerKey,headerValue);
                            }
                        }
                        message.put("headers",headers);
                    }else{
                        message.put(key,value);
                    }
                }
            }

            return message;

    }

}