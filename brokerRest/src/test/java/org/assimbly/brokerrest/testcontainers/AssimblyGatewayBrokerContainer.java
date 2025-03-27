package org.assimbly.brokerrest.testcontainers;

import org.assimbly.brokerrest.utils.TestApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URL;
import java.nio.file.Paths;

public class AssimblyGatewayBrokerContainer {

    private static final Logger log = LoggerFactory.getLogger(AssimblyGatewayBrokerContainer.class);

    private final Network network;
    private GenericContainer<?> gatewayBrokerContainer;
    private GenericContainer<?> gatewayHeadlessContainer;

    public AssimblyGatewayBrokerContainer() {
        this.network = Network.newNetwork();
    }

    public void init() {
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
        gatewayBrokerContainer.start();

        // initialize assimbly gateway headless container
        gatewayHeadlessContainer = new GenericContainer<>("assimbly/gateway-headless:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.ASSIMBLY_ENV)
                .withEnv("MONGO_SECRET_KEY",TestApplicationContext.MONGO_SECRET_KEY)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort());
        gatewayHeadlessContainer.start();
    }

    public void stop() {
        if (gatewayBrokerContainer != null) {
            gatewayBrokerContainer.stop();
        }
        if (gatewayHeadlessContainer != null) {
            gatewayHeadlessContainer.stop();
        }
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

    public String getBrokerBaseUrl() {
        if (gatewayBrokerContainer == null) {
            throw new IllegalStateException("Container has not been initialized. Call init() first.");
        }
        return "http://" + gatewayBrokerContainer.getHost() + ":" + gatewayBrokerContainer.getMappedPort(8088);
    }

    public String getHeadlessBaseUrl() {
        if (gatewayHeadlessContainer == null) {
            throw new IllegalStateException("Container has not been initialized. Call init() first.");
        }
        return "http://" + gatewayHeadlessContainer.getHost() + ":" + gatewayHeadlessContainer.getMappedPort(8088);
    }

}
