package org.assimbly.dil.blocks.beans.enrich.xml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.util.helper.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class XmlEnrichStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        try
        {
            Document enriched = XmlHelper.newDocument("<Enriched/>");

            if (resource == null && original == null) {
                throw new Exception("Something went wrong fetching the input data.");
            }

            Document originalXml = getXml(original, "left");
            Document resourceXml = getXml(resource, "bottom");

            if(originalXml == null && resourceXml == null) {
                throw new Exception("Something went wrong parsing the XML inputs.");
            }

            if (originalXml == null) {
                enriched = XmlHelper.mergeIn(enriched, resourceXml);
                resource.getIn().setBody(XmlHelper.prettyPrint(enriched));
                return resource;
            }

            if (resourceXml == null) {
                enriched = XmlHelper.mergeIn(enriched, originalXml);
                original.getIn().setBody(XmlHelper.prettyPrint(enriched));
                return original;
            }

            /*
                This avoids creating a new root element "Enriched" when two enrich components are used after each other,
                otherwise you would get:

                <Enriched>
                    <Enriched>

                    </Enriched>
                </Enriched>
             */
            if(originalXml.getDocumentElement().getTagName().equals("Enriched")){
                enriched = originalXml;
                enriched = XmlHelper.mergeIn(enriched, resourceXml);
            }else{
                enriched = XmlHelper.mergeIn(enriched, originalXml);
                enriched = XmlHelper.mergeIn(enriched, resourceXml);
            }

            original.getIn().setBody(XmlHelper.prettyPrint(enriched));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return original;
    }

    private Document getXml(Exchange exchange, String route) {

        try {
            Document document = XmlHelper.newDocument(exchange.getIn().getBody(String.class));

            if (document == null) {
                log.warn("No valid XML returned by the " + route + " route to the Enrich component.");
            }

            return document;

        } catch (Exception e) {
            log.warn("Unable to get data from the " + route + " route to the Enrich component.");
        }

        return null;
    }
}
