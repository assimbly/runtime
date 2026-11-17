package org.assimbly.dil.transpiler;

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Router whose named Branch decides on a Condition gets the Branch's language and expression as template parameters. */
class RouterConditionTest {

    private static final String FLOW_ID = "5914";
    private static final String TEMPLATE_KEY = "router.5998.routetemplate";

    @Test
    void splitRouterGetsTheLanguageOfItsSplitBranch() throws Exception {
        String template = transpile("split", "split", "xpath", "/users/user");

        assertTrue(template.contains("name=\"language\" value=\"xpath\""), template);
        assertTrue(template.contains("name=\"expression\" value=\"/users/user\""), template);
    }

    @Test
    void ifRouterGetsTheLanguageOfItsIfBranch() throws Exception {
        String template = transpile("if", "if", "xpath", "/order[@priority='high']");

        assertTrue(template.contains("name=\"language\" value=\"xpath\""), template);
    }

    private String transpile(String kind, String rule, String language, String expression) throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Users</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>router</type><uri>%s</uri>\
                <links>\
                <link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link>\
                <link><id>5914-5997</id><bound>out</bound><transport>sync</transport><rule>%s</rule><language>%s</language><expression>%s</expression></link>\
                <link><id>5914-5996</id><bound>out</bound><transport>sync</transport></link>\
                </links></step>\
                </steps></flow></flows></integration></integrations><core><connections/><messages/>\
                <routes/><routeConfigurations/></core></dil>""".formatted(kind, rule, language, expression.replace("'", "&apos;"));

        // XMLFileConfiguration does the same, but also validates against the DIL schema
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        TreeMap<String, String> properties = new Unmarshall().getProperties(conf, FLOW_ID);

        return properties.get(TEMPLATE_KEY);
    }
}
