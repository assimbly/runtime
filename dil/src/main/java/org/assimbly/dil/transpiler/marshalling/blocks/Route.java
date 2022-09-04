package org.assimbly.dil.transpiler.marshalling.blocks;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
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

    public TreeMap<String, String> setRoute(String type, String stepId, String routeId) throws Exception {

        String route = createRoute(routeId);

        route = createDataFormat(route);

        properties.put(type + "." + stepId + ".route.id", routeId);
        properties.put(type + "." + stepId + ".route", route);

        return properties;
    }

    private String createRoute(String routeId) throws Exception {

        Node node = getNode("/dil/core/routes/route[@id='" + routeId + "']");

        String routeAsString = DocConverter.convertNodeToString(node);

        return routeAsString;

    }

    private String createDataFormat(String route) throws Exception {

        if (route.contains("<customDataFormat ref")){
            Node node = getNode("/dil/core/routeConfigurations/routeConfiguration/dataFormats");

            String dataFormatAsString = DocConverter.convertNodeToString(node);
            dataFormatAsString = StringUtils.substringBetween(dataFormatAsString, "<dataFormats>", "</dataFormats");
            route = route.replaceAll("<customDataFormat ref=(.*)", dataFormatAsString);
        }

        return route;

    }

    private Node getNode(String xpath) throws XPathExpressionException {

        Document doc = conf.getDocument();

        XPath xpathFactory = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpathFactory.compile(xpath);
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);

        return node;
    }

}