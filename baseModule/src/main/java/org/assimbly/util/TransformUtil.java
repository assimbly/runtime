package org.assimbly.util;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;

public final class TransformUtil {

    private static Logger logger = LoggerFactory.getLogger("org.assimbly.util.TransformUtil");

    public static String transformXML(String xml, InputStream xslFile) {
        String outputXML = null;
        try {
            System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer(new StreamSource(xslFile));
            Source xmlStream = new StreamSource(new StringReader(xml));
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);
            transformer.transform(xmlStream, result);
            outputXML = writer.getBuffer().toString();
        } catch (TransformerConfigurationException tce) {
            tce.printStackTrace();
        } catch (TransformerException te) {
            te.printStackTrace();
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
        if(target == null || "".equals(target) || replacements == null || replacements.size() == 0)
            return target;

        //if we are doing case-insensitive replacements, we need to make the map case-insensitive--make a new map with all-lower-case keys
        if(!caseSensitive) {
            Map<String, String> altReplacements = new HashMap<String, String>(replacements.size());
            for(String key : replacements.keySet())
                altReplacements.put(key.toLowerCase(), replacements.get(key));

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

        StringBuffer res = new StringBuffer();
        while(matcher.find()) {
            String match = matcher.group(1);
            if(!caseSensitive)
                match = match.toLowerCase();
            matcher.appendReplacement(res, replacements.get(match));
        }
        matcher.appendTail(res);

        return res.toString();
    }

    private static String replaceMultipleStrings2(final String text, final Map<String, String> map ) {

		String[] keys = new String[map.size()];
		String[] values = new String[map.size()];
		int index = 0;
		for (Map.Entry<String, String> mapEntry : map.entrySet()) {
			keys[index] = mapEntry.getKey();
			values[index] = mapEntry.getValue();
			index++;
		}
        return StringUtils.replaceEach( text, keys, values );
    }
	
}