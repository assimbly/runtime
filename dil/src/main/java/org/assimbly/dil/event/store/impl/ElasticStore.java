package org.assimbly.dil.event.store.impl;

import org.apache.http.HttpHost;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.assimbly.dil.event.domain.Store;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.*;

public class ElasticStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    private final Store store;
    private volatile RestClient restClient;
    private String path;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService initExecutor = Executors.newSingleThreadExecutor();

    public ElasticStore(String collectorId, Store store) {
        this.store = store;
        // Start the async initialization chain
        initElasticsearch();
    }

    private void initElasticsearch() {
        // Run the blocking client creation on a separate thread
        CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Attempting to initialize ES client...");
                String uri = store.getUri();
                URL url = new URL(uri);
                String protocol = url.getProtocol();
                String host = url.getHost();
                int port = url.getPort();
                path = url.getPath();

                // This is a blocking call, but it's on a dedicated thread
                RestClient client = RestClient.builder(new HttpHost(host, port, protocol))
                        .setRequestConfigCallback(cfg -> cfg
                                .setConnectTimeout(5000)
                                .setSocketTimeout(5000)
                                .setConnectionRequestTimeout(5000))
                        .setHttpClientConfigCallback(HttpAsyncClientBuilder::disableAuthCaching)
                        .build();

                // Optional: A quick health check using a blocking call on the same thread
                client.performRequest(new Request("HEAD", "/"));
                log.info("ES client created and responsive.");
                return client;

            } catch (Exception e) {
                log.warn("ES client creation failed: {}", e.getMessage());
                return null;
            }
        }, initExecutor)
        .thenAccept(client -> {
            if (client != null) {
                this.restClient = client;
                log.info("ES initialized successfully.");
            }
        });
    }

    public void store(String json) {
        if (restClient == null) {
            log.warn("ES not initialized, skipping event");
            return;
        }
        try {
            Request request = new Request("POST", path);
            request.setJsonEntity(json);

            restClient.performRequestAsync(request, new ResponseListener() {
                @Override
                public void onSuccess(Response response) {
                    log.debug("Event inserted into ES: {}", json);
                }

                @Override
                public void onFailure(Exception exception) {
                    log.error("Failed to insert into ES: {}", exception.getMessage());
                }
            });
        } catch (Exception e) {
            log.error("Unexpected error sending to ES: {}", e.getMessage());
        }
    }

    public void stop() throws IOException {
        scheduler.shutdown();
        initExecutor.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            if (!initExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                initExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            initExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (restClient != null) {
            restClient.close();
        }
    }
}
