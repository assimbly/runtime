package org.assimbly.commons.utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class HttpUtil {

    private HttpUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static HttpResponse<String> getRequest(String path, Map<String,String> params, Map<String,String> headers) {
        return makeHttpCall(path, "GET", null, params, headers);
    }

    public static HttpResponse<String> postRequest(String path, String body, Map<String,String> params, Map<String,String> headers) {
        return makeHttpCall(path, "POST", body, params, headers);
    }

    public static HttpResponse<String> deleteRequest(String path, String body, Map<String,String> params, Map<String,String> headers) {
        return makeHttpCall(path, "DELETE", body, params, headers);
    }

    private static HttpResponse<String> makeHttpCall(
            String url, String method, Object body, Map<String,String> params, Map<String,String> headers
    ) {

        try(HttpClient client = HttpClient.newHttpClient()) {

            // build query string
            String queryString = buildQueryString(params);
            // include queryString on the url
            String fullUrl = url + queryString;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(fullUrl));

            // add headers
            if(headers != null) {
                headers.forEach(requestBuilder::header);
            }

            HttpRequest.BodyPublisher bodyPublisher;

            bodyPublisher = switch (body) {
                case String s  -> HttpRequest.BodyPublishers.ofString(s);
                case byte[] b  -> HttpRequest.BodyPublishers.ofByteArray(b);
                case null, default -> HttpRequest.BodyPublishers.noBody();
            };

            // set method and body dynamically
            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(bodyPublisher);
                case "PUT" -> requestBuilder.PUT(bodyPublisher);
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.method(method, bodyPublisher);
            }

            return client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String buildQueryString(Map<String,String> params) {
        String queryString = "";
        if(params != null && !params.isEmpty()) {
            queryString = "?" + params.entrySet().stream()
                    .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));
        }
        return queryString;
    }

    public static String extractSecret(String url) throws URISyntaxException {
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();

            String[] queryArr = query.split("\\?", 2);
            query = queryArr[1];

            Map<String, String> params = new ConcurrentHashMap<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                params.put(keyValue[0], keyValue[1]);
            }
            return params.get("secret");
        } catch (Exception _) {
            throw new URISyntaxException(url, "Cannot extract secret");
        }
    }

}
