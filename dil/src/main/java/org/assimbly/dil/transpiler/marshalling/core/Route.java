package org.assimbly.dil.transpiler.marshalling.core;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.docconverter.StringConverter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Route {

    static final Logger log = LoggerFactory.getLogger(Route.class);
    private static final Pattern JMS_URI = Pattern.compile("(\\buri=\")((?:jms|sjms|sjms2|activemq):[^\"]*)\"");
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

    // Adds the connection's ConnectionFactory to the JMS endpoints of a route that don't name one
    public static String addConnectionFactory(String route, String connectionId) {

        Matcher matcher = JMS_URI.matcher(route);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String uri = matcher.group(2);
            if (!uri.contains("connectionFactory=")) {
                uri = uri + (uri.contains("?") ? "&amp;" : "?") + "connectionFactory=#" + connectionId;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group(1) + uri + "\""));
        }

        matcher.appendTail(result);

        return result.toString();

    }

    private String createRoute(String flowId, String routeId) {

        Node node = getRoute(routeId);

        assert node != null;
        node = node.cloneNode(true);
        setLogNames((Element) node, flowId);
        String routeAsString = StringConverter.nodeToString(node);

        return routeAsString.replace("<route id=\"" + routeId + "\"", "<route id=\"" + flowId + "-" + routeId + "\" group=\"" + flowId + "\"");

    }

    private String createDataFormat(String route) throws Exception {

        String dataFormatAsString;
        if (route.contains("<customDataFormat ref=\"csv")){

            String ref = StringUtils.substringBetween(route, "<customDataFormat ref=\"", "\"/>");

            Node node = evaluateNodeXpath("/dil/core/routeConfigurations/routeConfiguration/dataFormats/csv[@id='" + ref +"']");

            dataFormatAsString = StringConverter.nodeToString(node);
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

    // Without a logName, Camel names the logger after the generated route resource and line (e_1a2b…:3), so use the flow's name
    private void setLogNames(Element route, String flowId) {

        String flowName = properties.get("flow.name");
        String logName = flowName == null || flowName.isBlank() ? flowId : flowName;

        NodeList logs = route.getElementsByTagName("log");

        for (int i = 0; i < logs.getLength(); i++) {
            Element log = (Element) logs.item(i);
            if (!log.hasAttribute("logName") && !log.hasAttribute("logger")) {
                log.setAttribute("logName", logName);
            }
        }

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