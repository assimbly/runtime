package org.assimbly.integration.configuration.marshalling.blocks;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assimbly.util.IntegrationUtil;

import java.util.List;
import java.util.TreeMap;

public class Header {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    private String headerXPath;
    private String headerId;

    public Header(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setHeader(String type, String stepId, String headerId) throws ConfigurationException {

        this.headerId = headerId;

        headerXPath = "core/headers/header[id='" + headerId + "']/keys";

        List<String> headerProporties = IntegrationUtil.getXMLParameters(conf, headerXPath);

        if(!headerProporties.isEmpty()){

            setId(type, stepId);

            setName();

            setKeys(headerProporties);
        }

        return properties;
    }

    private void setId(String type, String stepId){
        properties.put(type + "." + stepId + ".header.id", headerId);
    }

    private void setName(){
        String headerName = conf.getString("core/headers/header[id='" + headerId + "']/name");
        if(!headerName.isEmpty()) {
            properties.put("header." + headerId + ".name", headerName);
        }
    }

    private void setKeys(List<String> headerProporties){

        for(String headerProperty : headerProporties){
            if(!headerProperty.endsWith("type")) {

                String headerKey = headerProperty.substring(headerXPath.length() + 1);
                String headerValue = conf.getProperty(headerProperty).toString();
                String headerType = conf.getString(headerProperty + "/@type");

                if(headerType==null){
                    headerType = conf.getString(headerProperty + "/type");
                }
                properties.put("header." + headerId + "." + headerType + "." + headerKey, headerValue);
            }
        }
    }

}
