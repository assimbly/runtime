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

        AggregationStrategy enrichStrategy = switch (enrichType) {
            case "xml", "text/xml", "application/xml" -> new XmlEnrichStrategy();
            case "json", "application/json" -> new JsonEnrichStrategy();
            case "application/zip" -> new ZipFileEnrichStrategy();
            case "application/attachment" -> new AttachmentEnrichStrategy();
            case "application/override" -> new OverrideEnrichStrategy();
            default -> throw new UnsupportedOperationException();
        };

        return enrichStrategy.aggregate(originalExchange, resourceExchange);
    }

}
