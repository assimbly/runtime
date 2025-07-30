package org.assimbly.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class TransformUtil {

    private static final Logger log = LoggerFactory.getLogger("org.assimbly.util.TransformUtil");

    public static String transformXML(String xml, InputStream xslFile) {

        String outputXML = null;
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();

            StreamSource sourcXsl = new StreamSource(xslFile);
            Transformer transformer = factory.newTransformer(sourcXsl);
            Source xmlStream = new StreamSource(new StringReader(xml));
            StringWriter writer = new StringWriter();

            Result result = new StreamResult(writer);

            transformer.transform(xmlStream, result);

            outputXML = writer.getBuffer().toString();

            writer.close();

        } catch (TransformerConfigurationException tce) {
            log.error("XSLT Transformation of XML failed due to a TransformerConfigurationException",tce);
        } catch (TransformerException te) {
            log.error("XSLT Transformation of XML failed due to a TransformerException",te);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputXML;
    }

    /**
     * Performs simultaneous search/replace of multiple strings.
     *
     * @param target        string to perform replacements on.
     * @param replacements  map where key represents value to search for, and value represents replacem
     * @param caseSensitive whether or not the search is case-sensitive.
     * @return replaced string
     */
    public static String replaceMultipleStrings(String target, Map<String, String> replacements, boolean caseSensitive) {
        if(target == null || target.isEmpty() || replacements == null || replacements.isEmpty())
            return target;

        //if we are doing case-insensitive replacements, we need to make the map case-insensitive--make a new map with all-lower-case keys
        if(!caseSensitive) {
            Map<String, String> altReplacements = HashMap.newHashMap(replacements.size());
            for(Map.Entry<String, String> entry : replacements.entrySet())
                altReplacements.put(entry.getKey().toLowerCase(), replacements.get(entry.getKey()));

            replacements = altReplacements;
        }

        StringBuilder patternString = new StringBuilder();
        if(!caseSensitive)
            patternString.append("(?i)");

        patternString.append('(');
        boolean first = true;
        for(String key : replacements.keySet()) {
            if(first)
                first = false;
            else
                patternString.append('|');

            patternString.append(Pattern.quote(key));
        }
        patternString.append(')');

        Pattern pattern = Pattern.compile(patternString.toString());
        Matcher matcher = pattern.matcher(target);

        StringBuilder res = new StringBuilder();
        while(matcher.find()) {
            String match = matcher.group(1);
            if(!caseSensitive)
                match = match.toLowerCase();
            matcher.appendReplacement(res, replacements.get(match));
        }
        matcher.appendTail(res);

        return res.toString();

    }
	
	public static String nodeToString(Node node) {

		StringWriter sw = new StringWriter();

		try {
		  Transformer t = TransformerFactory.newInstance().newTransformer();
		  t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		  t.setOutputProperty(OutputKeys.INDENT, "yes");
		  t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
            log.error("nodeToString Transformer Exception: {}", String.valueOf(te));
		}

		return sw.toString();

	}

}