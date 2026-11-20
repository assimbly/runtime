package org.assimbly.dil.transpiler;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A trycatch Router uses the trycatch-router kamelet, with its unnamed Branch as try and its catch Branch as catch. */
class TryCatchRouterTest {

    private static final String KAMELET = "trycatch-router.kamelet.yaml";

    @Test
    void trycatchRouterGetsDefaultAndCatchBranches() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Orders</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>router</type><uri>trycatch</uri>\
                <links>\
                <link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link>\
                <link><id>5914-5997</id><bound>out</bound><transport>sync</transport></link>\
                <link><id>5914-5996</id><bound>out</bound><transport>sync</transport><rule>catch</rule></link>\
                </links></step>\
                </steps></flow></flows></integration></integrations><core><connections/><messages/>\
                <routes/><routeConfigurations/></core></dil>""";

        // XMLFileConfiguration does the same, but also validates against the DIL schema
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        // At runtime ConfigManager fills the catalog from the kamelet index
        CustomKameletCatalog.addName(KAMELET);
        String template;
        try {
            TreeMap<String, String> properties = new Unmarshall().getProperties(conf, "5914");
            template = properties.get("router.5998.routetemplate");
        } finally {
            CustomKameletCatalog.removeName(KAMELET);
        }

        assertTrue(template.contains("routeTemplateRef=\"trycatch-router\""), template);
        assertTrue(template.contains("name=\"out_default\" value=\"sync:5914-5997\""), template);
        assertTrue(template.contains("name=\"out_catch\" value=\"sync:5914-5996\""), template);
    }
}
