package org.assimbly.dil.blocks.beans.enrich;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.log4j.Logger;
import org.assimbly.dil.blocks.beans.enrich.attachment.AttachmentEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.json.JsonEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.override.OverrideEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.xml.XmlEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.zipfile.ZipFileEnrichStrategy;

public class EnrichStrategy implements AggregationStrategy {

    final static Logger logger = Logger.getLogger(EnrichStrategy.class);

    private AggregationStrategy EnrichStrategy;

    @Override
    public Exchange aggregate(Exchange originalExchange, Exchange resourceExchange) {

        String enrichType= "";

        if (originalExchange != null && originalExchange.getProperty("Enrich-Type") != null)
                enrichType = originalExchange.getProperty("Enrich-Type", String.class);

        switch(enrichType) {
            case "text/xml":
                EnrichStrategy = (AggregationStrategy) new XmlEnrichStrategy();
                break;
            case "application/json":
                EnrichStrategy = (AggregationStrategy) new JsonEnrichStrategy();
                break;
            case "application/zip":
                EnrichStrategy = (AggregationStrategy) new ZipFileEnrichStrategy();
                break;
            case "application/attachment":
                EnrichStrategy = (AggregationStrategy) new AttachmentEnrichStrategy();
                break;
            case "application/override":
                EnrichStrategy = (AggregationStrategy) new OverrideEnrichStrategy();
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return EnrichStrategy.aggregate(originalExchange, resourceExchange);
    }

}
