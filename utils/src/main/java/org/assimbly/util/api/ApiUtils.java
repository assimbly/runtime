package org.assimbly.util.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
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
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.Objects;

public final class ApiUtils {

    private static final Logger log = LoggerFactory.getLogger(ApiUtils.class);

    private ApiUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static boolean isValidDate(String dateStr, String format) {
        try {
            // adjust milliseconds dynamically if needed
            String normalizedDateStr = normalizeMilliseconds(dateStr, format);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
            formatter.parse(normalizedDateStr);
            return true;
        } catch (DateTimeParseException _) {
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
        return Instant.now()
                .atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern(format));
    }

    public static String readFileAsStringFromResources(String fileName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(
                    Objects.requireNonNull(classLoader.getResource(fileName)).toURI()
            );

            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error to load {} file from resources", fileName, e);
            return null;
        }
    }

    public static byte[] readFileAsBytesFromResources(String fileName) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Path path = Path.of(
                    Objects.requireNonNull(classLoader.getResource(fileName)).toURI()
            );

            return Files.readAllBytes(path);
        } catch (Exception e) {
            log.error("Error to load {} file from resources", fileName, e);
            return new byte[0];
        }
    }

    public static String extractRouteFromXmlByRouteId(String xml, String routeId) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);

        // Prevent XXE and other external entity attacks.
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );
        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the string content into a Document.
        InputSource inputSource = new InputSource(new StringReader(xml));
        Document document = builder.parse(inputSource);

        // Create XPath object.
        XPathFactory xPathFactory = XPathFactory.newInstance();
        XPath xpath = xPathFactory.newXPath();

        // XPath expression to find the route with the specific id.
        String expression = "//route[@id='" + routeId + "']";

        // Execute XPath to find the route element.
        NodeList routeNodes = (NodeList) xpath.evaluate(
                expression,
                document,
                XPathConstants.NODESET
        );

        if (routeNodes.getLength() > 0) {
            Node routeNode = routeNodes.item(0);
            return getStringFromNode(routeNode);
        }

        return null;
    }

    private static String getStringFromNode(Node node) throws Exception {
        StringWriter writer = new StringWriter();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();

        // Prevent external DTDs and stylesheets from being accessed.
        transformerFactory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );
        transformerFactory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_STYLESHEET,
                ""
        );

        // Enable secure processing.
        transformerFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        Transformer transformer = transformerFactory.newTransformer();

        // Format the output for better readability.
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        // Prevent the XML declaration from being included in the output.
        transformer.setOutputProperty(
                OutputKeys.OMIT_XML_DECLARATION,
                "yes"
        );

        // Convert the node to a string.
        transformer.transform(
                new DOMSource(node),
                new StreamResult(writer)
        );

        return writer.toString();
    }

    public static String buildAuth(String email, String pwd) {
        String data = email + ":" + pwd;
        return Base64.getEncoder()
                .encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }
}