package org.assimbly.dil.kamelets;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.RouteTemplateDefinition;
import org.apache.camel.model.RouteTemplateParameterDefinition;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Verifies the catalog-derived DIL Kamelets load cleanly as Camel route templates and are indexed. */
class CatalogKameletsTest {

    private static final List<String> KAMELETS = List.of(
            "elasticsearch-action",
            "slack-source",
            "slack-action",
            "telegram-source",
            "telegram-action",
            "azure-servicebus-source",
            "azure-servicebus-action",
            "mongodb-source",
            "mongodb-action",
            "jira-source",
            "jira-action",
            "azure-storage-blob-source",
            "azure-storage-blob-action",
            "dropbox-source",
            "dropbox-action",
            "pulsar-source",
            "pulsar-action",
            "whatsapp-action");

    private DefaultCamelContext context;

    @BeforeEach
    void createContext() {
        context = new DefaultCamelContext();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "elasticsearch-action",
            "slack-source",
            "slack-action",
            "telegram-source",
            "telegram-action",
            "azure-servicebus-source",
            "azure-servicebus-action",
            "mongodb-source",
            "mongodb-action",
            "jira-source",
            "jira-action",
            "azure-storage-blob-source",
            "azure-storage-blob-action",
            "dropbox-source",
            "dropbox-action",
            "pulsar-source",
            "pulsar-action",
            "whatsapp-action"
    })
    void loadsAsDilRouteTemplate(String name) throws Exception {
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/" + name + ".kamelet.yaml"));

        RouteTemplateDefinition template = context.getRouteTemplateDefinition(name);
        assertNotNull(template, "Expected route template with id " + name);
        assertEquals("{{routeConfigurationId}}", template.getRoute().getRouteConfigurationId());

        Set<String> params = template.getTemplateParameters().stream()
                .map(RouteTemplateParameterDefinition::getName)
                .collect(Collectors.toSet());

        assertTrue(params.containsAll(List.of("in", "out", "routeId", "stepId", "routeConfigurationId")),
                "Missing DIL parameters in " + name + ": " + params);
        if (name.endsWith("-action")) {
            assertTrue(params.contains("sink"), "Missing sink parameter in " + name);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "mongodb:mongoClient?hosts=localhost:27017&database=testdb&collection=items&operation=insert&createCollection=true&writeConcern=ACKNOWLEDGED&tls=false",
            "mongodb:mongoClient?hosts=localhost:27017&database=testdb&collection=items&consumerType=tailable&persistentTailTracking=false&fullDocument=default&tls=false",
            "jira:newIssues?jiraUrl=https://example.atlassian.net&delay=6000&maxResults=50&watchedFields=Status,Priority&sendOnlyUpdatedField=true&username=u&password=p",
            "jira:addIssue?jiraUrl=https://example.atlassian.net&username=u&password=p",
            "azure-storage-blob:myaccount/mycontainer?credentialType=SHARED_ACCOUNT_KEY&accessKey=c2VjcmV0&deleteAfterRead=false&moveAfterRead=false&delay=500",
            "azure-storage-blob:myaccount/mycontainer?operation=uploadBlockBlob&blobType=blockblob&credentialType=SHARED_ACCOUNT_KEY&accessKey=c2VjcmV0&createAppendBlob=true&createPageBlob=true&commitBlockListLater=true",
            "dropbox:get?accessToken=tok&apiKey=key&apiSecret=sec&refreshToken=ref&expireIn=14400000&clientIdentifier=app&remotePath=/folder/file.txt",
            "dropbox:put?accessToken=tok&apiKey=key&apiSecret=sec&refreshToken=ref&expireIn=14400000&clientIdentifier=app&remotePath=/folder/file.txt&uploadMode=add",
            "pulsar:persistent://public/default/my-topic?serviceUrl=pulsar://localhost:6650&subscriptionName=subs&subscriptionType=EXCLUSIVE&subscriptionInitialPosition=LATEST&subscriptionMode=DURABLE&numberOfConsumers=1&readCompacted=false&topicsPattern=false",
            "pulsar:persistent://public/default/my-topic?serviceUrl=pulsar://localhost:6650&sendTimeoutMs=30000&batchingEnabled=true&batchingMaxMessages=1000&compressionType=NONE&blockIfQueueFull=false&maxPendingMessages=1000",
            "whatsapp:123456789?authorizationToken=tok&apiVersion=v13.0&baseUri=https://graph.facebook.com&lazyStartProducer=false"
    })
    void resolvesCamelComponentEndpoints(String uri) {
        Endpoint endpoint = context.getEndpoint(uri);
        assertNotNull(endpoint, "Expected Camel endpoint to resolve for URI: " + uri);
    }

    @Test
    void allKameletsAreListedInClasspathIndex() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/kamelets/index.txt")) {
            assertNotNull(is, "/kamelets/index.txt should be generated on the classpath");
            Set<String> indexed = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toSet());

            for (String name : KAMELETS) {
                assertTrue(indexed.contains(name + ".kamelet.yaml"),
                        "Expected " + name + ".kamelet.yaml in /kamelets/index.txt");
            }
        }
    }
}
