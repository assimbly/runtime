package org.assimbly.integrationrest.testcontainers;

import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.URL;
import java.nio.file.Paths;


public class AssimblyGatewayHeadlessContainer {

    private static final Logger log = LoggerFactory.getLogger(AssimblyGatewayHeadlessContainer.class);

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
                .waitingFor(Wait.forListeningPort())
                .withFileSystemBind(getResourceSecurityPath(), "/data/.assimbly/security/")
                .withFileSystemBind("/tmp/jeka", "/data/.jeka"); // bind a writable directory to prevent AccessDeniedException on /data/.jeka
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

    private static String getResourceSecurityPath() {
        URL resource = AssimblyGatewayHeadlessContainer.class.getClassLoader().getResource("security");
        try {
            return Paths.get(resource.toURI()).toString();
        } catch (Exception e) {
            log.error("Error to copy files to container", e);
            return null;
        }
    }

    public String buildBrokerApiPath(String path) {
        if (gatewayHeadlessContainer == null) {
            throw new IllegalStateException("Container has not been initialized. Call init() first.");
        }
        return "http://" + gatewayHeadlessContainer.getHost() + ":" + gatewayHeadlessContainer.getMappedPort(8088) + path;
    }

    public MongoDBContainer getMongoContainer() {
        return mongoContainer;
    }
}
