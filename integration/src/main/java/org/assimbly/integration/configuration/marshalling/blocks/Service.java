package org.assimbly.integration.configuration.marshalling.blocks;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assimbly.util.IntegrationUtil;

import java.util.List;
import java.util.TreeMap;

public class Service {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    private String serviceXPath;
    private String serviceId;

    public Service(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setService(String type, String stepId, String serviceId) throws ConfigurationException {

        this.serviceId = serviceId;

        serviceXPath = "integration/services/service[id='" + serviceId + "']/";

        List<String> serviceProporties = IntegrationUtil.getXMLParameters(conf, serviceXPath + "keys");

        if(!serviceProporties.isEmpty()){

            setId(type, stepId);

            setName(type, stepId);

            setType();

            setKeys(serviceProporties);

        }

        return properties;

    }

    private void setId(String type, String stepId){
        properties.put(type + "." + stepId + ".service.id", serviceId);
    }

    private void setName(String type, String stepId){
        String serviceName = conf.getString(serviceXPath + "name");

        if(serviceName!=null && !serviceName.isEmpty()) {
            properties.put(type + "." + stepId + ".service.name", serviceName);
            properties.put("service." + serviceId + ".name", serviceName);
        }

    }

    private void setType(){

        String serviceType = conf.getString(serviceXPath + "type");

        if(!serviceType.isEmpty()) {
            properties.put("service." + serviceId + ".type", serviceType);
        }else{
            properties.put("service." + serviceId + ".type", "unknown");
        }

    }

    private void setKeys(List<String> serviceProporties){

        for(String serviceProperty : serviceProporties){
            properties.put("service." + serviceId + "." + serviceProperty.substring(serviceXPath.length() + 5).toLowerCase(), conf.getString(serviceProperty));
        }
    }

}
