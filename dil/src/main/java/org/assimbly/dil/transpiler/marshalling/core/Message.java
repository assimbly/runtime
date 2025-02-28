package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.assimbly.util.IntegrationUtil;

import java.util.List;
import java.util.TreeMap;

public class Message {

    private final TreeMap<String, String> properties;
    private final XMLConfiguration conf;
    private String headerXPath;
    private String messageId;

    public Message(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setHeader(String type, String stepId, String messageId) {

        this.messageId = messageId;

        headerXPath = "core/messages/message[id='" + messageId + "']/headers";

        List<String> headerProporties = IntegrationUtil.getXMLParameters(conf, headerXPath);

        if(!headerProporties.isEmpty()){

            setId(type, stepId);

            setName();

            setHeaders(headerProporties);
        }

        return properties;
    }

    private void setId(String type, String stepId){
        properties.put(type + "." + stepId + ".message.id", messageId);
    }

    private void setName(){
        String headerName = conf.getString("core/messages/message[id='" + messageId + "']/name");
        if(!headerName.isEmpty()) {
            properties.put("message." + messageId + ".name", headerName);
        }
    }

    private void setHeaders(List<String> headerProporties){

        for(String headerProperty : headerProporties){
            if(!headerProperty.endsWith("type")) {

                String key = headerProperty.substring(headerXPath.length() + 1);
                String value = conf.getProperty(headerProperty).toString();
                String type = conf.getString(headerProperty + "/@type");

                if(type==null){
                    type = conf.getString(headerProperty + "/type");
                }
                properties.put("message." + messageId + "." + type + "." + key, value);
            }
        }
    }

}
