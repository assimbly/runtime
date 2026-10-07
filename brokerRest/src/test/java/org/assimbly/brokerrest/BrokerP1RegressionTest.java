package org.assimbly.brokerrest;

import org.apache.activemq.artemis.api.core.QueueConfiguration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.broker.jmx.DestinationViewMBean;
import org.assimbly.broker.Broker;
import org.assimbly.broker.impl.ActiveMQArtemis;
import org.assimbly.broker.impl.ActiveMQClassic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.ResponseEntity;

import javax.management.ObjectName;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static jakarta.jms.DeliveryMode.PERSISTENT;
import static org.junit.jupiter.api.Assertions.*;

class BrokerP1RegressionTest {

    @Test
    void concurrentStatusRequestDoesNotChangeBrokerBeingStarted() throws Exception {
        var runtime = new ManagedBrokerRuntime();
        var checkingStatus = new CountDownLatch(1);
        var resumeStart = new CountDownLatch(1);
        Broker classic = proxy(Broker.class, (object, method, args) -> switch (method.getName()) {
            case "status" -> {
                checkingStatus.countDown();
                assertTrue(resumeStart.await(5, TimeUnit.SECONDS));
                yield "stopped";
            }
            case "start" -> "classic-started";
            default -> throw new AssertionError(method.getName());
        });
        Broker artemis = proxy(Broker.class, (object, method, args) -> switch (method.getName()) {
            case "status" -> "stopped";
            case "start" -> throw new AssertionError("Started the wrong broker");
            default -> throw new AssertionError(method.getName());
        });
        setField(runtime, ManagedBrokerRuntime.class, "classic", classic);
        setField(runtime, ManagedBrokerRuntime.class, "artemis", artemis);

        try (var executor = Executors.newSingleThreadExecutor()) {
            var started = executor.submit(() -> runtime.start("classic", "file"));
            try {
                assertTrue(checkingStatus.await(5, TimeUnit.SECONDS));
                assertEquals("stopped", runtime.getStatus("artemis"));
            } finally {
                resumeStart.countDown();
            }
            assertEquals("classic-started", started.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void controllerNormalizesAbsentEmptyAndJsonNullHeaders() {
        var received = new AtomicReference<Map<String, Object>>();
        var controller = new MessageBrokerRuntime(new ManagedBrokerRuntime() {
            @Override
            public String sendMessage(String type, String endpoint, Map<String, Object> headers, String body) {
                received.set(headers);
                return "success";
            }
        });

        for (String headers : new String[]{null, "{}", "null"}) {
            var response = (ResponseEntity<?>) controller.sendMessage("classic", "queue", "body", "text/plain", headers);
            assertEquals(200, response.getStatusCode().value());
            assertEquals("success", response.getBody());
            assertEquals(Map.of(), received.get());
        }
    }

    @Test
    void malformedHeadersUseTheExistingFailureResponse() {
        var controller = new MessageBrokerRuntime(new ManagedBrokerRuntime() {
            @Override
            public String sendMessage(String type, String endpoint, Map<String, Object> headers, String body) {
                throw new AssertionError("Invalid headers must not reach the broker");
            }
        });

        var response = (ResponseEntity<?>) controller.sendMessage("classic", "queue", "body", "text/plain", "{");
        assertEquals(400, response.getStatusCode().value());
    }

    @Test
    void classicSupportsAbsentAndImmutableHeadersWithoutMutatingTheCaller() throws Exception {
        var sentHeaders = new AtomicReference<Object>();
        DestinationViewMBean destination = proxy(DestinationViewMBean.class, (object, method, args) -> {
            assertEquals("sendTextMessage", method.getName());
            sentHeaders.set(args[0]);
            return "message-id";
        });
        var classic = new ActiveMQClassic() {
            @Override
            public DestinationViewMBean getDestinationViewMBean(String type, String name) {
                return destination;
            }
        };
        BrokerViewMBean view = proxy(BrokerViewMBean.class, (object, method, args) -> {
            assertEquals("getQueues", method.getName());
            return new ObjectName[]{new ObjectName("test:destinationName=queue")};
        });
        setField(classic, ActiveMQClassic.class, "brokerViewMBean", view);

        assertEquals("success", classic.sendMessage("queue", null, "body"));
        assertEquals(Map.of("JMSDeliveryMode", PERSISTENT), sentHeaders.get());
        classic.sendMessage("queue", Map.of(), "body");
        assertEquals(Map.of("JMSDeliveryMode", PERSISTENT), sentHeaders.get());

        Map<String, Object> original = Map.of("JMSDeliveryMode", "PERSISTENT", "JMSTimestamp", 123L, "custom", "value");
        classic.sendMessage("queue", original, "body");
        assertEquals(Map.of("JMSDeliveryMode", PERSISTENT, "custom", "value"), sentHeaders.get());
        assertEquals("PERSISTENT", original.get("JMSDeliveryMode"));
        assertEquals(123L, original.get("JMSTimestamp"));
    }

    @Test
    void artemisSendsMessagesWithAbsentAndEmptyHeaders(@TempDir Path directory) throws Exception {
        var embedded = new EmbeddedActiveMQ().setConfiguration(new ConfigurationImpl()
                .setPersistenceEnabled(false)
                .setSecurityEnabled(false)
                .setBindingsDirectory(directory.resolve("bindings").toString())
                .setJournalDirectory(directory.resolve("journal").toString())
                .setLargeMessagesDirectory(directory.resolve("large-messages").toString())
                .setPagingDirectory(directory.resolve("paging").toString())
                .setJMXManagementEnabled(false));
        try {
            embedded.start();
            embedded.getActiveMQServer().createQueue(QueueConfiguration.of("queue"));
            var artemis = new ActiveMQArtemis();
            setField(artemis, ActiveMQArtemis.class, "broker", embedded);

            assertNotNull(artemis.sendMessage("queue", null, "body"));
            assertNotNull(artemis.sendMessage("queue", Map.of(), "body"));
            assertEquals("2", artemis.countMessages("queue"));
        } finally {
            embedded.stop();
        }
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }

    private static void setField(Object target, Class<?> owner, String name, Object value) throws Exception {
        var field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
