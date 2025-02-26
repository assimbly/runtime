package org.assimbly.broker.converter;

import org.json.JSONObject;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Set;
public class CompositeDataConverter {

        public static String convertToJSON(CompositeData[] messages, Integer numberOfMessages, boolean list, boolean excludeBody) {

            if (messages == null) {
                return null;
            }

            int totalMessages;

            if(numberOfMessages==null){
                totalMessages = messages.length;
            }else if (messages.length < numberOfMessages){
                totalMessages = messages.length;
            }else{
                totalMessages = numberOfMessages;
            }

            JSONObject messagesAsJSON = new JSONObject();
            JSONObject messageAsJSON = new JSONObject();

            for(int i=0;i<totalMessages;i++){
                CompositeData message = messages[i];
                if(list) {
                    messageAsJSON.append("message", messageToJSONList(message));
                }else{
                    messageAsJSON.append("message", messageToJSON(message, excludeBody));
                }
            }

            messagesAsJSON.put("messages",messageAsJSON);

            return messagesAsJSON.toString();

        }

        public static JSONObject messageToJSONList(CompositeData compositeData){

            Set<String> keys = compositeData.getCompositeType().keySet();

            JSONObject message = new JSONObject();
            JSONObject jmsheaders = new JSONObject();

            for(String key : keys){

                Object value = compositeData.get(key);

                if (!(value instanceof TabularData)) {

                    switch (key) {
                        case "JMSMessageID":
                            message.put("messageid",value);
                            break;
                        case "messageID":
                            message.put("messageid",value);
                            break;
                        case "JMSTimestamp":
                            message.put("timestamp",value);
                            break;
                    }

                    if(key.startsWith("JMS")){
                        jmsheaders.put(key,value);
                    }
                }
            }

            message.put("jmsHeaders",jmsheaders);

            return message;

    }

    public static JSONObject messageToJSON(CompositeData compositeData, boolean excludeBody){

        Set<String> keys = compositeData.getCompositeType().keySet();

        JSONObject message = new JSONObject();
        JSONObject headers = new JSONObject();
        JSONObject jmsheaders = new JSONObject();

        for(String key : keys){
            Object value = compositeData.get(key);

            if (!(value instanceof TabularData)) {

                if(key.equals("PropertiesText")){
                    Object propertiesText = compositeData.get("PropertiesText");
                    if(propertiesText instanceof String){

                        propertiesText = ((String) propertiesText).substring( 1, ((String)propertiesText).length() - 1);

                        String[] properties = ((String) propertiesText).split(", ");
                        for(String property: properties){

                            String headerKey;
                            String headerValue;

                            if(property.contains("=")) {
                                headerKey = property.substring(0, property.indexOf('='));
                                headerValue = property.substring(property.indexOf('=') + 1);
                                if(headerKey.startsWith("JMS")){
                                    jmsheaders.put(headerKey,headerValue);
                                }else{
                                    headers.put(headerKey,headerValue);
                                }
                            }else if(!property.isEmpty()){
                                headerKey = "header";
                                headerValue = property;
                                headers.put(headerKey,headerValue);
                            }
                        }
                    }
                    message.put("headers",headers);
                }else if(key.startsWith("JMS")){

                    if(key.equalsIgnoreCase("JMSMessageID")) {
                        message.put("messageid", value);
                    } else if(key.equalsIgnoreCase("JMSTimestamp")) {
                        message.put("timestamp",value);
                    }

                    jmsheaders.put(key,value);

                }else if(key.equalsIgnoreCase("messageID")) {
                        message.put("messageid", value);
                }else if(key.equalsIgnoreCase("Text")) {
                    if(!excludeBody) {
                        message.put("body", value);
                    }
                }else{
                    message.put(key,value);
                }
            }
        }

        message.put("jmsHeaders",jmsheaders);

        return message;

    }

}