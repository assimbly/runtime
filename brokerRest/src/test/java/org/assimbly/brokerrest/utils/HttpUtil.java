package org.assimbly.brokerrest.utils;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpUtil {

    public static HttpResponse<String> makeHttpCall(
            String url, String method, String body, HashMap<String,String> params, HashMap<String,String> headers
    ) {

        try {
            HttpClient client = HttpClient.newHttpClient();

            // build query string
            String queryString = buildQueryString(params);
            // include queryString on the url
            String fullUrl = url + queryString;

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(URI.create(fullUrl));

            // add headers
            headers.forEach(requestBuilder::header);

            // set method and body dynamically
            switch (method.toUpperCase()) {
                case "GET" -> requestBuilder.GET();
                case "POST" -> requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                case "PUT" -> requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(body));
                case "DELETE" -> requestBuilder.DELETE();
                default -> requestBuilder.method(method, body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = client.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            return response;

        } catch (Exception e) {
            return null;
        }
    }

    private static String buildQueryString(HashMap<String,String> params) {
        String queryString = params != null && !params.isEmpty()
                ? "?" + params.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"))
                : "";

        return queryString;
    }

    public static String extractSecret(String url) throws Exception {
        URI uri = new URI(url);
        String query = uri.getQuery();

        String[] queryArr = query.split("\\?", 2);
        query = queryArr[1];

        Map<String, String> params = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            params.put(keyValue[0], keyValue[1]);
        }
        return params.get("secret");
    }

}
