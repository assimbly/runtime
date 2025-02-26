package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Node;

import java.util.TreeMap;

public class RouteConfiguration {

    private final TreeMap<String, String> properties;
    private final XMLConfiguration conf;

    public RouteConfiguration(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRouteConfiguration(String type, String stepId, String routeConfigurationId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");

        if(node!=null){

            String routeConfigurationAsString = DocConverter.convertNodeToString(node);

            if (routeConfigurationAsString.contains("<dataFormats>")){
                routeConfigurationAsString = StringUtils.substringBefore(routeConfigurationAsString,"<dataFormats>") + StringUtils.substringAfter(routeConfigurationAsString,"</dataFormats>");
            }

            properties.put(type + "." + stepId + ".routeconfiguration.id", routeConfigurationId);
            properties.put(type + "." + stepId + ".routeconfiguration", routeConfigurationAsString);

        }

        return properties;
    }

}