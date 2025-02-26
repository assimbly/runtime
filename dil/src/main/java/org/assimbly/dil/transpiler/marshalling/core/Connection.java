package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.assimbly.util.IntegrationUtil;

import java.util.List;
import java.util.TreeMap;

public class Connection {

    private final TreeMap<String, String> properties;
    private final XMLConfiguration conf;
    private String connectionXPath;
    private String connectionId;

    public Connection(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setConnection(String type, String stepId, String connectionId) throws ConfigurationException {

        this.connectionId = connectionId;

        connectionXPath = "core/connections/connection[id='" + connectionId + "']/";

        List<String> connectionProporties = IntegrationUtil.getXMLParameters(conf, connectionXPath + "keys");

        if(!connectionProporties.isEmpty()){

            setId(type, stepId);

            setName(type, stepId);

            setType();

            setKeys(connectionProporties);

        }

        return properties;

    }

    private void setId(String type, String stepId){
        properties.put(type + "." + stepId + ".connection.id", connectionId);
    }

    private void setName(String type, String stepId){
        String connectionName = conf.getString(connectionXPath + "name");

        if(connectionName!=null && !connectionName.isEmpty()) {
            properties.put(type + "." + stepId + ".connection.name", connectionName);
            properties.put("connection." + connectionId + ".name", connectionName);
        }

    }

    private void setType(){

        String connectionType = conf.getString(connectionXPath + "type");

        if(connectionType.isEmpty()) {
            properties.put("connection." + connectionId + ".type", "unknown");
        }else{
            properties.put("connection." + connectionId + ".type", connectionType);
        }

    }

    private void setKeys(List<String> connectionProporties){

        for(String connectionProperty : connectionProporties){
            properties.put("connection." + connectionId + "." + connectionProperty.substring(connectionXPath.length() + 5).toLowerCase(), conf.getString(connectionProperty));
        }
    }

}
