package org.assimbly.integrationrest.testcontainers;

import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class AssimblyGatewayHeadlessContainer {

    private static final GenericContainer<?> gatewayHeadlessContainer;
    private static final Network network = Network.newNetwork();
    private static MongoDBContainer mongoContainer;
    private static String tokenId;

    static {
        // initialize mongodb container
        mongoContainer = new MongoDBContainer("mongo:3.3.8")
                .withExposedPorts(27017)
                .withNetwork(network)
                .withNetworkAliases("flux-mongo")
                .waitingFor(Wait.forListeningPort());
        mongoContainer.start();

        // initialize assimbly gateway container
        gatewayHeadlessContainer = new GenericContainer<>("assimbly/gateway-headless:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.ASSIMBLY_ENV)
                .withEnv("MONGO_SECRET_KEY",TestApplicationContext.MONGO_SECRET_KEY)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort());
        gatewayHeadlessContainer.start();
    }

    public static String getBaseUrl() {
        String host = gatewayHeadlessContainer.getHost();
        Integer port = gatewayHeadlessContainer.getMappedPort(8088);
        return "http://" + host + ":" + port;
    }

    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}
