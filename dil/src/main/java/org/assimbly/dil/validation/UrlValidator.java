package org.assimbly.dil.validation;

import org.assimbly.util.error.ValidationErrorMessage;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlValidator {

    private static final int DEFAULT_TIMEOUT = 5000;

    /**
     * User-Agents to try for the validation button. Starts with Apache HttpClient to stay close to
     * Camel HTTPS runtime, then falls back when a site blocks or stalls on that agent.
     */
    private static final List<String> DEFAULT_USER_AGENTS = List.of(
            "Apache-HttpClient/5.4 (Java/21)",
            "curl/8.7.1"
    );

    private static final ValidationErrorMessage UNREACHABLE_ERROR = new ValidationErrorMessage("Url is not reachable from the server!");
    private static final ValidationErrorMessage INVALID_URL_ERROR = new ValidationErrorMessage("Url is not valid!");

    private final int timeoutMs;
    private final List<String> userAgents;

    public UrlValidator() {
        this(DEFAULT_TIMEOUT, DEFAULT_USER_AGENTS);
    }

    UrlValidator(int timeoutMs, List<String> userAgents) {
        this.timeoutMs = timeoutMs;
        this.userAgents = userAgents;
    }

    public ValidationErrorMessage validate(String url) {
        try {
            String decodedUrl = URLDecoder.decode(url, StandardCharsets.UTF_8);

            if (!validateURL(decodedUrl)) {
                return INVALID_URL_ERROR;
            }

            Exception lastFailure = null;
            for (String userAgent : userAgents) {
                try {
                    attemptConnection(decodedUrl, userAgent);
                    return null;
                } catch (UnknownHostException e) {
                    return UNREACHABLE_ERROR;
                } catch (SocketTimeoutException e) {
                    lastFailure = e;
                } catch (IOException e) {
                    lastFailure = e;
                }
            }

            if (lastFailure != null) {
                return new ValidationErrorMessage(lastFailure.getMessage());
            }
            return UNREACHABLE_ERROR;
        } catch (Exception e) {
            return new ValidationErrorMessage(e.getMessage());
        }
    }

    private void attemptConnection(String url, String userAgent) throws IOException {
        HttpURLConnection httpUrlConn = (HttpURLConnection) new URL(url).openConnection();
        try {
            httpUrlConn.setRequestMethod("HEAD");
            httpUrlConn.setConnectTimeout(timeoutMs);
            httpUrlConn.setReadTimeout(timeoutMs);
            httpUrlConn.setRequestProperty("User-Agent", userAgent);
            httpUrlConn.connect();
            // Trigger the request; throws when no connection can be established / times out
            httpUrlConn.getResponseMessage();
        } finally {
            httpUrlConn.disconnect();
        }
    }

    public static final boolean validateURL(String url) {
        Pattern regex = Pattern.compile("^(https?):\\/\\/[-a-zA-Z0-9+&@#\\/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#\\/%=~_|]");
        Matcher matcher = regex.matcher(url);
        return matcher.find();
    }
}
