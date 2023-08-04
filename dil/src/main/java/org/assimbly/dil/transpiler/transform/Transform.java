package org.assimbly.dil.transpiler.transform;

import org.apache.commons.lang3.time.StopWatch;
import org.assimbly.util.TransformUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;


public final class Transform {

    final static Logger log = LoggerFactory.getLogger(Transform.class);

    public static String transformToDil(String xml){

		//convert camel2 to camel3
        String camel3Xml = camel2ToCamel3(xml);

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		InputStream is = classloader.getResourceAsStream("transform-to-dil.xsl");

        //transform to DIL format
		String dilXml = TransformUtil.transformXML(camel3Xml,is);

        log.info("The DIL format:\n\n" + dilXml);

        return dilXml;

	}
			
	private static String camel2ToCamel3(String input){
		
		Map<String, String> map = new HashMap<>();

		map.put("xmlns=\"http://camel.apache.org/schema/blueprint\"","");		
		map.put("consumer.bridgeErrorHandler","bridgeErrorHandler");
        map.put("consumer.delay","delay");
		map.put("headerName","name");
        map.put("propertyName","name");
        map.put("\"velocity:generate\"","\"velocity:generate?allowTemplateFromHeader=true\"");
        map.put("xslt:","xslt-saxon:");
        map.put("jetty:http:","jetty-nossl:http:");
        map.put("log:nl.kabisa.flux//","log:org.assimbly.runtime");
        map.put("&amp;saxon=true","");
        map.put("?saxon=true\"","");
        map.put("?saxon=true&amp;","?");
        map.put("xml2excel","xmltoexcel");
        map.put("excel2xml","exceltoxml");
        map.put("csv2xml","csvtoxml");
        map.put("global-variables","tenantvariables");
        map.put("tenant-variables","tenantvariables");
        map.put("<custom ref=\"csv-","<customDataFormat ref=\"csv-");
        map.put("strategyRef","aggregationStrategy");
        map.put("executorServiceRef","executorService");
        map.put("quartz2:","quartz:");
        map.put("http4:","http:");
        map.put("https4:","https:");		
        map.put("sql-component:","sql-custom:");
        map.put("pdf2txt:","pdftotext:");
        map.put("form2xml:","formtoxml:");
        map.put("univocity-csv","univocityCsv");
        map.put("univocity-header","univocityHeader");
        map.put("checkedZipFileDataFormat:unmarshal?usingIterator=true","zipFile:unmarshal");
        map.put("ref=\"checkedZipFileDataFormat\"","ref=\"zipFileDataFormat\"");
        map.put("file://tenants","file:///data/.assimbly/tenants");



        String output = TransformUtil.replaceMultipleStrings(input, map, true);

		return output;
		
	}


}