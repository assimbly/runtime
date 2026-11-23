package org.assimbly.brokerrest;

import jakarta.jms.Connection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.broker.BrokerService;
import org.assimbly.broker.Broker;
import org.assimbly.broker.impl.Artemis;
import org.assimbly.broker.impl.ActiveMQ;
import org.assimbly.broker.impl.EndpointNotFoundException;
import org.assimbly.util.BaseDirectory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BrokerReviewRegressionTest {

    private EmbeddedActiveMQ embeddedArtemis;
    private BrokerService activemqService;

    @AfterEach
    void stopBrokers() throws Exception {
        if (embeddedArtemis != null) {
            embeddedArtemis.stop();
        }
        if (activemqService != null) {
            activemqService.stop();
        }
    }

    @Test
    void concurrentStartRequestsStartTheBrokerOnce() throws Exception {
        var runtime = new ManagedBrokerRuntime();
        var starts = new AtomicInteger();
        var started = new AtomicBoolean();
        var firstStartEntered = new CountDownLatch(1);
        var secondStartEntered = new CountDownLatch(2);
        var resumeStart = new CountDownLatch(1);
        Broker activemq = (Broker) Proxy.newProxyInstance(Broker.class.getClassLoader(), new Class<?>[]{Broker.class},
                (object, method, args) -> switch (method.getName()) {
                    case "status" -> started.get() ? "started" : "stopped";
                    case "start" -> {
                        starts.incrementAndGet();
                        firstStartEntered.countDown();
                        secondStartEntered.countDown();
                        assertTrue(resumeStart.await(5, TimeUnit.SECONDS));
                        started.set(true);
                        yield "started";
                    }
                    default -> throw new AssertionError(method.getName());
                });
        setField(runtime, ManagedBrokerRuntime.class, "activemq", activemq);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runtime.start("activemq", "file"));
            var second = executor.submit(() -> runtime.start("activemq", "file"));
            try {
                assertTrue(firstStartEntered.await(5, TimeUnit.SECONDS));
                assertFalse(secondStartEntered.await(300, TimeUnit.MILLISECONDS), "Second request started the broker again");
            } finally {
                resumeStart.countDown();
            }
            assertEquals("started", first.get(5, TimeUnit.SECONDS));
            assertEquals("started", second.get(5, TimeUnit.SECONDS));
        }
        assertEquals(1, starts.get());
    }

    @Test
    void artemisManagesQueuesWhenNotStartedFromFile(@TempDir Path directory) throws Exception {
        Artemis artemis = artemis(directory);

        assertEquals("success", artemis.createQueue("created"));
        assertTrue(artemis.getQueues().contains("created"));
    }

    @Test
    void artemisCountsMessagesOfAllListedQueues(@TempDir Path directory) throws Exception {
        Artemis artemis = artemis(directory, "first", "second");
        artemis.sendMessage("first", null, "body");
        artemis.sendMessage("second", null, "body");
        artemis.sendMessage("second", null, "body");

        assertEquals("3", artemis.countMessagesFromList("first, second, unknown"));
    }

    @Test
    void artemisBrowsesMessageById(@TempDir Path directory) throws Exception {
        Artemis artemis = artemis(directory, "queue");
        artemis.sendMessage("queue", null, "first");
        artemis.sendMessage("queue", null, "second");
        JSONArray listed = messages(artemis.listMessages("queue", null));
        String messageId = String.valueOf(listed.getJSONObject(1).get("messageID"));

        JSONArray browsed = messages(artemis.browseMessage("queue", messageId, false));

        assertEquals(1, browsed.length());
        assertEquals("second", browsed.getJSONObject(0).get("body"));
    }

    @Test
    void activemqTreatsTopicsAsEndpointsButMovesAndPurgesQueuesOnly() throws Exception {
        ActiveMQ activemq = activemq();
        activemq.createQueue("queue");
        activemq.createTopic("topic");

        assertEquals("success", activemq.sendMessage("topic", null, "body"));
        assertEquals("0", activemq.countMessagesFromList("topic"));
        assertThrows(EndpointNotFoundException.class, () -> activemq.moveMessages("topic", "queue"));
        assertThrows(EndpointNotFoundException.class, () -> activemq.removeMessages("topic"));
        assertThrows(EndpointNotFoundException.class, () -> activemq.countMessages("unknown"));
        assertEquals("success", activemq.deleteQueue("queue"));
    }

    @Test
    void activemqBrowsesOnlyTheRequestedMessageAndRejectsInvalidPages() throws Exception {
        ActiveMQ activemq = activemq();
        activemq.createQueue("queue");
        activemq.sendMessage("queue", null, "body");

        assertEquals(1, messages(activemq.browseMessages("queue", 1, 10, false)).length());
        assertEquals(0, messages(activemq.browseMessage("queue", "x' OR '1'='1", false)).length());
        assertThrows(IllegalArgumentException.class, () -> activemq.browseMessages("queue", 0, 10, false));
        assertThrows(IllegalArgumentException.class, () -> activemq.browseMessages("queue", 1, 0, false));
    }

    @Test
    void activemqStatsWorkWithoutOpenwireConnector() throws Exception {
        assertEquals(0, activemq().stats().get("maxConnections"));
    }

    @Test
    void activemqListsEveryConnection() throws Exception {
        ActiveMQ activemq = activemq();
        var factory = new ActiveMQConnectionFactory("vm://" + activemqService.getBrokerName() + "?create=false");

        try (Connection first = factory.createConnection(); Connection second = factory.createConnection()) {
            first.start();
            second.start();

            JSONArray connections = new JSONObject(activemq.getConnections())
                    .getJSONObject("connections").getJSONArray("connection");

            assertEquals(2, connections.length());
            assertNotEquals(connections.getJSONObject(0).get("connectionID").toString(),
                    connections.getJSONObject(1).get("connectionID").toString());
        }
    }

    @Test
    void absentConfigurationWritesTheDefaultConfiguration(@TempDir Path directory) throws Exception {
        BaseDirectory baseDirectory = BaseDirectory.getInstance();
        String previous = baseDirectory.getBaseDirectory();
        baseDirectory.setBaseDirectory(directory.toString());
        try {
            assertEquals("success", new ActiveMQ().setFileConfiguration(null));
            assertEquals("success", new Artemis().setFileConfiguration(null));
        } finally {
            baseDirectory.setBaseDirectory(previous);
        }
        assertTrue(directory.resolve("broker/activemq.xml").toFile().exists());
        assertTrue(directory.resolve("broker/broker.xml").toFile().exists());
    }

    @Test
    void brokersThatWereNeverStartedAreStoppedAndCanBeStopped() throws Exception {
        for (Broker broker : new Broker[]{new ActiveMQ(), new Artemis()}) {
            assertEquals("stopped", broker.status());
            assertEquals("stopped", broker.stop());
            assertNull(broker.getBroker());
        }
    }

    @Test
    void activemqReturnsTheRequestedPage() throws Exception {
        ActiveMQ activemq = activemq();
        activemq.createQueue("queue");
        for (int i = 0; i < 3; i++) {
            activemq.sendMessage("queue", null, "body");
        }

        assertEquals(2, messages(activemq.browseMessages("queue", 1, 2, false)).length());
        assertEquals(1, messages(activemq.browseMessages("queue", 2, 2, false)).length());
        assertEquals(0, messages(activemq.browseMessages("queue", 3, 2, false)).length());
        assertEquals(0, messages(activemq.browseMessages("queue", Integer.MAX_VALUE, Integer.MAX_VALUE, false)).length());
    }

    @Test
    void artemisRejectsUnknownTopicAndSkipsHeadersWithoutValue(@TempDir Path directory) throws Exception {
        Artemis artemis = artemis(directory, "queue");
        var headers = new HashMap<String, Object>();
        headers.put("empty", null);
        headers.put("custom", "value");

        assertThrows(EndpointNotFoundException.class, () -> artemis.getTopic("unknown"));
        assertNotNull(artemis.sendMessage("queue", headers, "body"));
        assertEquals("1", artemis.countMessages("queue"));
    }

    @Test
    void unknownConfigurationTypeIsRejected() throws Exception {
        var runtime = new ManagedBrokerRuntime();
        Broker stopped = (Broker) Proxy.newProxyInstance(Broker.class.getClassLoader(), new Class<?>[]{Broker.class},
                (object, method, args) -> "stopped");
        setField(runtime, ManagedBrokerRuntime.class, "activemq", stopped);

        assertThrows(IllegalArgumentException.class, () -> runtime.start("activemq", "unknown"));
    }

    @Test
    void legacyClassicBrokerTypeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ManagedBrokerRuntime().getStatus("classic"));
    }

    @Test
    void controllersAnswerWithTheResultOrTheReasonOfTheFailure() {
        var controller = new QueueManagerRuntime(new ManagedBrokerRuntime() {
            @Override
            public String createQueue(String brokerType, String queueName) {
                return "success";
            }

            @Override
            public String deleteQueue(String brokerType, String queueName) throws Exception {
                throw new EndpointNotFoundException("Endpoint " + queueName + " not found");
            }
        });

        var created = controller.createQueue("activemq", "queue", "text/plain");
        var deleted = controller.deleteQueue("activemq", "queue", "text/plain");

        assertEquals(200, created.getStatusCode().value());
        assertEquals("success", created.getBody());
        assertEquals(400, deleted.getStatusCode().value());
        assertEquals("Endpoint queue not found", deleted.getBody());
    }

    private Artemis artemis(Path directory, String... queues) throws Exception {
        embeddedArtemis = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .setBindingsDirectory(directory.resolve("bindings").toString())
                .setJournalDirectory(directory.resolve("journal").toString())
                .setLargeMessagesDirectory(directory.resolve("large-messages").toString())
                .setPagingDirectory(directory.resolve("paging").toString())
                .setJMXManagementEnabled(false));
        embeddedArtemis.start();
        for (String queue : queues) {
            embeddedArtemis.getActiveMQServer().createQueue(QueueConfiguration.of(queue));
        }
        var artemis = new Artemis();
        setField(artemis, Artemis.class, "broker", embeddedArtemis);
        return artemis;
    }

    private ActiveMQ activemq() throws Exception {
        activemqService = new BrokerService();
        activemqService.setBrokerName("review-" + System.nanoTime());
        activemqService.setPersistent(false);
        activemqService.start();
        var activemq = new ActiveMQ();
        setField(activemq, ActiveMQ.class, "broker", activemqService);
        activemq.setBrokerViewMBean();
        return activemq;
    }

    private static JSONArray messages(String json) {
        JSONObject messages = new JSONObject(json).getJSONObject("messages");
        return messages.has("message") ? messages.getJSONArray("message") : new JSONArray();
    }

    private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
