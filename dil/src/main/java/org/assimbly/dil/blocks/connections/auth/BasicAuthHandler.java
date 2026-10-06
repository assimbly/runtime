package org.assimbly.dil.blocks.connections.auth;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jetty handler that enforces HTTP Basic authentication for the path of an HTTPS source.
 *
 * <p>Camel adds handlers to the (shared) Jetty server, but doesn't remove them when a flow stops.
 * Reusing one instance per connection makes sure a restarted flow updates the existing handler
 * instead of stacking another security handler that would reject valid credentials.
 */
public class BasicAuthHandler extends Handler.Wrapper {

    private static final Logger log = LoggerFactory.getLogger(BasicAuthHandler.class);

    private static final Map<String, BasicAuthHandler> HANDLERS = new ConcurrentHashMap<>();

    private final String connectionId;

    private volatile String path;
    private volatile boolean matchOnUriPrefix;
    private volatile String username;
    private volatile String password;

    private BasicAuthHandler(String connectionId) {
        this.connectionId = connectionId;
    }

    public static BasicAuthHandler getInstance(String connectionId) {
        return HANDLERS.computeIfAbsent(connectionId, BasicAuthHandler::new);
    }

    public void configure(String path, boolean matchOnUriPrefix, String username, String password) {
        this.path = normalizePath(path);
        this.matchOnUriPrefix = matchOnUriPrefix;
        this.username = username;
        this.password = password;
        log.info("Basic Authentication for connection {}: path={} matchOnUriPrefix={} username={}",
                connectionId, this.path, matchOnUriPrefix, username);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        if (!isProtectedPath(request.getHttpURI().getCanonicalPath())) {
            return super.handle(request, response, callback);
        }

        String authorization = request.getHeaders().get(HttpHeader.AUTHORIZATION);
        if (authorization != null && authorization.regionMatches(true, 0, "Basic ", 0, 6)) {
            String credentials = authorization.substring(6).trim();
            try {
                String decoded = new String(Base64.getDecoder().decode(credentials), StandardCharsets.ISO_8859_1);
                int colon = decoded.indexOf(':');
                if (colon > 0) {
                    String requestUsername = decoded.substring(0, colon);
                    String requestPassword = decoded.substring(colon + 1);
                    if (Objects.equals(username, requestUsername) && Objects.equals(password, requestPassword)) {
                        return super.handle(request, response, callback);
                    }
                }
            } catch (IllegalArgumentException e) {
                // Malformed Base64: fall through to the normal 401 challenge
                log.debug("Basic Authentication for connection {}: malformed Base64 credentials", connectionId);
            }
        }

        response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE, "Basic realm=\"" + connectionId + "\"");
        Response.writeError(request, response, callback, HttpStatus.UNAUTHORIZED_401);
        return true;
    }

    private boolean isProtectedPath(String requestPath) {

        if (requestPath == null || username == null || password == null) {
            return false;
        }

        String normalizedRequestPath = normalizePath(requestPath);

        if (matchOnUriPrefix) {
            return normalizedRequestPath.equals(path) || normalizedRequestPath.startsWith(path + "/");
        }

        return normalizedRequestPath.equals(path);
    }

    private static String normalizePath(String path) {

        if (path == null) {
            return "";
        }

        String normalized = path.startsWith("/") ? path : "/" + path;

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

}
