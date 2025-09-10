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
import java.net.ConnectException;
import java.net.URL;
import java.util.concurrent.*;

public class ElasticStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    private final Store store;
    private volatile RestClient restClient;
    private String path;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();

    private volatile boolean isRunning = true;

    private static final int CONNECTION_MAX_RETRIES = 5;
    private static final long CONNECTION_INITIAL_DELAY_SECONDS = 30;

    private static final int POST_MAX_RETRIES = 3;
    private static final long POST_INITIAL_DELAY_MS = 1000;

    private int connectionRetryCount = 0;
    private long currentConnectionDelay = CONNECTION_INITIAL_DELAY_SECONDS;

    public ElasticStore(String collectorId, Store store) {
        this.store = store;
        // Start the background consumer thread
        scheduler.submit(this::processEvents);
    }

    private void processEvents() {
        while (isRunning) {
            try {
                // Wait for a connection to be available
                ensureConnection();
                String json = eventQueue.take();

                // Once connected, attempt to post the event with retries
                postToElasticsearchWithRetry(json, 0);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Event processor interrupted. Shutting down.");
                break;
            }
        }
    }

    private void ensureConnection() throws InterruptedException {
        while (restClient == null && isRunning) {
            try {
                log.info("Attempting to establish ElasticSearch connection... (Attempt {} of {})", connectionRetryCount + 1, CONNECTION_MAX_RETRIES);
                String uri = store.getUri();
                URL url = new URL(uri);
                String protocol = url.getProtocol();
                String host = url.getHost();
                int port = url.getPort();
                path = url.getPath();

                RestClient client = RestClient.builder(new HttpHost(host, port, protocol))
                        .setRequestConfigCallback(cfg -> cfg
                                .setConnectTimeout(5000)
                                .setSocketTimeout(5000)
                                .setConnectionRequestTimeout(5000))
                        .setHttpClientConfigCallback(HttpAsyncClientBuilder::disableAuthCaching)
                        .build();

                client.performRequest(new Request("HEAD", "/"));
                log.info("ElasticSearch client created and responsive.");
                this.restClient = client;
                connectionRetryCount = 0; // Reset counter on success
                currentConnectionDelay = CONNECTION_INITIAL_DELAY_SECONDS;
                return;

            } catch (Exception e) {
                log.warn("ElasticSearch connection failed", e);
                if (connectionRetryCount < CONNECTION_MAX_RETRIES) {
                    connectionRetryCount++;
                    log.info("Retrying connection in {} seconds...", currentConnectionDelay);
                    TimeUnit.SECONDS.sleep(currentConnectionDelay);
                    currentConnectionDelay *= 2;
                } else {
                    log.error("Failed to connect after {} retries. Events will be queued.", CONNECTION_MAX_RETRIES);
                    // To avoid a tight loop, we will sleep for the maximum backoff time.
                    TimeUnit.SECONDS.sleep(currentConnectionDelay / 2);
                }
            }
        }
    }

    public void store(String json) {
        try {
            eventQueue.put(json);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to queue event for ElasticSearch", e);
        }
    }

    private void postToElasticsearchWithRetry(String json, int attempt) {
        if (restClient == null) {
            log.warn("Connection lost. Re-queuing event for later");
            try {
                eventQueue.put(json);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Failed to re-queue event", e);
            }
            return;
        }

        // Log the current attempt number before making the request
        log.info("Attempting to post event (Attempt {} of {}).", attempt + 1, POST_MAX_RETRIES + 1);

        Request request = new Request("POST", path);
        request.setJsonEntity(json);

        restClient.performRequestAsync(request, new ResponseListener() {
            @Override
            public void onSuccess(Response response) {
                log.info("Event inserted into ElasticSearch");
            }

            @Override
            public void onFailure(Exception exception) {
                log.error("Failed to insert into ElasticSearch: {}", exception.getMessage());

                if (isConnectionError(exception)) {
                    log.warn("Connection lost. Re-queuing event and initiating reconnection.");
                    restClient = null;
                    try {
                        eventQueue.put(json);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Failed to re-queue event after connection lost.", e);
                    }
                } else if (attempt < POST_MAX_RETRIES && isRetryable(exception)) {
                    long delay = POST_INITIAL_DELAY_MS * (long) Math.pow(2, attempt);
                    log.warn("Retrying post in {}ms.", delay);
                    scheduler.schedule(() -> postToElasticsearchWithRetry(json, attempt + 1), delay, TimeUnit.MILLISECONDS);
                } else {
                    log.error("Failed to insert into ElasticSearch after {} attempts. Giving up on this event.", attempt + 1);
                }
            }
        });
    }

    private boolean isRetryable(Exception e) {
        return e instanceof ConnectException || e instanceof IOException;
    }

    private boolean isConnectionError(Exception e) {
        return e instanceof ConnectException || (e.getMessage() != null && e.getMessage().contains("Connection refused"));
    }

    public void stop() throws IOException {
        isRunning = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (restClient != null) {
            restClient.close();
        }
    }
}