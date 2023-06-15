package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Node;

import java.util.TreeMap;

public class RouteConfiguration {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;

    public RouteConfiguration(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRouteConfiguration(String type, String stepId, String routeConfigurationId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");

        if(node!=null){

            String routeAsString = DocConverter.convertNodeToString(node);

            if (routeAsString.contains("<dataFormats>")){
                routeAsString = routeAsString.replaceAll("<dataFormats>((.|\\n)*)<\\/dataFormats>", "");
            }

            properties.put(type + "." + stepId + ".routeconfiguration.id", routeConfigurationId);
            properties.put(type + "." + stepId + ".routeconfiguration", routeAsString);

        }

        return properties;
    }

}