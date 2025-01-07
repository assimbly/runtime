package org.assimbly.dil.blocks.beans.xml;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class XmlAggregateStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static String XML_DECLARATION_UTF_8 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static String AGGREGATE_INIT_TAG = "<Aggregated>";
    private static String AGGREGATE_END_TAG = "</Aggregated>";

    @Override
    public Exchange aggregate(Exchange newExchange, Exchange splitExchange) {

        try {
            String splitXml = getBody(splitExchange);
            String newXml = getBody(newExchange);

            boolean isSplitXmlNull = splitXml == null;
            boolean isNewXmlNull = newXml == null;

            if(isNewXmlNull && isSplitXmlNull) {
                throw new Exception("XML Aggregate: Inputs are empty.");
            }

            if(isNewXmlNull) {
                newXml = buildAggregateBody("", splitXml);
                splitExchange.getIn().setBody(newXml);
                return splitExchange;
            }

            newXml = buildAggregateBody(newXml, splitXml);

            newExchange.getIn().setBody(newXml);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return newExchange;
    }

    private String buildAggregateBody(String newXml, String splitXml) {
        String result = "";

        if(containsXmlDeclaration(splitXml)) {
            // removes xml declaration from splitXml
            int declarationEndPos = splitXml.indexOf("?>");
            splitXml = splitXml.substring(declarationEndPos + 2);
        }

        if(newXml.endsWith(AGGREGATE_END_TAG)) {
            result = StringUtils.substring(newXml, 0, newXml.length() - AGGREGATE_END_TAG.length()) + splitXml + AGGREGATE_END_TAG;
        } else {
            result = XML_DECLARATION_UTF_8 + AGGREGATE_INIT_TAG + splitXml + AGGREGATE_END_TAG;
        }
        return result;
    }

    private boolean containsXmlDeclaration(String xml) {
        return xml.trim().startsWith("<?xml");
    }

    private String getBody(Exchange exchange) {

        try {
            return exchange.getIn().getBody(String.class);
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to get data from the route to the Aggregate component.");
            }
        }

        return null;
    }

}
