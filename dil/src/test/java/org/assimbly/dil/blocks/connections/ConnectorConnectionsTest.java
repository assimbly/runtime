package org.assimbly.dil.blocks.connections;

import com.mongodb.client.MongoClient;
import org.apache.camel.component.pulsar.PulsarComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.pulsar.client.api.PulsarClient;
import org.assimbly.dil.blocks.connections.broker.PulsarConnection;
import org.assimbly.dil.blocks.connections.database.MongoDBConnection;
import org.assimbly.util.EncryptionUtil;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConnectorConnectionsTest {

    private DefaultCamelContext context;
    private EncryptableProperties encryptableProperties;
    private final List<AutoCloseable> resourcesToClose = new ArrayList<>();

    @BeforeEach
    void setUp() {
        context = new DefaultCamelContext();
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("test-secret");
        encryptableProperties = new EncryptableProperties(encryptor);
        context.getRegistry().bind("encryptableProperties", encryptableProperties);
        context.getRegistry().bind("encryptionUtil", new EncryptionUtil("test-secret"));
    }

    @AfterEach
    void tearDown() throws Exception {
        for (AutoCloseable closeable : resourcesToClose) {
            closeable.close();
        }
        resourcesToClose.clear();
        context.stop();
    }

    @Test
    void mongoDbConnectionBindsClientFromUriAndReusesExisting() {
        encryptableProperties.setProperty("connection.mongo1.uri", "mongodb://localhost:27017/testdb");

        new MongoDBConnection(context, encryptableProperties, "mongo1").start();
        Object bound = context.getRegistry().lookupByName("mongo1");
        MongoClient firstClient = assertInstanceOf(MongoClient.class, bound);
        resourcesToClose.add(firstClient);

        new MongoDBConnection(context, encryptableProperties, "mongo1").start();
        assertSame(firstClient, context.getRegistry().lookupByName("mongo1"));
    }

    @Test
    void mongoDbConnectionBindsClientFromStructuredFields() {
        encryptableProperties.setProperty("connection.mongo2.hosts", "localhost:27017,localhost:27018");
        encryptableProperties.setProperty("connection.mongo2.username", "appuser");
        encryptableProperties.setProperty("connection.mongo2.password", "secret");
        encryptableProperties.setProperty("connection.mongo2.authsource", "admin");
        encryptableProperties.setProperty("connection.mongo2.tls", "true");

        new MongoDBConnection(context, encryptableProperties, "mongo2").start();
        MongoClient client = assertInstanceOf(MongoClient.class, context.getRegistry().lookupByName("mongo2"));
        resourcesToClose.add(client);
    }

    @Test
    void mongoDbConnectionRejectsMissingHostAndUri() {
        assertThrows(IllegalArgumentException.class,
                () -> new MongoDBConnection(context, encryptableProperties, "mongoMissing").start());
    }

    @Test
    void connectionDispatchesMongoDbType() throws Exception {
        TreeMap<String, String> props = new TreeMap<>();
        props.put("action.1.connection.id", "mongoDispatch");
        props.put("connection.mongoDispatch.type", "mongodb");
        props.put("connection.mongoDispatch.host", "localhost");
        props.put("connection.mongoDispatch.port", "27017");

        new Connection(context, props, "action.1.connection.id").start();

        MongoClient client = assertInstanceOf(MongoClient.class, context.getRegistry().lookupByName("mongoDispatch"));
        resourcesToClose.add(client);
    }

    @Test
    void pulsarConnectionBindsClientConfiguresComponentAndReusesExisting() throws Exception {
        encryptableProperties.setProperty("connection.pulsar1.serviceurl", "pulsar://localhost:6650");
        encryptableProperties.setProperty("connection.pulsar1.token", "dummy-jwt-token");

        new PulsarConnection(context, encryptableProperties, "pulsar1").start();
        PulsarClient firstClient = assertInstanceOf(PulsarClient.class, context.getRegistry().lookupByName("pulsar1"));
        resourcesToClose.add(firstClient);

        PulsarComponent component = context.getComponent("pulsar", PulsarComponent.class);
        assertNotNull(component);
        assertSame(firstClient, component.getPulsarClient());

        new PulsarConnection(context, encryptableProperties, "pulsar1").start();
        assertSame(firstClient, context.getRegistry().lookupByName("pulsar1"));
    }

    @Test
    void pulsarConnectionRejectsMissingServiceUrl() {
        assertThrows(IllegalArgumentException.class,
                () -> new PulsarConnection(context, encryptableProperties, "pulsarMissing").start());
    }

    @Test
    void connectionDispatchesPulsarType() throws Exception {
        TreeMap<String, String> props = new TreeMap<>();
        props.put("source.1.connection.id", "pulsarDispatch");
        props.put("connection.pulsarDispatch.type", "pulsar");
        props.put("connection.pulsarDispatch.serviceurl", "pulsar://localhost:6650");

        new Connection(context, props, "source.1.connection.id").start();

        PulsarClient client = assertInstanceOf(PulsarClient.class, context.getRegistry().lookupByName("pulsarDispatch"));
        resourcesToClose.add(client);
    }
}
