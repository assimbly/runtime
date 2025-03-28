package org.assimbly.integrationrest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

public class Utils {

    private static final Logger log = LoggerFactory.getLogger(Utils.class);

    public static boolean isValidDate(String dateStr, String format) {
        try {
            // adjust milliseconds dynamically if needed
            String normalizedDateStr = normalizeMilliseconds(dateStr, format);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            formatter.parse(normalizedDateStr);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static String normalizeMilliseconds(String dateStr, String format) {
        // only modify if the format expects milliseconds
        if (format.contains("SSS")) {
            // 1-digit milliseconds -> add two trailing zeros
            dateStr = dateStr.replaceAll("(\\.\\d)(?!\\d)", "$100");
            // 2-digit milliseconds -> add a trailing zero
            dateStr = dateStr.replaceAll("(\\.\\d{2})(?!\\d)", "$10");
        }
        return dateStr;
    }

    public static String getNowDate(String format) {
        return Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern(format));
    }

    public static String readFileAsStringFromResources(String fileName) throws IOException {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(Objects.requireNonNull(classLoader.getResource(fileName)).toURI());
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error(String.format("Error to load %s file from resources", fileName), e);
            return null;
        }
    }

    public static byte[] readFileAsBytesFromResources(String fileName) throws IOException {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(Objects.requireNonNull(classLoader.getResource(fileName)).toURI());
            return Files.readAllBytes(path);
        } catch (Exception e) {
            log.error(String.format("Error to load %s file from resources", fileName), e);
            return null;
        }
    }

    public static String extractRouteFromXmlByRouteId(String xml, String routeId) throws Exception {

        // create a DocumentBuilderFactory and set up a DocumentBuilder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        // parse the string content into a Document
        InputSource inputSource = new InputSource(new StringReader(xml));
        Document document = builder.parse(inputSource);

        // create XPath object
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // xpath expression to find the route with the specific id
        String expression = "//route[@id='" + routeId + "']";

        // execute XPath to find the route element
        NodeList routeNodes = (NodeList) xpath.evaluate(expression, document, XPathConstants.NODESET);

        // check if the route is found
        if (routeNodes.getLength() > 0) {
            // get the first matching route node (you can change this to handle multiple matches if needed)
            Node routeNode = routeNodes.item(0);

            // convert the node to a string (XML)
            return getStringFromNode(routeNode);
        }
        return null;
    }

    private static String getStringFromNode(Node node) throws Exception {
        // create a transformer to convert the node to a string
        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // format the output for better readability
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // prevent the XML declaration from being included in the output
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        // convert the node to a string
        transformer.transform(new DOMSource(node), new StreamResult(writer));
        return writer.toString();
    }
}
