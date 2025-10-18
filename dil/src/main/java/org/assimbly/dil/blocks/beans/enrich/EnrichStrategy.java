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
            case "application/xml", "text/xml", "xml"  -> new XmlEnrichStrategy();
            case "application/json", "json" -> new JsonEnrichStrategy();
            case "application/zip", "zip" -> new ZipFileEnrichStrategy();
            case "application/attachment", "attachment" -> new AttachmentEnrichStrategy();
            case "application/override", "override" -> new OverrideEnrichStrategy();
            default -> throw new UnsupportedOperationException("enrichType '" + enrichType + "' isn't a supported enrichStrategy (valid values are xml, json, zip, attachment or override)");
        };

        return enrichStrategy.aggregate(originalExchange, resourceExchange);
    }

}
