package org.assimbly.dil.blocks.beans.xml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.util.helper.XmlHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class XmlAggregateStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        try
        {
            Document aggregated = XmlHelper.newDocument("<Aggregated/>");

            if (resource == null && original == null) {
                throw new Exception("Something went wrong fetching the input data.");
            }

            Document originalXml = getXml(original),
                     resourceXml = getXml(resource);

            if(originalXml == null && resourceXml == null) {
                throw new Exception("Something went wrong parsing the XML inputs.");
            }

            if (originalXml == null) {
                aggregated = XmlHelper.mergeIn(aggregated, resourceXml);
                resource.getIn().setBody(XmlHelper.prettyPrint(aggregated));
                return resource;
            }

            if (resourceXml == null) {
                aggregated = XmlHelper.mergeIn(aggregated, originalXml);
                original.getIn().setBody(XmlHelper.prettyPrint(aggregated));
                return original;
            }
            /*
                This avoids creating a new root element "Aggregated" when two aggregate components are used after each other,
                otherwise you would get:

                <Aggregated>
                    <Aggregated>

                    </Aggregated>
                </Aggregated>
            */

            if(originalXml.getDocumentElement().getTagName().equals("Aggregated")){
                aggregated = originalXml;
                aggregated = XmlHelper.mergeIn(aggregated, resourceXml);
            }else{
                aggregated = XmlHelper.mergeIn(aggregated, originalXml);
                aggregated = XmlHelper.mergeIn(aggregated, resourceXml);
            }
            original.getIn().setBody(XmlHelper.prettyPrint(aggregated));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return original;
    }

    private Document getXml(Exchange exchange) {

        try {
            Document document = XmlHelper.newDocument(exchange.getIn().getBody(String.class));

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
}
