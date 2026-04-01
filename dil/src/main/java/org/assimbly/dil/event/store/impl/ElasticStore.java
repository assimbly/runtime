package org.assimbly.dil.event.store.impl;

import java.util.concurrent.*;

import org.elasticsearch.client.*;
import org.assimbly.dil.event.domain.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.atomic.AtomicReference;

public final class ElasticStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    // AtomicReference is more idiomatic than volatile + synchronized for lazy init
    private static final AtomicReference<RestClient> CLIENT_REF = new AtomicReference<>();

    // Daemon threads so the JVM can exit cleanly without explicit shutdown
    private static final ScheduledExecutorService RETRY_SCHEDULER =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "elastic-retry");
                t.setDaemon(true);
                return t;
            });

    private static final int MAX_IN_FLIGHT   = 100;
    private static final Semaphore IN_FLIGHT = new Semaphore(MAX_IN_FLIGHT);
    private static final int MAX_RETRIES     = 10;
    private static final long INITIAL_DELAY_MS = 1_000L;

    private final String path;

    public ElasticStore(Store store) {
        URI uri = URI.create(store.getUri());

        // compareAndSet: only the first thread to win the race builds the client
        if (CLIENT_REF.get() == null) {
            RestClient candidate = buildClient(uri);
            if (!CLIENT_REF.compareAndSet(null, candidate)) {
                // Another thread already set it — close the one we just built
                closeQuietly(candidate);
            }
        }

        this.path = uri.getPath();
    }

    private static RestClient buildClient(URI uri) {
        org.apache.http.HttpHost host =
                new org.apache.http.HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());

        return RestClient.builder(host)
                .setRequestConfigCallback(b -> b
                        .setConnectTimeout(1_000)
                        .setSocketTimeout(3_000))
                .setHttpClientConfigCallback(b -> b
                        .setMaxConnTotal(MAX_IN_FLIGHT)
                        .setMaxConnPerRoute(MAX_IN_FLIGHT))
                .build();
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

        CLIENT_REF.get().performRequestAsync(req, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                IN_FLIGHT.release();
            }

            @Override
            public void onFailure(Exception ex) {
                IN_FLIGHT.release();
                if (attempt < MAX_RETRIES) {
                    long delay = INITIAL_DELAY_MS * (1L << attempt); // bit-shift is cleaner than Math.pow
                    log.warn("[ES-RETRY] Attempt {}/{} failed, retrying in {}ms", attempt + 1, MAX_RETRIES, delay, ex);
                    RETRY_SCHEDULER.schedule(() -> storeInternal(json, attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    log.error("[ES-GIVEUP] Failed after {} attempts", MAX_RETRIES + 1, ex);
                }
            }
        });
    }

    private static void closeQuietly(RestClient c) {
        try {
            c.close();
        } catch (Exception e) {
            log.warn("Failed to close redundant ElasticSearch client during init race", e);
        }
    }

}