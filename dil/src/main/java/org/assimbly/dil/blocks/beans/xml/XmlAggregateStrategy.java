package org.assimbly.dil.blocks.beans.xml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.assimbly.util.helper.XmlHelper;
import org.w3c.dom.Document;


public class XmlAggregateStrategy implements AggregationStrategy {

    private final static Logger logger = Logger.getLogger(XmlAggregateStrategy.class);

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

            if(originalXml == null && resourceXml == null)
                throw new Exception("Something went wrong parsing the XML inputs.");

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
            logger.error(e.getMessage(), e);
        }
        return original;
    }

    private Document getXml(Exchange exchange) {

        try {
            Document document = XmlHelper.newDocument(exchange.getIn().getBody(String.class));

            if (document == null)
                logger.warn("No valid XML returned by the route to the Aggregate component.");

            return document;

        } catch (Exception e) {

            if (logger.isDebugEnabled()) {
                logger.debug("Unable to get data from the route to the Aggregate component.");
            }
        }

        return null;
    }
}
