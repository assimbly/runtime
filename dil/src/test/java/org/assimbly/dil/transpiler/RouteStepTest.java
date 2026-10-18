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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A Route Step's route as the transpiler hands it to the runtime. */
class RouteStepTest {

    private static final String FLOW_ID = "5914";
    private static final String ROUTE_KEY = "route.5998.route";

    @Test
    void jmsEndpointsUseTheStepsConnection() throws Exception {
        String route = transpile("SJMS", """
                <route id="5998">
                  <from uri="sjms:queue:in"/>
                  <to uri="sjms2:queue:out?timeToLive=1000"/>
                  <to uri="jms:queue:other?connectionFactory=#mine"/>
                  <to uri="file:out"/>
                </route>""");

        assertTrue(route.contains("uri=\"sjms:queue:in?connectionFactory=#1251\""), route);
        assertTrue(route.contains("uri=\"sjms2:queue:out?timeToLive=1000&amp;connectionFactory=#1251\""), route);
        assertTrue(route.contains("uri=\"jms:queue:other?connectionFactory=#mine\""), route);
        assertTrue(route.contains("uri=\"file:out\""), route);
    }

    @Test
    void otherConnectionsLeaveTheRouteAlone() throws Exception {
        String route = transpile("JDBC", """
                <route id="5998">
                  <from uri="sjms:queue:in"/>
                </route>""");

        assertTrue(route.contains("uri=\"sjms:queue:in\""), route);
    }

    @Test
    void logsAreNamedAfterTheFlow() throws Exception {
        String route = transpile("SJMS", """
                <route id="5998">
                  <from uri="file:in"/>
                  <log message="Received a new customer!"/>
                  <log message="named" logName="mine"/>
                </route>""");

        assertEquals(1, route.split("logName=\"MoveFiles\"", -1).length - 1, route);
        assertTrue(route.contains("logName=\"mine\""), route);
    }

    private String transpile(String connectionType, String route) throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>MoveFiles</name><type>route</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5998</id><type>route</type><uri/><blocks><block><id>5998</id><type>route</type></block><block><id>1251</id><type>connection</type></block></blocks></step>\
                </steps></flow></flows></integration></integrations><core><connections><connection><id>1251</id><name>EmbeddedBroker</name><type>%s</type>\
                <keys><url>tcp://localhost:61616</url><jmsprovider>ActiveMQ Artemis</jmsprovider></keys></connection></connections><messages/>\
                <routes>%s</routes><routeConfigurations/></core></dil>""".formatted(connectionType, route);

        // XMLFileConfiguration does the same, but also validates against the DIL schema
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        TreeMap<String, String> properties = new Unmarshall().getProperties(conf, FLOW_ID);

        return properties.get(ROUTE_KEY);
    }
}
