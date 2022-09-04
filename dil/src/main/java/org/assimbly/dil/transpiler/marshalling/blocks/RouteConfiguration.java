package org.assimbly.dil.transpiler.marshalling.blocks;

import org.apache.commons.configuration2.XMLConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.*;
import java.util.TreeMap;

public class RouteConfiguration {

    private TreeMap<String, String> properties;
    private XMLConfiguration conf;

    public RouteConfiguration(TreeMap<String, String> properties, XMLConfiguration conf) {
        this.properties = properties;
        this.conf = conf;
    }

    public TreeMap<String, String> setRouteConfiguration(String type, String stepId, String routeConfigurationId) throws Exception {

        Node node = getNode("/dil/core/routeConfigurations/routeConfiguration[@id='" + routeConfigurationId + "']");

        String routeAsString = DocConverter.convertNodeToString(node);

        properties.put(type + "." + stepId + ".routeconfiguration.id", routeConfigurationId);
        properties.put(type + "." + stepId + ".routeconfiguration", routeAsString);

        return properties;
    }


    private Node getNode(String xpath) throws XPathExpressionException {

        Document doc = conf.getDocument();

        XPath xpathFactory = XPathFactory.newInstance().newXPath();
        XPathExpression expr = xpathFactory.compile(xpath);
        Node node = (Node) expr.evaluate(doc, XPathConstants.NODE);

        return node;

    }

}