package org.assimbly.integrationrest.testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class AssimblyGatewayHeadlessContainer {

    public static String ASSIMBLY_ENV = "ASSIMBLY_ENV";
    public static String MONGO_SECRET_KEY = "c3RlbXNldmVyeXRoaW5ncmVhbHdoaWNoZWZmb3J0b2ZmaWNlc3RpZmZjYWtlZ2VuZXJhbGVsZWN0cmljbWFpbA==";

    private static final GenericContainer<?> gatewayContainer;
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
        gatewayContainer = new GenericContainer<>("assimbly/gateway-headless:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", ASSIMBLY_ENV)
                .withEnv("MONGO_SECRET_KEY",MONGO_SECRET_KEY)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort());
        gatewayContainer.start();
    }

    public static String getBaseUrl() {
        String host = gatewayContainer.getHost();
        Integer port = gatewayContainer.getMappedPort(8088);
        return "http://" + host + ":" + port;
    }

    public static MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}
