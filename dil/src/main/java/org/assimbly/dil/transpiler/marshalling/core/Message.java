package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.assimbly.util.IntegrationUtil;

import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

public class Message {

    private final TreeMap<String, String> properties;
    private final XMLConfiguration conf;
    private String messageId;

    public Message(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setMessage(String messageId) {

        this.messageId = messageId;

        setId();

        setName();

        setBody();

        setHeaders();

        return properties;
    }

    private void setId(){
        properties.put("message." + messageId + ".id", messageId);
    }

    private void setName(){
        String messageName = conf.getString("core/messages/message[id='" + messageId + "']/name");
        if(messageName != null && !messageName.isEmpty()) {
            properties.put("message." + messageId + ".name", messageName);
        }
    }

    private void setBody(){

        String bodycontentXPath = "core/messages/message[id='" + messageId + "'  or name='" + messageId + "']/body/content";
        String bodylanguageXPath = "core/messages/message[id='" + messageId + "'  or name='" + messageId + "']/body/language";

        String bodyContent = conf.getString(bodycontentXPath);
        if(bodyContent != null && !bodyContent.isEmpty()) {
            properties.put("message." + messageId + ".body.content", bodyContent);
        }

        String bodyLanguage = conf.getString(bodylanguageXPath);
        if(bodyLanguage != null && !bodyLanguage.isEmpty()) {
            properties.put("message." + messageId + ".body.language", bodyLanguage);
        }

    }

    private void setHeaders(){

        String headersXPath = "core/messages/message[id='" + messageId + "'  or name='" + messageId + "']/headers/header";

        List<HierarchicalConfiguration<ImmutableNode>> headers = conf.configurationsAt(headersXPath);

        for (HierarchicalConfiguration<ImmutableNode> header : headers) {

            String headerType = header.getString("type");
            String language   = header.getString("language");
            String name       = header.getString("name");
            String value      = header.getString("value");

            properties.put("message." + messageId + "." + headerType + "." + name, value);
            properties.put("message." + messageId + "." + headerType + "." + name + ".language", language);

        }

    }

}
