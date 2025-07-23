package org.assimbly.dil.event.store.impl;

import org.apache.http.HttpHost;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class ElasticStore {

    protected Logger log = LoggerFactory.getLogger(getClass());

    RestClient restClient;
    private final org.assimbly.dil.event.domain.Store store;
    private String path;

    public ElasticStore(org.assimbly.dil.event.domain.Store store) {

        this.store = store;

        try {
            start();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

    }

    public void store(String json) {

        Request request = new Request("POST", path);
        request.setJsonEntity(json);

        restClient.performRequestAsync(request,
                new ResponseListener() {
                    @Override
                    public void onSuccess(Response response) {
                        log.debug("Insert into elasticsearch. json={}", json);
                    }

                    @Override
                    public void onFailure(Exception e) {
                        log.error("Failed to store event into elasticsearch. Reason: {}", e.getMessage(), e);
                    }
                });
    }

    public void start() throws MalformedURLException {

        URI uri = URI.create(store.getUri());
        URL url = uri.toURL();
        String protocol = url.getProtocol();
        String host = url.getHost();
        int port = url.getPort();
        path = url.getPath();

        log.info("Start elasticsearch client for url: {}", uri);

        restClient = RestClient.builder(new HttpHost(host, port, protocol)).build();
    }

    public void stop() throws IOException {

        String uri = store.getUri();

        log.info("Stop elasticsearch client for url: {}", uri);

        restClient.close();
    }

    public boolean isRunning() {
        return restClient.isRunning();
    }


}
