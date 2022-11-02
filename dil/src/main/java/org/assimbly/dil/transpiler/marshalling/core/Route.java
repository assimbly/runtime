package org.assimbly.dil.transpiler.marshalling.core;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.*;
import java.util.TreeMap;

public class Route {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;

    public Route(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRoute(String type, String flowId, String stepId, String routeId) throws Exception {

        String route = createRoute(flowId, routeId);

        route = createDataFormat(route);

        properties.put(type + "." + stepId + ".route.id", flowId + "-" + routeId);
        properties.put(type + "." + stepId + ".route", route);

        return properties;
    }

    private String createRoute(String flowId, String routeId) throws Exception {

        Node node = IntegrationUtil.getNode(conf,"/dil/core/routes/route[@id='" + routeId + "']");

        String routeAsString = DocConverter.convertNodeToString(node);

        routeAsString = StringUtils.replace(routeAsString,"id=\"" + routeId +"\"" ,"id=\"" + flowId + "-" + routeId +"\"");

        return routeAsString;

    }

    private String createDataFormat(String route) throws Exception {

        if (route.contains("<customDataFormat ref")){
            Node node = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration/dataFormats");

            String dataFormatAsString = DocConverter.convertNodeToString(node);
            dataFormatAsString = StringUtils.substringBetween(dataFormatAsString, "<dataFormats>", "</dataFormats");
            route = route.replaceAll("<customDataFormat ref=(.*)", dataFormatAsString);
        }

        return route;

    }


}