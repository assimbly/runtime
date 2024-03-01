package org.assimbly.dil.transpiler.transform;

import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.s9api.*;
import org.assimbly.util.TransformUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.*;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class Transform {

    final static Logger log = LoggerFactory.getLogger(Transform.class);

    private XsltTransformer transformer;

    private Processor processor;

    public Transform() throws SaxonApiException {
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("transform-to-dil.xsl");
        processor = new Processor(false);
        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(is));
        transformer = executable.load();
    }

    public String transformToDil(String xml, String flowId) throws Exception {

        //convert camel2 to camel3
        String camel3Xml = camel2ToCamel3(xml, flowId);

        String dilXml = transformXML6(camel3Xml, transformer, processor);

        log.info("The DIL format:\n\n" + dilXml);

        return dilXml;

	}
			
	private String camel2ToCamel3(String input, String flowId){

        Map<String, String> map = new HashMap<>();

        map.put("xmlns=\"http://camel.apache.org/schema/blueprint\"","");
		map.put("consumer.bridgeErrorHandler","bridgeErrorHandler");
        map.put("consumer.delay","delay");
		map.put("headerName=","name=");
        map.put("propertyName=","name=");
        map.put("\"velocity:generate\"","\"velocity:generate?allowTemplateFromHeader=true\"");
        map.put("xslt:","xslt-saxon:");
        map.put("jetty:http:","jetty-nossl:http:");
        map.put("log:nl.kabisa.flux//","log:org.assimbly.runtime." + flowId);
        map.put("&amp;saxon=true","");
        map.put("?saxon=true\"","");
        map.put("?saxon=true&amp;","?");
        map.put("xml2excel","xmltoexcel");
        map.put("excel2xml","exceltoxml");
        map.put("csv2xml","csvtoxml");
        map.put("sandbox://javaScript","sandbox://js");
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
        map.put("google-drive:","googledrive:");
        map.put("univocity-csv","univocityCsv");
        map.put("<custom ref=\"zipFileDataFormat\"/>","<zipFile/>");
        map.put("exchange.getIn().hasAttachments","exchange.getIn(org.apache.camel.attachment.AttachmentMessage.class).hasAttachments");
        map.put("<simple>${exchange.getIn().hasAttachments}</simple>","<method beanType=\"org.assimbly.mail.component.mail.SplitAttachmentsExpression\" method=\"hasAttachments\"/>");
        map.put("<ref>splitAttachmentsExpression</ref>","<method beanType=\"org.assimbly.mail.component.mail.SplitAttachmentsExpression\"/>");
        map.put("file://tenants","file:///data/.assimbly/tenants");
        map.put("DovetailQueueName","AssimblyQueueName");
        map.put("DovetailQueueHasMessages","AssimblyQueueHasMessages");
        map.put("DovetailPendingMessagesCount","AssimblyPendingMessagesCount");

        String output = TransformUtil.replaceMultipleStrings(input, map, true);

        output = replaceUnmarshalCheckedZipFileDataFormat(output);

        return output;
		
	}

    private String replaceUnmarshalCheckedZipFileDataFormat(String xml) {
        return xml.replaceAll(
                "<unmarshal>([\\r\\n\\t\\s]*)<custom ref=\"checkedZipFileDataFormat\"\\/>([\\r\\n\\t\\s]*)<\\/unmarshal>",
                "<process ref=\"Unzip\"/>"
        );
    }


    public String transformXML2(String xml, InputStream is) {

        String outputXML = null;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance("net.sf.saxon.TransformerFactoryImpl", null);
            StreamSource sourcXsl = new StreamSource(is);
            try {
                TransformerImpl transformer = (TransformerImpl) transformerFactory.newTransformer(sourcXsl);

                StringWriter writer = new StringWriter();

                Result result = new StreamResult(writer);

                Source xmlStream = new StreamSource(new StringReader(xml));
                transformer.transform(xmlStream, result);

                outputXML = writer.getBuffer().toString();

                writer.close();

            } catch (TransformerConfigurationException e) {
                throw new RuntimeException(e);
            }

        } catch (TransformerException te) {
            te.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return outputXML;

    }

    private String transformXML4(String xml, InputStream is) throws Exception {

        String outputXML = null;

        SAXTransformerFactory stf = (SAXTransformerFactory)TransformerFactory.newInstance();

        Templates templates1 = stf.newTemplates(new StreamSource(is));

        TransformerHandler th1 = stf.newTransformerHandler(templates1);

        StringWriter writer = new StringWriter();
        Result result = new StreamResult(writer);
        Source xmlStream = new StreamSource(new StringReader(xml));

        th1.getTransformer().transform(xmlStream, result);

        outputXML = writer.getBuffer().toString();

        return outputXML;
    }

    public String transformXML5(String xmlString, InputStream is) throws SaxonApiException {

        Processor processor = new Processor(false);
        DocumentBuilder builder = processor.newDocumentBuilder();
        XdmNode sourceDocument = builder.build(new StreamSource(new StringReader(xmlString)));

        XsltCompiler compiler = processor.newXsltCompiler();
        XsltExecutable executable = compiler.compile(new StreamSource(is));
        XsltTransformer transformer = executable.load();

        transformer.setInitialContextNode(sourceDocument);

        Serializer out = processor.newSerializer();
        StringWriter stringWriter = new StringWriter();
        out.setOutputWriter(stringWriter);

        transformer.setDestination(out);
        transformer.transform();

        return stringWriter.toString();

    }



    public String transformXML6(String xmlString, XsltTransformer transformer, Processor processor ) throws SaxonApiException {

        DocumentBuilder builder = processor.newDocumentBuilder();

        XdmNode sourceDocument = builder.build(new StreamSource(new StringReader(xmlString)));

        transformer.setInitialContextNode(sourceDocument);

        Serializer out = processor.newSerializer();
        StringWriter stringWriter = new StringWriter();
        out.setOutputWriter(stringWriter);

        transformer.setDestination(out);
        transformer.transform();

        return stringWriter.toString();

    }

}
