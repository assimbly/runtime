package org.assimbly.dil.event.store.impl;

import org.assimbly.dil.event.domain.Store;
import org.elasticsearch.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public final class ElasticStore {

    private static RestClient client;

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    // Background scheduler for retries
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
                    URI uri = URI.create(store.getUri());

                    // FIX 1: Use the legacy HttpHost (org.apache.http.HttpHost)
                    // Most ES RestClient versions are still bound to the 4.x HttpHost type.
                    org.apache.http.HttpHost host = new org.apache.http.HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());

                    client = RestClient.builder(host)
                            .setRequestConfigCallback(builder -> builder
                                    // FIX 2: Use int (milliseconds) instead of (int, TimeUnit)
                                    .setConnectTimeout(1000)
                                    .setSocketTimeout(3000))
                            .setHttpClientConfigCallback(httpClientBuilder -> {
                                return httpClientBuilder
                                        .setMaxConnTotal(MAX_IN_FLIGHT)
                                        .setMaxConnPerRoute(MAX_IN_FLIGHT);
                            })
                            .build();
                }
            }
        }
        this.path = URI.create(store.getUri()).getPath();
    }

    public void store(String json) {
        storeInternal(json, 0);
    }

    private void storeInternal(String json, int attempt) {
        try {
            IN_FLIGHT.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Store operation interrupted", e);
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
                IN_FLIGHT.release();

                if (attempt < MAX_RETRIES) {
                    long delay = INITIAL_DELAY_MS * (long) Math.pow(2, attempt);
                    RETRY_SCHEDULER.schedule(() -> storeInternal(json, attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    log.error("[ES-GIVEUP] Failed after " + (MAX_RETRIES + 1) + " attempts.", ex);
                }
            }
        });
    }
}