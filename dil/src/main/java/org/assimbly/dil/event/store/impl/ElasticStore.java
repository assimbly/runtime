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
import java.util.concurrent.Semaphore;

public final class ElasticStore {

    private static final Logger log = LoggerFactory.getLogger(ElasticStore.class);

    private static RestClient client;

    private static final int MAX_IN_FLIGHT = 100;
    private static final Semaphore IN_FLIGHT = new Semaphore(MAX_IN_FLIGHT);

    private final String path;

    public ElasticStore(Store store) throws Exception {
        synchronized (ElasticStore.class) {
            if (client == null) {
                URL url = new URL(store.getUri());
                client = RestClient.builder(
                                new HttpHost(url.getHost(), url.getPort(), url.getProtocol())
                        )
                        .setHttpClientConfigCallback(b ->
                                b.setMaxConnTotal(30)
                                        .setMaxConnPerRoute(10))
                        .build();
            }
        }
        this.path = new URL(store.getUri()).getPath();
    }

    public void store(String json) {

        if (!IN_FLIGHT.tryAcquire()) {
            log.warn("Dropping event: ES overloaded");
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
            public void onFailure(Exception exception) {
                IN_FLIGHT.release();
                log.error("ES insert failed", exception);
            }
        });
    }
}