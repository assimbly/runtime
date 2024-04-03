package org.assimbly.dil.blocks.beans.xml;

import ca.uhn.hl7v2.conf.spec.usecase.AbstractUseCaseComponent;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.assimbly.docconverter.DocConverter;
import org.assimbly.util.helper.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import static org.apache.camel.builder.Builder.simple;


public class XmlAggregateStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        try {

            boolean CamelSplitComplete = newExchange.getProperty("CamelSplitComplete",Boolean.class);

            String newXml = getXml(oldExchange, "old"),
                    splitXml = getXml(newExchange, "new");

            if(newXml == null && splitXml == null) {
                throw new Exception("XML Aggregate: Something went wrong parsing the XML inputs.");
            }else if(newXml==null){
                newExchange.getIn().setBody(splitXml);
                return newExchange;
            }else{
                newXml = newXml + splitXml;
            }

            if(CamelSplitComplete){
                newXml = format("<Aggregated>" + newXml + "</Aggregated>");
                oldExchange.getIn().setBody(newXml);
            }else{
                oldExchange.getIn().setBody(newXml);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return oldExchange;
    }

    private String getXml(Exchange exchange, String x) {

        try {
            return exchange.getIn().getBody(String.class);
        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unable to get data from the route to the Aggregate component.");
            }
        }

        return null;
    }

    public String format(String xml) {

        try {
            final InputSource src = new InputSource(new StringReader(xml));
            final Node document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(src).getDocumentElement();
            final Boolean keepDeclaration = Boolean.valueOf(xml.startsWith("<?xml"));

            //May need this: System.setProperty(DOMImplementationRegistry.PROPERTY,"com.sun.org.apache.xerces.internal.dom.DOMImplementationSourceImpl");


            final DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
            final DOMImplementationLS impl = (DOMImplementationLS) registry.getDOMImplementation("LS");
            final LSSerializer writer = impl.createLSSerializer();

            writer.getDomConfig().setParameter("format-pretty-print", Boolean.TRUE); // Set this to true if the output needs to be beautified.
            writer.getDomConfig().setParameter("xml-declaration", keepDeclaration); // Set this to true if the declaration is needed to be outputted.

            return writer.writeToString(document);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
