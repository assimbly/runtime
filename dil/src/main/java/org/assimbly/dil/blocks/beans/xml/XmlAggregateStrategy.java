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
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;


public class XmlAggregateStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        try {

            Document aggregated = DocConverter.convertStringToDoc("<Aggregated/>");

            Document originalXml = getXml(oldExchange),
                    resourceXml = getXml(newExchange);

            if(originalXml == null && resourceXml == null) {
                throw new Exception("XML Aggregate: Something went wrong parsing the XML inputs.");
            }else if(originalXml == null) {
                aggregated = mergeDoc(aggregated, resourceXml);
                newExchange.getIn().setBody(aggregated);
                return newExchange;
            }else if (resourceXml == null) {
                aggregated = mergeDoc(aggregated, originalXml);
            }else if(originalXml.getDocumentElement().getTagName().equals("Aggregated")){
                aggregated = mergeDoc(originalXml, resourceXml);
            }else{
                aggregated = mergeDoc(aggregated, originalXml);
                aggregated = mergeDoc(aggregated, resourceXml);
            }

            oldExchange.getIn().setBody(aggregated);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return oldExchange;
    }

    private Document getXml(Exchange exchange) {

        try {
            String xml = exchange.getIn().getBody(String.class);
            Document document = DocConverter.convertStringToDoc(xml);

            if (document == null) {
                log.warn("No valid XML returned by the route to the Aggregate component.");
            }

            return document;

        } catch (Exception e) {

            if (log.isDebugEnabled()) {
                log.debug("Unable to get data from the route to the Aggregate component.");
            }
        }

        return null;
    }

    public static Document mergeDoc(Document original, Document addition) {
        Node copy = original.importNode(addition.getFirstChild(), true);
        original.getFirstChild().appendChild(copy);
        return original;
    }



}
