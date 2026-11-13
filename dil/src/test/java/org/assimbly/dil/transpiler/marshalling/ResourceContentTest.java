package org.assimbly.dil.transpiler.marshalling;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Resource bodies (e.g. Groovy scripts) must be stored verbatim. They legitimately contain
 * {@code ${...}} and commas, which earlier broke on Commons Configuration interpolation and
 * list splitting (a GString method call like {@code ${users.size()}} threw a JXPathException).
 */
class ResourceContentTest {

    // Contains a GString method call and commas: both would be mangled by interpolation / splitting.
    private static final String SCRIPT = "result = \"n=${users.size()}, line=${entries.join('\\n')}\"";

    private static final String DIL = """
            <dil>\
            <core><resources>\
            <resource><id>script1</id><name>script1</name><content>%s</content></resource>\
            </resources></core>\
            <integrations><integration><id>1001</id><flows>\
            <flow><id>flow1</id><name>flow1</name></flow>\
            </flows></integration></integrations>\
            </dil>""".formatted(SCRIPT);

    @Test
    void groovyResourceBodyIsStoredVerbatim() {
        TreeMap<String, String> properties = assertDoesNotThrow(() -> new Unmarshall().getProperties(load(DIL), "flow1"));

        assertEquals(SCRIPT, properties.get("resource.script1"));
    }

    private static XMLConfiguration load(String xml) throws Exception {
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(xml));
        return conf;
    }

}
