package org.assimbly.dil.transpiler.marshalling.core;

import net.sf.saxon.xpath.XPathFactoryImpl;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.assimbly.dil.transpiler.XMLFileConfiguration;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.IntegrationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.TreeMap;

public class Route {

    final static Logger log = LoggerFactory.getLogger(Route.class);
    private final Document doc;
    private TreeMap<String, String> properties;
    private XMLConfiguration conf;
    XPathFactory xf = new XPathFactoryImpl();

    public Route(TreeMap<String, String> properties, XMLConfiguration conf, Document doc) {
        this.properties = properties;
        this.conf = conf;
        this.doc = doc;
    }

    public TreeMap<String, String> setRoute(String type, String flowId, String stepId, String routeId) throws Exception {

        String route = createRoute(flowId, routeId);

        route = createDataFormat(route);

        properties.put(type + "." + stepId + ".route.id", flowId + "-" + routeId);
        properties.put(type + "." + stepId + ".route", route);

        return properties;
    }

    private String createRoute(String flowId, String routeId) throws Exception {

       //Node node = IntegrationUtil.getNode(conf,"/dil/core/routes/route[@id='" + routeId + "']");

        Node node = evaluateNodeXpath("/dil/core/routes/route[@id='" + routeId + "']");

        String routeAsString = DocConverter.convertNodeToString(node);

        if(routeAsString.contains("yamldsl")){
            routeAsString = StringUtils.substringBetween(routeAsString,"<yamldsl xmlns=\"http://camel.apache.org/schema/spring\">","</yamldsl>");
            routeAsString = StringUtils.replace(routeAsString,"id: " + routeId,"id: " + flowId + "-" + routeId);
        }else{
            routeAsString = StringUtils.replace(routeAsString,"id=\"" + routeId +"\"" ,"id=\"" + flowId + "-" + routeId +"\"");
        }

        return routeAsString;

    }

    private String createDataFormat(String route) throws Exception {

        String dataFormatAsString = null;
        if (route.contains("<customDataFormat ref=\"csv")){

            String ref = StringUtils.substringBetween(route, "<customDataFormat ref=\"", "\"/>");

            //Node node = IntegrationUtil.getNode(conf,"/dil/core/routeConfigurations/routeConfiguration/dataFormats/csv[@id='" + ref +"']");
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


}