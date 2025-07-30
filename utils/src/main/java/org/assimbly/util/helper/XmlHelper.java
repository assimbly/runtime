package org.assimbly.util.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlHelper {

    private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.helper.XmlHelper");

    private static final String INVALID_CHAR_REGEX = "[^A-Za-z0-9_.-]";

    private static final String INVALID_START_REGEX = "^([0-9.-]|(?i)xml).*";

    public static Document newDocument(){
        DocumentBuilderFactory icFactory;
        DocumentBuilder icBuilder;

        try {
            icFactory = DocumentBuilderFactory.newInstance();
            icBuilder = icFactory.newDocumentBuilder();

            return icBuilder.newDocument();
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static Document newDocument(String xml){
        if(xml == null) {
            return null;
        }

        DocumentBuilderFactory icFactory;
        DocumentBuilder icBuilder;

        try {
            icFactory = DocumentBuilderFactory.newInstance();
            icBuilder = icFactory.newDocumentBuilder();

            return icBuilder.parse(
                    new InputSource(new StringReader(xml))
            );
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static List<Element> getChildrenByTagName(Element parent, String name) {
        List<Element> nodeList = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE &&
                    name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }

        return nodeList;
    }


    public static Document mergeIn(Document original, Document addition) {
        Node copy = original.importNode(addition.getFirstChild(), true);

        original.getFirstChild().appendChild(copy);

        return original;
    }

    public static String prettyPrint(Node doc) {
        Transformer transformer;
        StreamResult result = new StreamResult(new StringWriter());

        try {
            transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            transformer.transform(new DOMSource(doc), result);
        } catch (TransformerException e) {
            log.error(e.getMessage(), e);
        }

        return result.getWriter().toString();
    }

    public static String prettyPrint(String xml){
        Document doc = null;

        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            doc = db.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }

        return prettyPrint(doc);
    }

    public static String prettyPrintWithPossibleException(String xml) throws Exception {
        Document doc;
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        doc = db.parse(new InputSource(new StringReader(xml)));

        return prettyPrint(doc);
    }

    public static boolean hasInvalidXml(String input) {
        // Check for invalid characters
        if (Pattern.compile(INVALID_CHAR_REGEX).matcher(input).find()) {
            return true;
        }
        // Check for invalid starting patterns
        return Pattern.compile(INVALID_START_REGEX).matcher(input).matches();
    }

    public static String fixInvalidXml(String input) {
        String result = input;

        result = result.replaceAll(INVALID_CHAR_REGEX, "");

        Pattern startPattern = Pattern.compile(INVALID_START_REGEX);
        Matcher startMatcher = startPattern.matcher(result);

        if (startMatcher.matches() || result.isEmpty()) { // If it starts with an invalid pattern or is now empty
            if (result.isEmpty()) {
                result = "element-" + input; // Use original input to ensure some content
            } else {
                result = "element-" + result;
            }
        }

        return result;
    }
}
