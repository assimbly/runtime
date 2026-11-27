package org.assimbly.dil.blocks.beans.enrich.xml;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XmlEnrichStrategyTest {

    private static final String LINK = "<![CDATA[/transactions?dateFrom=2026-11-20&size=2]]>";

    @Test
    void keepsCdataSectionsOfTheOriginalMessage() {
        String result = enrich("<HttpData><Link>" + LINK + "</Link></HttpData>", "<Resource/>");

        assertTrue(result.contains("<Link>" + LINK + "</Link>"), result);
        assertFalse(result.contains("&amp;"), result);
    }

    @Test
    void keepsCdataSectionsOfTheResource() {
        String result = enrich("<Original/>", "<HttpData><Link>" + LINK + "</Link></HttpData>");

        assertTrue(result.contains("<Link>" + LINK + "</Link>"), result);
        assertFalse(result.contains("&amp;"), result);
    }

    private static String enrich(String originalBody, String resourceBody) {
        DefaultCamelContext context = new DefaultCamelContext();

        Exchange original = new DefaultExchange(context);
        original.getIn().setBody(originalBody);
        Exchange resource = new DefaultExchange(context);
        resource.getIn().setBody(resourceBody);

        return new XmlEnrichStrategy().aggregate(original, resource).getIn().getBody(String.class);
    }
}
