package org.assimbly.dil.blocks.connections.auth;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.X509TrustManager;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Jetty handler that enforces mutual TLS for the path of an HTTPS source.
 *
 * The TLS connector requests (but doesn't require) a client certificate, because the port is shared by all flows.
 * This handler validates the client certificate for the path of one flow against the trusted certificates of its connection.
 */
public class MutualTlsHandler extends Handler.Wrapper {

    private static final Logger log = LoggerFactory.getLogger(MutualTlsHandler.class);

    // Camel adds handlers to the (shared) Jetty server, but doesn't remove them when a flow stops.
    // Reusing one instance per connection makes sure a restarted flow updates the existing handler instead of adding a second one.
    private static final Map<String, MutualTlsHandler> HANDLERS = new ConcurrentHashMap<>();

    private final String connectionId;

    private volatile String path;
    private volatile boolean matchOnUriPrefix;
    private volatile X509TrustManager trustManager;

    private MutualTlsHandler(String connectionId) {
        this.connectionId = connectionId;
    }

    public static MutualTlsHandler getInstance(String connectionId) {
        return HANDLERS.computeIfAbsent(connectionId, MutualTlsHandler::new);
    }

    public void configure(String path, boolean matchOnUriPrefix, X509TrustManager trustManager) {
        this.path = normalizePath(path);
        this.matchOnUriPrefix = matchOnUriPrefix;
        this.trustManager = trustManager;
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {

        if (!isProtectedPath(request.getHttpURI().getCanonicalPath())) {
            return super.handle(request, response, callback);
        }

        X509Certificate[] chain = null;
        if (request.getAttribute(EndPoint.SslSessionData.ATTRIBUTE) instanceof EndPoint.SslSessionData sslSessionData) {
            chain = sslSessionData.peerCertificates();
        }

        if (chain == null || chain.length == 0) {
            log.warn("Mutual TLS for connection {}: request to {} rejected, no client certificate provided", connectionId, request.getHttpURI().getPath());
            Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403, "Client certificate required");
            return true;
        }

        try {
            trustManager.checkClientTrusted(chain, chain[0].getPublicKey().getAlgorithm());
        } catch (CertificateException e) {
            log.warn("Mutual TLS for connection {}: request to {} rejected, client certificate subject={} issuer={} is not trusted. Reason: {}",
                    connectionId, request.getHttpURI().getPath(), chain[0].getSubjectX500Principal(), chain[0].getIssuerX500Principal(), e.getMessage());
            Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403, "Client certificate not trusted");
            return true;
        }

        return super.handle(request, response, callback);

    }

    private boolean isProtectedPath(String requestPath) {

        if (requestPath == null || trustManager == null) {
            return false;
        }

        String normalizedRequestPath = normalizePath(requestPath);

        if (matchOnUriPrefix) {
            return normalizedRequestPath.equals(path) || normalizedRequestPath.startsWith(path + "/");
        }

        return normalizedRequestPath.equals(path);

    }

    // Removes the trailing slash, so /path and /path/ are treated the same
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
