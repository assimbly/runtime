package org.assimbly.dil.transpiler;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How XMLFileConfiguration reads a flow configuration that isn't to be trusted. */
class XMLFileConfigurationTest {

    private static final String DIL = """
            <dil><integrations>\
            <integration><id>1001</id><flows><flow><id>5914</id></flow></flows></integration>\
            <integration><id>1002</id><flows><flow><id>6000</id></flow></flows></integration>\
            </integrations></dil>""";

    @Test
    void doctypeIsRejected() {
        String xml = "<!DOCTYPE dil [<!ENTITY xxe SYSTEM \"file:///etc/hostname\">]>" + DIL;

        SAXParseException exception = assertThrows(SAXParseException.class, () -> parse(xml));

        assertTrue(exception.getMessage().contains("DOCTYPE"), exception.getMessage());
    }

    @Test
    void flowIdsAreThoseOfTheIntegration() throws Exception {
        assertEquals(List.of("5914"), XMLFileConfiguration.getFlowIds("1001", parse(DIL)));
    }

    @Test
    void integrationIdCannotChangeTheExpression() throws Exception {
        assertEquals(List.of(), XMLFileConfiguration.getFlowIds("0 or 1=1", parse(DIL)));
    }

    private static Document parse(String xml) throws Exception {
        return XMLFileConfiguration.newDocumentBuilderFactory().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
    }

}
