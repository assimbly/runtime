package org.assimbly.integrationrest.testcontainers;

import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;


public class AssimblyGatewayHeadlessContainer {

    private final Network network;
    private GenericContainer<?> gatewayHeadlessContainer;
    private MongoDBContainer mongoContainer;

    public AssimblyGatewayHeadlessContainer() {
        this.network = Network.newNetwork();
    }

    public void init() {
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
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.assimblyEnv)
                .withEnv("MONGO_SECRET_KEY",TestApplicationContext.mongoSecretKey)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort());
        gatewayHeadlessContainer.start();
    }

    public void stop() {
        if (gatewayHeadlessContainer != null) {
            gatewayHeadlessContainer.stop();
        }
        if (mongoContainer != null) {
            mongoContainer.stop();
        }
    }

    public String getBaseUrl() {
        if (gatewayHeadlessContainer == null) {
            throw new IllegalStateException("Container has not been initialized. Call init() first.");
        }
        return "http://" + gatewayHeadlessContainer.getHost() + ":" + gatewayHeadlessContainer.getMappedPort(8088);
    }

    public MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}
