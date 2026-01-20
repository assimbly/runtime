package org.assimbly.dil.event.store.impl;

import org.apache.http.HttpHost;
import org.assimbly.dil.event.domain.Store;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class ElasticStore {

    private static volatile RestClient client;

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    // Background scheduler for retries (prevents blocking main threads)
    private static final ScheduledExecutorService RETRY_SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    // Concurrency control
    private static final int MAX_IN_FLIGHT = 100;
    private static final Semaphore IN_FLIGHT = new Semaphore(MAX_IN_FLIGHT);

    // Retry Configuration
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_DELAY_MS = 1000;

    private final String path;

    public ElasticStore(Store store) throws Exception {
        if (client == null) {
            synchronized (ElasticStore.class) {
                if (client == null) {
                    URL url = new URL(store.getUri());
                    client = RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()))
                            .setRequestConfigCallback(c -> c
                                    .setConnectTimeout(3000)   // 3s to connect
                                    .setSocketTimeout(10000))  // 10s for data
                            .setHttpClientConfigCallback(b -> b
                                    .setMaxConnTotal(MAX_IN_FLIGHT)
                                    .setMaxConnPerRoute(MAX_IN_FLIGHT))
                            .build();
                }
            }
        }
        this.path = new URL(store.getUri()).getPath();
    }

    public void store(String json) {
        // Step 1: Try to get a permit. If full, don't wait (don't stuck).
        if (!IN_FLIGHT.tryAcquire()) {
            log.warn("Dropping event: ES overloaded");
            return;
        }

        // Step 2: Start the async request chain
        sendToElasticWithRetry(json, 0);
    }

    private void sendToElasticWithRetry(String json, int attempt) {
        Request req = new Request("POST", path);
        req.setJsonEntity(json);

        client.performRequestAsync(req, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                // Success: Release the slot and we are done
                IN_FLIGHT.release();
            }

            @Override
            public void onFailure(Exception ex) {
                if (attempt < MAX_RETRIES) {
                    // Exponential Backoff: 1000 * 2^attempt (1s, 2s, 4s, 8s, 16s)
                    long delay = INITIAL_DELAY_MS * (long) Math.pow(2, attempt);

                    // Schedule retry on background thread
                    RETRY_SCHEDULER.schedule(() -> sendToElasticWithRetry(json, attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    // All retries failed: Release slot so others can use it
                    IN_FLIGHT.release();
                    log.error("[ES-GIVEUP] Failed after " + (MAX_RETRIES + 1) + " attempts. Event lost.");
                }
            }
        });
    }
}