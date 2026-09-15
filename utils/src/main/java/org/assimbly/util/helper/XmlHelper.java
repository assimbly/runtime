package org.assimbly.util.helper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
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
import java.util.regex.Pattern;

public final class XmlHelper {

    private static final Logger log =
            LoggerFactory.getLogger("org.assimbly.util.helper.XmlHelper");

    private static final String INVALID_CHAR_REGEX = "[^A-Za-z0-9_.-]";
    private static final String INVALID_START_REGEX = "^([0-9.-]|(?i)xml).*";

    private static final Pattern INVALID_CHAR_PATTERN =
            Pattern.compile(INVALID_CHAR_REGEX);

    private static final Pattern INVALID_START_PATTERN =
            Pattern.compile(INVALID_START_REGEX);

    private XmlHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Creates a new empty XML document.
     */
    public static Document newDocument() {
        try {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.newDocument();
        } catch (ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     * Parses XML into a DOM document.
     * External entities and external DTDs are disabled to prevent XXE attacks.
     */
    public static Document newDocument(String xml) {
        if (xml == null) {
            return null;
        }

        try {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(
                    new InputSource(new StringReader(xml))
            );
        } catch (SAXException | IOException | ParserConfigurationException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public static List<Element> getChildrenByTagName(Element parent, String name) {
        List<Element> nodeList = new ArrayList<>();

        for (Node child = parent.getFirstChild();
             child != null;
             child = child.getNextSibling()) {

            if (child.getNodeType() == Node.ELEMENT_NODE
                    && name.equals(child.getNodeName())) {
                nodeList.add((Element) child);
            }
        }

        return nodeList;
    }

    public static void mergeIn(Document original, Document addition) {
        Node copy = original.importNode(addition.getFirstChild(), true);

        original.getFirstChild().appendChild(copy);

    }

    /**
     * Converts a DOM node to formatted XML.
     */
    public static String prettyPrint(Node doc) {
        if (doc == null) {
            return null;
        }

        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        try {
            TransformerFactory factory = createSecureTransformerFactory();
            Transformer transformer = factory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount",
                    "2"
            );

            transformer.transform(
                    new DOMSource(doc),
                    result
            );
        } catch (TransformerException e) {
            log.error(e.getMessage(), e);
        }

        return writer.toString();
    }

    /**
     * Parses and pretty-prints XML.
     */
    public static String prettyPrint(String xml) {
        if (xml == null) {
            return null;
        }

        Document doc = null;

        try {
            DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();

            doc = builder.parse(
                    new InputSource(new StringReader(xml))
            );
        } catch (ParserConfigurationException | SAXException | IOException e) {
            log.error(e.getMessage(), e);
        }

        return prettyPrint(doc);
    }

    /**
     * Parses and pretty-prints XML, allowing parsing exceptions to propagate.
     */
    public static String prettyPrintWithPossibleException(String xml) throws Exception {
        DocumentBuilderFactory factory = createSecureDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.parse(
                new InputSource(new StringReader(xml))
        );

        return prettyPrint(doc);
    }

    public static boolean hasInvalidXml(String input) {
        // Check for invalid characters.
        if (INVALID_CHAR_PATTERN.matcher(input).find()) {
            return true;
        }

        // Check for invalid starting patterns.
        return INVALID_START_PATTERN.matcher(input).matches();
    }

    /**
     * Creates a securely configured DocumentBuilderFactory.
     * Prevents XML External Entity (XXE) attacks and external DTD access.
     */
    private static DocumentBuilderFactory createSecureDocumentBuilderFactory()
            throws ParserConfigurationException {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        // Disallow DOCTYPE declarations completely.
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        // Disable external general entities.
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        // Disable external parameter entities.
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        // Do not expand entity references.
        factory.setExpandEntityReferences(false);

        // Disable external DTD access.
        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        // Disable external schema access.
        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        return factory;
    }

    /**
     * Creates a securely configured TransformerFactory.
     * Prevents external DTDs and stylesheets from being accessed.
     */
    private static TransformerFactory createSecureTransformerFactory()
            throws TransformerException {

        TransformerFactory factory = TransformerFactory.newInstance();

        // Enable secure XML processing.
        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        // Disable external DTD access.
        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        // Disable external stylesheet access.
        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
                ""
        );

        return factory;
    }
}