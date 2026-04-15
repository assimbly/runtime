package org.assimbly.dil.transpiler.marshalling.core;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.model.EndpointDefinition;
import org.assimbly.dil.transpiler.model.EndpointType;
import org.assimbly.docconverter.DocConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;

import javax.xml.transform.*;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TreeMap;

public class Route {

    final static Logger log = LoggerFactory.getLogger(Route.class);
    private final Document doc;
    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    XPathFactory xf = new XPathFactoryImpl();

    private final List<EndpointDefinition> endpoints;
    private final String flowId;

    public Route(TreeMap<String, String> properties, XMLConfiguration conf, Document doc, List<EndpointDefinition> endpoints, String flowId) {
        this.properties = properties;
        this.conf = conf;
        this.doc = doc;
        this.endpoints = endpoints;
        this.flowId = flowId;
    }

    public TreeMap<String, String> setRoute(String type, String flowId, String stepId, String routeId) throws Exception {

        String route = createRoute(flowId, routeId);

        route = createDataFormat(route);

        properties.put(type + "." + stepId + ".route.id", flowId + "-" + routeId);
        properties.put(type + "." + stepId + ".route", route);

        return properties;

    }

    private String createRoute(String flowId, String routeId) throws Exception {

        Node node = getRoute(routeId);

        extractFromEndpoints(node);

        String routeAsString = DocConverter.convertNodeToString(node);

        routeAsString = StringUtils.replace(routeAsString,"id=\"" + routeId +"\"" ,"id=\"" + flowId + "-" + routeId +"\"");


        return routeAsString;

    }

    private String createDataFormat(String route) throws Exception {

        String dataFormatAsString = null;
        if (route.contains("<customDataFormat ref=\"csv")){

            String ref = StringUtils.substringBetween(route, "<customDataFormat ref=\"", "\"/>");

            Node node = evaluateNodeXpath("/dil/core/routeConfigurations/routeConfiguration/dataFormats/csv[@id='" + ref +"']");

            dataFormatAsString = DocConverter.convertNodeToString(node);
            if(dataFormatAsString!=null) {
                route = route.replaceAll("<customDataFormat ref=(.*)", dataFormatAsString);
            }else{
                log.warn("Route:\n\n" + route + "\n\n Contains custom csv dataformat, but csv dataFormat is null");
            }
        }

        if (route.contains("marshal ref=\"multipart")){
            route = route.replaceAll("<unmarshal ref=(.*)", "<unmarshal><mimeMultipart/></unmarshal>");
            route = route.replaceAll("<marshal ref=(.*)", "<marshal><mimeMultipart/></marshal>");
        }

        return route;

    }

    private Node evaluateNodeXpath(String xpath) throws TransformerException, XPathExpressionException {
        XPathExpression xp = xf.newXPath().compile(xpath);
        return (Node) xp.evaluate(doc, XPathConstants.NODE);
    }

    private Node getRoute(String routeId){

        NodeList flow = doc.getElementsByTagName("route");

        for (int i = 0; i < flow.getLength(); i++) {
            Node node = flow.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                String id = element.getAttribute("id");

                if (routeId.equals(id)) {
                    return node;
                }
            }
        }

        return null;
    }

    private void extractFromEndpoints(Node routeNode) {

        if (routeNode == null) return;

        NodeList children = routeNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);

            if (child instanceof Element) {
                Element el = (Element) child;

                if ("from".equals(el.getTagName())) {

                    String uri = el.getAttribute("uri");

                    if (uri != null && !uri.isEmpty()) {
                        registerEndpoint(uri);
                    }
                }

                // 🔁 still recurse (important for nested EIPs)
                extractFromEndpoints(child);
            }
        }
    }

    private void registerEndpoint(String uri) {

        if (uri == null) return;

        if (uri.startsWith("jetty:")) {
            String path = extractJettyPath(uri);
            endpoints.add(new EndpointDefinition(flowId, EndpointType.JETTY, path));
        }

        else if (uri.startsWith("as2:")) {
            String pattern = extractAs2Pattern(uri);
            if (pattern != null) {
                endpoints.add(new EndpointDefinition(flowId, EndpointType.AS2, pattern));
            }
        }
    }

    private String extractJettyPath(String uri) {
        if (uri == null) return null;

        // remove scheme + host
        String cleaned = uri.replaceFirst("^jetty:(https?|ssl)://[^/]+(:\\d+)?", "");

        // remove query params
        int idx = cleaned.indexOf("?");
        String path = (idx != -1) ? cleaned.substring(0, idx) : cleaned;

        // normalize
        if (path == null || path.isEmpty()) {
            path = "/"; // root endpoint
        }

        // remove trailing slash (except root)
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path.toLowerCase();
    }

    private String extractAs2Pattern(String uri) {
        if (uri == null) return null;

        String[] parts = uri.split("\\?");
        if (parts.length < 2) return null;

        String query = parts[1];

        for (String param : query.split("&")) {
            if (param.startsWith("requestUriPattern=")) {
                String value = param.substring("requestUriPattern=".length());
                return value.toLowerCase();
            }
        }

        return null;
    }


}