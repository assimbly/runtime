package org.assimbly.dil.transpiler.marshalling.core;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.TreeMap;

public class Route {

    static final Logger log = LoggerFactory.getLogger(Route.class);
    private final Document doc;
    private final TreeMap<String, String> properties;
    XPathFactory xf = new XPathFactoryImpl();

    public Route(TreeMap<String, String> properties, Document doc) {
        this.properties = properties;
        this.doc = doc;
    }

    public TreeMap<String, String> setRoute(String type, String flowId, String stepId, String routeId) throws Exception {

        String route = createRoute(flowId, routeId);

        route = createDataFormat(route);

        properties.put(type + "." + stepId + ".route.id", flowId + "-" + routeId);
        properties.put(type + "." + stepId + ".route", route);

        return properties;

    }

    private String createRoute(String flowId, String routeId) {

        Node node = getRoute(routeId);

        assert node != null;
        String routeAsString = DocConverter.convertNodeToString(node);

        routeAsString = StringUtils.replace(routeAsString,"id=\"" + routeId +"\"" ,"id=\"" + flowId + "-" + routeId +"\"");
        routeAsString = StringUtils.replace(routeAsString,"id=\"" + routeId +"\"" ,"id=\"" + flowId + "-" + routeId +"\"");



        return routeAsString;

    }

    private String createDataFormat(String route) throws Exception {

        String dataFormatAsString;
        if (route.contains("<customDataFormat ref=\"csv")){

            String ref = StringUtils.substringBetween(route, "<customDataFormat ref=\"", "\"/>");

            Node node = evaluateNodeXpath("/dil/core/routeConfigurations/routeConfiguration/dataFormats/csv[@id='" + ref +"']");

            dataFormatAsString = DocConverter.convertNodeToString(node);
            if(dataFormatAsString!=null) {
                route = route.replaceAll("<customDataFormat ref=(.*)", dataFormatAsString);
            }else{
                log.warn("Route:\n\n{}\n\n Contains custom csv dataformat, but csv dataFormat is null", route);
            }
        }

        if (route.contains("marshal ref=\"multipart")){
            route = route.replaceAll("<unmarshal ref=(.*)", "<unmarshal><mimeMultipart/></unmarshal>");
            route = route.replaceAll("<marshal ref=(.*)", "<marshal><mimeMultipart/></marshal>");
        }

        return route;

    }

    private Node evaluateNodeXpath(String xpath) throws XPathExpressionException {
        XPathExpression xp = xf.newXPath().compile(xpath);
        return (Node) xp.evaluate(doc, XPathConstants.NODE);
    }

    private Node getRoute(String routeId){

        NodeList flow = doc.getElementsByTagName("route");

        for (int i = 0; i < flow.getLength(); i++) {
            Node node = flow.item(i);
            if (node instanceof Element element) {
                String id = element.getAttribute("id");

                if (routeId.equals(id)) {
                    return node;
                }
            }
        }

        return null;
    }

}