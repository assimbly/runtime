package org.assimbly.integrationrest.testcontainers;

import org.assimbly.integrationrest.utils.TestApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
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
    private GenericContainer<?> wireMockContainer;

    public AssimblyGatewayHeadlessContainer() {
        this.network = Network.newNetwork();
    }

    public void init() {

        // to prevent the "Prematurely reached end of stream" error on the mongo container, it will be started only once
        if(mongoContainer == null) {
            // initialize mongodb container
            mongoContainer = new MongoDBContainer("mongo:3.3.8")
                    .withExposedPorts(27017)
                    .withNetwork(network)
                    .withNetworkAliases("flux-mongo")
                    .waitingFor(Wait.forListeningPort());
            mongoContainer.start();
        }

        if (wireMockContainer == null) {
            // initialize wireMock container
            wireMockContainer = new GenericContainer<>("wiremock/wiremock:latest")
                    .withExposedPorts(8080)
                    .withNetwork(network)
                    .withNetworkAliases("www.dneonline.com")
                    .withClasspathResourceMapping("wiremock", "/home/wiremock", BindMode.READ_ONLY) // this maps your mock definitions
                    .withCommand("--port 8080")
                    .waitingFor(Wait.forListeningPort());
            wireMockContainer.start();
        }

        // initialize assimbly gateway container
        gatewayHeadlessContainer = new GenericContainer<>("assimbly/gateway-headless:development")
                .withExposedPorts(8088)
                .withNetwork(network)
                .withEnv("ASSIMBLY_ENV", TestApplicationContext.assimblyEnv)
                .withEnv("MONGO_SECRET_KEY",TestApplicationContext.mongoSecretKey)
                .waitingFor(Wait.forLogMessage(".*Assimbly is running!.*", 1))
                .waitingFor(Wait.forListeningPort())
                .withFileSystemBind(getResourceSecurityPath(), "/data/.assimbly/security/", BindMode.READ_WRITE)
                .withFileSystemBind("/tmp/jeka", "/data/.jeka", BindMode.READ_WRITE); // bind a writable directory to prevent AccessDeniedException on /data/.jeka
        gatewayHeadlessContainer.start();
    }

    public void stop() {
        if (gatewayHeadlessContainer != null) {
            gatewayHeadlessContainer.stop();
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
