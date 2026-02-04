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
    private static final ScheduledExecutorService RETRY_SCHEDULER = Executors.newScheduledThreadPool(2);

    // Concurrency control
    private static final int MAX_IN_FLIGHT = 100;
    private static final Semaphore IN_FLIGHT = new Semaphore(MAX_IN_FLIGHT);

    // Retry Configuration
    private static final int MAX_RETRIES = 10;
    private static final long INITIAL_DELAY_MS = 1000;

    private final String path;

    public ElasticStore(Store store) throws Exception {
        if (client == null) {
            synchronized (ElasticStore.class) {
                if (client == null) {
                    URL url = new URL(store.getUri());
                    client = RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()))
                            .setRequestConfigCallback(c -> c
                                    .setConnectTimeout(1000)
                                    .setSocketTimeout(3000))
                            .setHttpClientConfigCallback(b -> b
                                    .setMaxConnTotal(MAX_IN_FLIGHT)
                                    .setMaxConnPerRoute(MAX_IN_FLIGHT)
                            .setDefaultIOReactorConfig(org.apache.http.impl.nio.reactor.IOReactorConfig.custom()
                                    .setIoThreadCount(Math.max(1, Runtime.getRuntime().availableProcessors() / 2))
                                    .build()))
                            .build();
                }
            }
        }
        this.path = new URL(store.getUri()).getPath();
    }

    public void store(String json) {
        storeInternal(json, 0);
    }

    private void storeInternal(String json, int attempt) {
        try {
            // Change from tryAcquire() to acquire()
            // This makes the background thread wait until a permit is free
            IN_FLIGHT.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Request req = new Request("POST", path);
        req.setJsonEntity(json);

        client.performRequestAsync(req, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                IN_FLIGHT.release();
            }

            @Override
            public void onFailure(Exception ex) {
                // RELEASE IMMEDIATELY so the slot is available for others
                IN_FLIGHT.release();

                if (attempt < MAX_RETRIES) {
                    long delay = INITIAL_DELAY_MS * (long) Math.pow(2, attempt);
                    // Schedule retry - it will call storeInternal and try to get a new permit
                    RETRY_SCHEDULER.schedule(() -> storeInternal(json, attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    log.error("[ES-GIVEUP] Failed after " + (MAX_RETRIES + 1) + " attempts.");
                }
            }
        });
    }
}