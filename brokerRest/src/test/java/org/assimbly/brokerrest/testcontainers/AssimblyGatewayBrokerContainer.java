package org.assimbly.brokerrest.testcontainers;

import org.assimbly.brokerrest.utils.TestApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URL;
import java.nio.file.Paths;

public class AssimblyGatewayBrokerContainer {

    private static final Logger log = LoggerFactory.getLogger(AssimblyGatewayBrokerContainer.class);

    private static final GenericContainer<?> gatewayBrokerContainer;
    private static final GenericContainer<?> gatewayHeadlessContainer;
    private static final Network network = Network.newNetwork();

    static {
        // initialize assimbly gateway broker container
        gatewayBrokerContainer = new GenericContainer<>("assimbly/gateway-broker:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.ASSIMBLY_ENV)
                .withEnv("MONGO_SECRET_KEY", TestApplicationContext.MONGO_SECRET_KEY)
                .withEnv("ASSIMBLY_BROKER_JMX_PORT", TestApplicationContext.ASSIMBLY_BROKER_JMX_PORT)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort())
                .withFileSystemBind(getResourcePath(), "/data/.assimbly/");
//                .withLogConsumer(new Slf4jLogConsumer(log));
        gatewayBrokerContainer.start();

        // initialize assimbly gateway headless container
        gatewayHeadlessContainer = new GenericContainer<>("assimbly/gateway-headless:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.ASSIMBLY_ENV)
                .withEnv("MONGO_SECRET_KEY",TestApplicationContext.MONGO_SECRET_KEY)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort());
//                .withLogConsumer(new Slf4jLogConsumer(log));
        gatewayHeadlessContainer.start();
    }

    private static String getResourcePath() {
        URL resource = AssimblyGatewayBrokerContainer.class.getClassLoader().getResource("container");
        try {
            return Paths.get(resource.toURI()).toString();
        } catch (Exception e) {
            log.error("Error to copy files to container", e);
            return null;
        }
    }

    public static String getBrokerBaseUrl() {
        String host = gatewayBrokerContainer.getHost();
        Integer port = gatewayBrokerContainer.getMappedPort(8088);
        return "http://" + host + ":" + port;
    }

    public static String getHeadlessBaseUrl() {
        String host = gatewayHeadlessContainer.getHost();
        Integer port = gatewayHeadlessContainer.getMappedPort(8088);
        return "http://" + host + ":" + port;
    }

    public static void waitFor(String expLog, int numTimes) {
        gatewayHeadlessContainer.waitingFor(Wait.forLogMessage(expLog, numTimes));
    }
}
