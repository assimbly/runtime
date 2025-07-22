package org.assimbly.broker.converter;

import org.json.JSONObject;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import java.util.Set;

public final class CompositeDataConverter {

    private CompositeDataConverter() {
        throw new IllegalStateException("Utility class");
    }

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

                    if (key.equalsIgnoreCase("JMSMessageID") || key.equalsIgnoreCase("messageID")) {
                            message.put("messageid",value);
                    }else if(key.equalsIgnoreCase("JMSTimestamp")){
                        message.put("timestamp",value);
                    }

                    if(key.startsWith("JMS")){
                        jmsheaders.put(key,value);
                    }
                }
            }

            message.put("jmsHeaders",jmsheaders);

            return message;

    }

    public static JSONObject messageToJSON(CompositeData compositeData, boolean excludeBody) {
        JSONObject message = new JSONObject();
        JSONObject headers = new JSONObject();
        JSONObject jmsHeaders = new JSONObject();

        Set<String> keys = compositeData.getCompositeType().keySet();

        for (String key : keys) {
            Object value = compositeData.get(key);
            if (!(value instanceof TabularData)) {
                processMessageProperty(key, value, message, headers, jmsHeaders, excludeBody);
            }
        }

        message.put("headers", headers);
        message.put("jmsHeaders", jmsHeaders);
        return message;
    }

    private static void processMessageProperty(String key, Object value, JSONObject message,
                                               JSONObject headers, JSONObject jmsHeaders, boolean excludeBody) {
        if ("PropertiesText".equals(key)) {
            parsePropertiesText(value, headers, jmsHeaders);
        } else if (key.startsWith("JMS")) {
            handleJmsProperty(key, value, message, jmsHeaders);
        } else if ("messageID".equalsIgnoreCase(key)) {
            message.put("messageid", value);
        } else if ("Text".equalsIgnoreCase(key) && !excludeBody) {
            message.put("body", value);
        } else if (!"Text".equalsIgnoreCase(key)) {
            message.put(key, value);
        }
    }

    private static void parsePropertiesText(Object propertiesTextObject, JSONObject headers, JSONObject jmsHeaders) {
        if (!(propertiesTextObject instanceof String propertiesText)) return;

        String cleanedText = propertiesText.substring(1, propertiesText.length() - 1);
        String[] properties = cleanedText.split(", ");

        for (String property : properties) {
            if (!property.isEmpty()) {
                addPropertyToHeaders(property, headers, jmsHeaders);
            }
        }
    }

    private static void addPropertyToHeaders(String property, JSONObject headers, JSONObject jmsHeaders) {
        String headerKey;
        String headerValue;

        if (property.contains("=")) {
            int equalsIndex = property.indexOf('=');
            headerKey = property.substring(0, equalsIndex);
            headerValue = property.substring(equalsIndex + 1);
        } else {
            headerKey = "header";
            headerValue = property;
        }

        if (headerKey.startsWith("JMS")) {
            jmsHeaders.put(headerKey, headerValue);
        } else {
            headers.put(headerKey, headerValue);
        }

    }

    private static void handleJmsProperty(String key, Object value, JSONObject message, JSONObject jmsHeaders) {
        if ("JMSMessageID".equalsIgnoreCase(key)) {
            message.put("messageid", value);
        } else if ("JMSTimestamp".equalsIgnoreCase(key)) {
            message.put("timestamp", value);
        }

        jmsHeaders.put(key, value);
    }

}