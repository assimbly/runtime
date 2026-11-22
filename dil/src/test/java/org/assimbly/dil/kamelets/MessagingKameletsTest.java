package org.assimbly.dil.kamelets;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.Endpoint;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.component.sjms.SjmsComponent;
import org.apache.camel.component.sjms.SjmsEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.BasicConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.io.FileHandler;
import org.apache.commons.configuration2.tree.xpath.XPathExpressionEngine;
import org.assimbly.dil.blocks.connections.Connection;
import org.assimbly.dil.transpiler.marshalling.Unmarshall;
import org.assimbly.dil.transpiler.marshalling.catalog.CustomKameletCatalog;
import org.assimbly.util.EncryptionUtil;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the ActiveMQ and Artemis (SJMS) DIL Kamelets and their JMS connection wiring. */
class MessagingKameletsTest {

    private static final List<String> KAMELETS = List.of(
            "activemq-source",
            "activemq-action",
            "artemis-source",
            "artemis-action");

    private DefaultCamelContext context;

    @BeforeEach
    void createContext() throws Exception {
        context = new DefaultCamelContext();
        context.addComponent("activemq", new JmsComponent());
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        for (String name : KAMELETS) {
            PluginHelper.getRoutesLoader(context).loadRoutes(
                    ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + name + ".kamelet.yaml"));
        }
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {"activemq-source", "activemq-action", "artemis-source", "artemis-action"})
    void loadsAsDilRouteTemplate(String name) {
        RouteTemplateDefinition template = context.getRouteTemplateDefinition(name);
        assertNotNull(template, "Expected route template with id " + name);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Set<String> params = template.getTemplateParameters().stream()
                .map(RouteTemplateParameterDefinition::getName)
                .collect(Collectors.toSet());

        assertTrue(params.containsAll(List.of("path", "connectionFactory", "in", "out", "routeId", "stepId", "routeConfigurationId")),
                "Missing DIL parameters in " + name + ": " + params);
        if (name.endsWith("-action")) {
            assertTrue(params.contains("sink"), "Missing sink parameter in " + name);
        }
    }

    @Test
    void allMessagingKameletsAreListedInClasspathIndex() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/kamelets/index.txt")) {
            assertNotNull(is, "/kamelets/index.txt should be generated on the classpath");
            Set<String> indexed = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toSet());

            for (String name : KAMELETS) {
                assertTrue(indexed.contains(name + ".kamelet.yaml"),
                        "Expected " + name + ".kamelet.yaml in /kamelets/index.txt");
            }
        }
    }

    @Test
    void transpilesActiveMqSourceAndActionWithConnectionFactory() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Orders</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5997</id><type>source</type><uri>activemq:orders.in</uri>\
                <blocks><block><id>amq1</id><type>connection</type></block></blocks>\
                <links><link><id>5914-5998</id><bound>out</bound><transport>sync</transport></link></links></step>\
                <step><id>5998</id><type>sink</type><uri>activemq:orders.out</uri>\
                <options><exchangePattern>InOnly</exchangePattern><timeToLive>60000</timeToLive></options>\
                <blocks><block><id>amq1</id><type>connection</type></block></blocks>\
                <links><link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link></links></step>\
                </steps></flow></flows></integration></integrations><core><connections>\
                <connection><id>amq1</id><name>ExternalActiveMQ</name><type>activemq</type>\
                <keys><url>tcp://localhost:61616</url><jmsprovider>ActiveMQ Classic</jmsprovider></keys></connection>\
                </connections><messages/><routes/><routeConfigurations/></core></dil>""";

        TreeMap<String, String> props = unmarshallWithCatalog(dil);

        String sourceTemplate = props.get("source.5997.routetemplate");
        assertNotNull(sourceTemplate);
        assertTrue(sourceTemplate.contains("routeTemplateRef=\"activemq-source\""), sourceTemplate);
        assertTrue(sourceTemplate.contains("name=\"path\" value=\"orders.in\""), sourceTemplate);
        assertTrue(sourceTemplate.contains("name=\"connectionFactory\" value=\"#amq1\""), sourceTemplate);

        String sinkTemplate = props.get("sink.5998.routetemplate");
        assertNotNull(sinkTemplate);
        assertTrue(sinkTemplate.contains("routeTemplateRef=\"activemq-action\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"path\" value=\"orders.out\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"connectionFactory\" value=\"#amq1\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"exchangePattern\" value=\"InOnly\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"timeToLive\" value=\"60000\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"sink\" value=\"true\""), sinkTemplate);
    }

    @Test
    void transpilesArtemisSourceAndActionWithConnectionFactory() throws Exception {
        String dil = """
                <?xml version="1.0" encoding="UTF-8"?><dil><version>6.0.0</version><integrations><integration><id>1001</id><name>default</name><type>FULL</type>\
                <options><environment>Dev1</environment><stage>DEVELOPMENT</stage></options><flows><flow><id>5914</id><name>Events</name><type>esb</type><version>1</version>\
                <options><autostart>false</autostart><logLevel>OFF</logLevel></options><steps>\
                <step><id>5999</id><type>error</type><uri/></step>\
                <step><id>5997</id><type>source</type><uri>artemis:queue:events.in</uri>\
                <options><concurrentConsumers>3</concurrentConsumers></options>\
                <blocks><block><id>art1</id><type>connection</type></block></blocks>\
                <links><link><id>5914-5998</id><bound>out</bound><transport>sync</transport></link></links></step>\
                <step><id>5998</id><type>sink</type><uri>artemis:topic:events.out</uri>\
                <options><deliveryPersistent>false</deliveryPersistent></options>\
                <blocks><block><id>art1</id><type>connection</type></block></blocks>\
                <links><link><id>5914-5998</id><bound>in</bound><transport>sync</transport></link></links></step>\
                </steps></flow></flows></integration></integrations><core><connections>\
                <connection><id>art1</id><name>ExternalArtemis</name><type>artemis</type>\
                <keys><url>tcp://localhost:61616</url><jmsprovider>ActiveMQ Artemis</jmsprovider></keys></connection>\
                </connections><messages/><routes/><routeConfigurations/></core></dil>""";

        TreeMap<String, String> props = unmarshallWithCatalog(dil);

        String sourceTemplate = props.get("source.5997.routetemplate");
        assertNotNull(sourceTemplate);
        assertTrue(sourceTemplate.contains("routeTemplateRef=\"artemis-source\""), sourceTemplate);
        assertTrue(sourceTemplate.contains("name=\"path\" value=\"queue:events.in\""), sourceTemplate);
        assertTrue(sourceTemplate.contains("name=\"connectionFactory\" value=\"#art1\""), sourceTemplate);
        assertTrue(sourceTemplate.contains("name=\"concurrentConsumers\" value=\"3\""), sourceTemplate);

        String sinkTemplate = props.get("sink.5998.routetemplate");
        assertNotNull(sinkTemplate);
        assertTrue(sinkTemplate.contains("routeTemplateRef=\"artemis-action\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"path\" value=\"topic:events.out\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"connectionFactory\" value=\"#art1\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"deliveryPersistent\" value=\"false\""), sinkTemplate);
        assertTrue(sinkTemplate.contains("name=\"sink\" value=\"true\""), sinkTemplate);
    }

    @Test
    void connectionDispatchesArtemisToSjmsAndBindsArtemisConnectionFactory() throws Exception {
        bindEncryptionHelpers();

        TreeMap<String, String> props = new TreeMap<>();
        props.put("source.1.connection.id", "artemisConn");
        props.put("connection.artemisConn.type", "artemis");
        props.put("connection.artemisConn.url", "tcp://localhost:61616");

        new Connection(context, props, "source.1.connection.id").start();

        Object bound = context.getRegistry().lookupByName("artemisConn");
        assertInstanceOf(org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory.class, bound);

        SjmsComponent sjms = context.getComponent("sjms", SjmsComponent.class);
        assertNotNull(sjms);
        assertTrue(sjms.isBridgeErrorHandler());
    }

    @Test
    void resolvesActiveMqAndArtemisSourceEndpoints() throws Exception {
        ConnectionFactory classicCf = new ActiveMQConnectionFactory("vm://localhost?broker.persistent=false");
        context.getRegistry().bind("amqConn", classicCf);

        ConnectionFactory artemisCf = new org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory("tcp://localhost:61616");
        context.getRegistry().bind("artConn", artemisCf);

        TemplatedRouteBuilder.builder(context, "activemq-source")
                .routeId("amq-src")
                .parameter("routeId", "amq-src")
                .parameter("stepId", "1")
                .parameter("path", "orders.in")
                .parameter("connectionFactory", "#amqConn")
                .parameter("out", "mock:amqOut")
                .add();

        TemplatedRouteBuilder.builder(context, "artemis-source")
                .routeId("art-src")
                .parameter("routeId", "art-src")
                .parameter("stepId", "2")
                .parameter("path", "queue:events.in")
                .parameter("connectionFactory", "#artConn")
                .parameter("concurrentConsumers", "2")
                .parameter("out", "mock:artOut")
                .add();

        Endpoint amqEndpoint = context.getEndpoint("activemq:orders.in?connectionFactory=#amqConn&synchronous=true");
        JmsEndpoint jmsEndpoint = assertInstanceOf(JmsEndpoint.class, amqEndpoint);
        assertEquals("orders.in", jmsEndpoint.getDestinationName());
        assertSame(classicCf, jmsEndpoint.getConfiguration().getConnectionFactory());
        assertTrue(jmsEndpoint.getConfiguration().isSynchronous());

        Endpoint artEndpoint = context.getEndpoint(
                "sjms:queue:events.in?connectionFactory=#artConn&acknowledgementMode=AUTO_ACKNOWLEDGE&transacted=false&concurrentConsumers=2&asyncConsumer=false&synchronous=false");
        SjmsEndpoint sjmsEndpoint = assertInstanceOf(SjmsEndpoint.class, artEndpoint);
        assertEquals("events.in", sjmsEndpoint.getDestinationName());
        assertSame(artemisCf, sjmsEndpoint.getConnectionFactory());
        assertEquals(2, sjmsEndpoint.getConcurrentConsumers());
        assertFalse(sjmsEndpoint.isTransacted());
    }

    private void bindEncryptionHelpers() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("test-secret");
        context.getRegistry().bind("encryptableProperties", new EncryptableProperties(encryptor));
        context.getRegistry().bind("encryptionUtil", new EncryptionUtil("test-secret"));
    }

    private TreeMap<String, String> unmarshallWithCatalog(String dil) throws Exception {
        XMLConfiguration conf = new BasicConfigurationBuilder<>(XMLConfiguration.class)
                .configure(new Parameters().xml().setExpressionEngine(new XPathExpressionEngine()))
                .getConfiguration();
        new FileHandler(conf).load(new StringReader(dil));

        List<String> files = KAMELETS.stream().map(k -> k + ".kamelet.yaml").toList();
        files.forEach(CustomKameletCatalog::addName);
        try {
            return new Unmarshall().getProperties(conf, "5914");
        } finally {
            files.forEach(CustomKameletCatalog::removeName);
        }
    }
}
