package org.assimbly.broker.converter;

import org.json.JSONObject;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.*;
public class CompositeDataConverter {

        public static String convertToJSON(CompositeData[] messages, Integer numberOfMessages, boolean list) {

            if (messages == null) {
                return null;
            }

            if(numberOfMessages==null){
                numberOfMessages = messages.length;
            }else if (messages.length < numberOfMessages){
                numberOfMessages = messages.length;
            }


            JSONObject messagesAsJSON = new JSONObject();
            JSONObject messageAsJSON = new JSONObject();

            for(int i=0;i<numberOfMessages;i++){
                CompositeData message = messages[i];
                if(list) {
                    messageAsJSON.append("message", messageToJSONList(message));
                }else{
                    messageAsJSON.append("message", messageToJSON(message));
                }
            }

            messagesAsJSON.put("messages",messageAsJSON);

            return messagesAsJSON.toString();

        }

        public static JSONObject messageToJSONList(CompositeData compositeData){

            Set<String> keys = compositeData.getCompositeType().keySet();

            JSONObject message = new JSONObject();
            JSONObject headers = new JSONObject();

            for(String key : keys){
                Object value = compositeData.get(key);

                if (!(value instanceof TabularData)) {
                    switch (key) {
                        case "JMSPriority":
                            message.put("priority",value);
                            break;
                        case "JMSMessageID":
                            message.put("messageID",value);
                            break;
                        case "JMSDestination":
                            message.put("address",value);
                            break;
                        case "JMSExpiration":
                            message.put("expiration",value);
                            break;
                        case "JMSTimestamp":
                            message.put("timestamp",value);
                            break;
                        case "JMSDeliveryMode":
                            if(value.equals("PERSISTENT")){
                                message.put("durable","true");
                            }else{
                                message.put("durable","false");
                            }
                            break;
                    }
                }
            }

            return message;

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
                            String headerKey;
                            String headerValue;
                            if(property.contains("=")) {
                                headerKey = property.split("=")[0];
                                headerValue = property.split("=")[1];
                            }else{
                                headerKey = "header";
                                headerValue = property;
                            }
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