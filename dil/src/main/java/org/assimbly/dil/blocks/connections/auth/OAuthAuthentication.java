package org.assimbly.dil.blocks.connections.auth;

import org.apache.camel.CamelContext;
import org.eclipse.jetty.ee10.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.server.Handler;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a Jetty SecurityHandler for OAuth 2.0 / OpenID Connect authentication
 * and binds it to the Camel registry under the given connectionId.
 * <p>
 * Required properties (keyed by connectionId):
 *   connection.<id>.oauth.issuerUrl       – The OpenID Provider issuer URL
 *                                           (e.g. https://accounts.google.com)
 *   connection.<id>.oauth.clientId        – The OAuth2 client ID
 *   connection.<id>.oauth.clientSecret    – The OAuth2 client secret (may be encrypted)
 *   connection.<id>.oauth.redirectUri     – The redirect URI registered with the provider
 *                                           (e.g. http://localhost:8080/callback)
 * <p>
 * Optional properties:
 *   connection.<id>.oauth.realmName       – Jetty realm name (default: "OAuthRealm")
 *   connection.<id>.oauth.errorPath       – Path for auth errors (default: "/error")
 */
public class OAuthAuthentication {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEFAULT_REALM = "OAuthRealm";
    private static final String DEFAULT_ERROR_PATH = "/error";

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    // Required fields
    private String issuerUrl;
    private String clientId;
    private String clientSecret;
    private String redirectUri;

    // Optional fields
    private String realmName;
    private String errorPath;
    private String path;

    public OAuthAuthentication(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() throws Exception {

        log.info("Setting OAuth Authentication for connection={}", connectionId);

        setFields();
        validateFields();

        ConstraintSecurityHandler securityHandler = setHandlers();
        addToRegistry(securityHandler, connectionId);
    }

    private void setFields() {

        String prefix = "connection." + connectionId + ".oauth.";

        issuerUrl    = properties.getProperty(prefix + "issuerUrl");
        clientId     = properties.getProperty(prefix + "clientId");
        clientSecret = properties.getProperty(prefix + "clientSecret");
        redirectUri  = properties.getProperty(prefix + "redirectUri");
        redirectUri  = properties.getProperty(prefix + "path","");

        realmName = properties.getProperty(prefix + "realmName", DEFAULT_REALM);
        errorPath = properties.getProperty(prefix + "errorPath", DEFAULT_ERROR_PATH);
    }

    private void validateFields() throws Exception {

        if (issuerUrl == null || issuerUrl.isBlank()) {
            throw new Exception("OAuth Authentication: 'issuerUrl' is required");
        }
        if (clientId == null || clientId.isBlank()) {
            throw new Exception("OAuth Authentication: 'clientId' is required");
        }
        if (clientSecret == null || clientSecret.isBlank()) {
            throw new Exception("OAuth Authentication: 'clientSecret' is required");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new Exception("OAuth Authentication: 'redirectUri' is required");
        }
    }

    private ConstraintSecurityHandler setHandlers() {

        // Build the OpenID Connect configuration.
        // OpenIdConfiguration discovers the provider's endpoints via the issuer URL
        // (it fetches <issuerUrl>/.well-known/openid-configuration automatically).
        OpenIdConfiguration openIdConfig = new OpenIdConfiguration.Builder()
                .issuer(issuerUrl)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        // The authenticator handles the OAuth 2.0 / OIDC redirect flow.
        // redirectUri   – where the provider posts the auth code back
        // errorPath     – where Jetty redirects on authentication failure
        OpenIdAuthenticator authenticator = new OpenIdAuthenticator(openIdConfig, redirectUri, errorPath);

        // Protect all paths by default; callers can narrow this after retrieval
        // from the registry if needed.
        Constraint constraint = new Constraint.Builder()
                .name("auth")
                .roles("**")   // "**" = any authenticated user (Jetty 12 convention)
                .build();

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setConstraint(constraint);
        mapping.setPathSpec(path + "/*");

        ConstraintSecurityHandler securityHandler = new ConstraintSecurityHandler();
        securityHandler.setAuthenticator(authenticator);
        securityHandler.setRealmName(realmName);
        securityHandler.addConstraintMapping(mapping);

        return securityHandler;
    }

    private void addToRegistry(ConstraintSecurityHandler securityHandler, String connectionId) throws Exception {

        context.getRegistry().bind(connectionId, Handler.class, securityHandler);

        Object isRegistered = context.getRegistry().lookupByName(connectionId);

        if (isRegistered != null) {
            log.info("OAuthAuthentication for connection {} is registered", connectionId);
        } else {
            throw new Exception("OAuthAuthentication for connection " + connectionId
                    + " cannot be registered. SecurityHandler is null");
        }
    }
}