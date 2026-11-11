package org.assimbly.brokerrest;

import jakarta.jms.Connection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.broker.BrokerService;
import org.assimbly.broker.Broker;
import org.assimbly.broker.impl.ActiveMQArtemis;
import org.assimbly.broker.impl.ActiveMQClassic;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class BrokerReviewRegressionTest {

    private EmbeddedActiveMQ embeddedArtemis;
    private BrokerService classicService;

    @AfterEach
    void stopBrokers() throws Exception {
        if (embeddedArtemis != null) {
            embeddedArtemis.stop();
        }
        if (classicService != null) {
            classicService.stop();
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
        Broker classic = (Broker) Proxy.newProxyInstance(Broker.class.getClassLoader(), new Class<?>[]{Broker.class},
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
        setField(runtime, ManagedBrokerRuntime.class, "classic", classic);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> runtime.start("classic", "file"));
            var second = executor.submit(() -> runtime.start("classic", "file"));
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
        ActiveMQArtemis artemis = artemis(directory);

        assertEquals("success", artemis.createQueue("created"));
        assertTrue(artemis.getQueues().contains("created"));
    }

    @Test
    void artemisCountsMessagesOfAllListedQueues(@TempDir Path directory) throws Exception {
        ActiveMQArtemis artemis = artemis(directory, "first", "second");
        artemis.sendMessage("first", null, "body");
        artemis.sendMessage("second", null, "body");
        artemis.sendMessage("second", null, "body");

        assertEquals("3", artemis.countMessagesFromList("first, second, unknown"));
    }

    @Test
    void artemisBrowsesMessageById(@TempDir Path directory) throws Exception {
        ActiveMQArtemis artemis = artemis(directory, "queue");
        artemis.sendMessage("queue", null, "first");
        artemis.sendMessage("queue", null, "second");
        JSONArray listed = messages(artemis.listMessages("queue", null));
        String messageId = String.valueOf(listed.getJSONObject(1).get("messageID"));

        JSONArray browsed = messages(artemis.browseMessage("queue", messageId, false));

        assertEquals(1, browsed.length());
        assertEquals("second", browsed.getJSONObject(0).get("body"));
    }

    @Test
    void classicTreatsTopicsAsEndpointsButMovesAndPurgesQueuesOnly() throws Exception {
        ActiveMQClassic classic = classic();
        classic.createQueue("queue");
        classic.createTopic("topic");

        assertEquals("success", classic.sendMessage("topic", null, "body"));
        assertEquals("0", classic.countMessagesFromList("topic"));
        assertThrows(EndpointNotFoundException.class, () -> classic.moveMessages("topic", "queue"));
        assertThrows(EndpointNotFoundException.class, () -> classic.removeMessages("topic"));
        assertThrows(EndpointNotFoundException.class, () -> classic.countMessages("unknown"));
        assertEquals("success", classic.deleteQueue("queue"));
    }

    @Test
    void classicBrowsesOnlyTheRequestedMessageAndRejectsInvalidPages() throws Exception {
        ActiveMQClassic classic = classic();
        classic.createQueue("queue");
        classic.sendMessage("queue", null, "body");

        assertEquals(1, messages(classic.browseMessages("queue", 1, 10, false)).length());
        assertEquals(0, messages(classic.browseMessage("queue", "x' OR '1'='1", false)).length());
        assertThrows(IllegalArgumentException.class, () -> classic.browseMessages("queue", 0, 10, false));
        assertThrows(IllegalArgumentException.class, () -> classic.browseMessages("queue", 1, 0, false));
    }

    @Test
    void classicStatsWorkWithoutOpenwireConnector() throws Exception {
        assertEquals(0, classic().stats().get("maxConnections"));
    }

    @Test
    void classicListsEveryConnection() throws Exception {
        ActiveMQClassic classic = classic();
        var factory = new ActiveMQConnectionFactory("vm://" + classicService.getBrokerName() + "?create=false");

        try (Connection first = factory.createConnection(); Connection second = factory.createConnection()) {
            first.start();
            second.start();

            JSONArray connections = new JSONObject(classic.getConnections())
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
            assertEquals("success", new ActiveMQClassic().setFileConfiguration(null));
            assertEquals("success", new ActiveMQArtemis().setFileConfiguration(null));
        } finally {
            baseDirectory.setBaseDirectory(previous);
        }
        assertTrue(directory.resolve("broker/activemq.xml").toFile().exists());
        assertTrue(directory.resolve("broker/broker.xml").toFile().exists());
    }

    @Test
    void brokersThatWereNeverStartedAreStoppedAndCanBeStopped() throws Exception {
        for (Broker broker : new Broker[]{new ActiveMQClassic(), new ActiveMQArtemis()}) {
            assertEquals("stopped", broker.status());
            assertEquals("stopped", broker.stop());
            assertNull(broker.getBroker());
        }
    }

    @Test
    void classicReturnsTheRequestedPage() throws Exception {
        ActiveMQClassic classic = classic();
        classic.createQueue("queue");
        for (int i = 0; i < 3; i++) {
            classic.sendMessage("queue", null, "body");
        }

        assertEquals(2, messages(classic.browseMessages("queue", 1, 2, false)).length());
        assertEquals(1, messages(classic.browseMessages("queue", 2, 2, false)).length());
        assertEquals(0, messages(classic.browseMessages("queue", 3, 2, false)).length());
        assertEquals(0, messages(classic.browseMessages("queue", Integer.MAX_VALUE, Integer.MAX_VALUE, false)).length());
    }

    @Test
    void artemisRejectsUnknownTopicAndSkipsHeadersWithoutValue(@TempDir Path directory) throws Exception {
        ActiveMQArtemis artemis = artemis(directory, "queue");
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
        setField(runtime, ManagedBrokerRuntime.class, "classic", stopped);

        assertThrows(IllegalArgumentException.class, () -> runtime.start("classic", "unknown"));
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

        var created = controller.createQueue("classic", "queue", "text/plain");
        var deleted = controller.deleteQueue("classic", "queue", "text/plain");

        assertEquals(200, created.getStatusCode().value());
        assertEquals("success", created.getBody());
        assertEquals(400, deleted.getStatusCode().value());
        assertEquals("Endpoint queue not found", deleted.getBody());
    }

    private ActiveMQArtemis artemis(Path directory, String... queues) throws Exception {
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
        var artemis = new ActiveMQArtemis();
        setField(artemis, ActiveMQArtemis.class, "broker", embeddedArtemis);
        return artemis;
    }

    private ActiveMQClassic classic() throws Exception {
        classicService = new BrokerService();
        classicService.setBrokerName("review-" + System.nanoTime());
        classicService.setPersistent(false);
        classicService.start();
        var classic = new ActiveMQClassic();
        setField(classic, ActiveMQClassic.class, "broker", classicService);
        classic.setBrokerViewMBean();
        return classic;
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
