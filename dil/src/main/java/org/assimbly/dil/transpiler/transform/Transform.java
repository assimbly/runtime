package org.assimbly.dil.transpiler.transform;

import org.assimbly.util.TransformUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Transform {

    protected static Logger log = LoggerFactory.getLogger("org.assimbly.util.TransformUtil");

	public static String transformToDil(String xml){

		//convert camel2 to camel3
		xml = camel2ToCamel3(xml);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("transform-to-dil.xsl");

		//transform to DIL format
		xml = TransformUtil.transformXML(xml,is);

        System.out.println("The DIL format:\n\n" + xml);

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
        map.put("strategyRef","aggregationStrategy");
        map.put("quartz2:","quartz:");
        map.put("http4:","http:");
        map.put("https4:","https:");		
        map.put("sql-component:","sql-custom:");
        map.put("pdf2txt:","pdftotext:");
        map.put("form2xml:","formtoxml:");

		String output = TransformUtil.replaceMultipleStrings(input, map, true);
		
		//you may uncheck the method below, because it maybe faster on large maps
		//replaceMultipleString2(input, map);
		
		return output;
		
	}


}