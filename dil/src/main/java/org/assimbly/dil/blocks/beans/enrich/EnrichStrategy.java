package org.assimbly.dil.blocks.beans.enrich;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.assimbly.dil.blocks.beans.enrich.attachment.AttachmentEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.json.JsonEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.override.OverrideEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.xml.XmlEnrichStrategy;
import org.assimbly.dil.blocks.beans.enrich.zipfile.ZipFileEnrichStrategy;

public class EnrichStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange originalExchange, Exchange resourceExchange) {

        String enrichType= "";

        if (originalExchange != null && originalExchange.getProperty("Enrich-Type") != null) {
            enrichType = originalExchange.getProperty("Enrich-Type", String.class);
        }

        AggregationStrategy enrichStrategy;

        switch(enrichType) {
            case "xml":
            case "text/xml":
            case "application/xml":
                enrichStrategy = new XmlEnrichStrategy();
                break;
            case "json":
            case "application/json":
                enrichStrategy = new JsonEnrichStrategy();
                break;
            case "application/zip":
                enrichStrategy = new ZipFileEnrichStrategy();
                break;
            case "application/attachment":
                enrichStrategy = new AttachmentEnrichStrategy();
                break;
            case "application/override":
                enrichStrategy = new OverrideEnrichStrategy();
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return enrichStrategy.aggregate(originalExchange, resourceExchange);
    }

}
