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

import org.w3c.dom.Node;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.dom.DOMSource;


public final class TransformUtil {

    protected static Logger log = LoggerFactory.getLogger("org.assimbly.util.TransformUtil");

	public static String convertCamelToAssimblyFormat(String xml){
		
		//convert camel2 to camel3
		xml = camel2ToCamel3(xml);
	
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("transform-to-assimbly.xsl");

		//transform to Assimbly format
		xml = transformXML(xml,is);
	
		return xml;
	}
			
	private static String camel2ToCamel3(String input){
		
		Map<String, String> map = new HashMap<>();

		map.put("xmlns=\"http://camel.apache.org/schema/blueprint\"","");		
		map.put("consumer.bridgeErrorHandler","bridgeErrorHandler");
		map.put("headerName","name");
        map.put("propertyName","name");
        map.put("\"velocity:generate\"","\"velocity:generate?allowTemplateFromHeader=true\"");
        map.put("xslt:","xslt-saxon:");
        map.put("&amp;saxon=true","");
        map.put("?saxon=true\"","");
        map.put("?saxon=true&amp;","?");
        map.put("xml2excel","xmltoexcel");
        map.put("excel2xml","exceltoxml");
        map.put("csv2xml","csvtoxml");
        map.put("global-variables","globalvariables");
        map.put("<custom ref=\"csv-","<customDataFormat ref=\"csv-");
        map.put("quartz2:","quartz:");
       
		
		String output = replaceMultipleStrings(input, map, true);
		
		//you may uncheck the method below, because it maybe faster on large maps
		//replaceMultipleString2(input, map);
		
		return output;
		
	}


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
	
	public static String nodeToString(Node node) {

		StringWriter sw = new StringWriter();

		try {
		  Transformer t = TransformerFactory.newInstance().newTransformer();
		  t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		  t.setOutputProperty(OutputKeys.INDENT, "yes");
		  t.transform(new DOMSource(node), new StreamResult(sw));
		} catch (TransformerException te) {
		  log.error("NodeToString Transformer Exception: " + te);
		}

		return sw.toString();

	}
}