package org.assimbly.dil.blocks.connections.broker;

import org.apache.camel.CamelContext;
import org.apache.camel.component.pulsar.PulsarComponent;
import org.apache.pulsar.client.api.AuthenticationFactory;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulsarConnection {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String serviceUrl;
    private String token;
    private String authenticationClass;
    private String authenticationParams;
    private String tlsTrustCertsFilePath;
    private String allowTlsInsecureConnection;
    private String enableTlsHostnameVerification;

    public PulsarConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() throws Exception {
        setFields();

        if (checkConnection()) {
            log.info("Creating new Pulsar client connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reusing Pulsar client connection with id={}", connectionId);
        }
    }

    private void setFields() {
        String prefix = "connection." + connectionId + ".";
        serviceUrl = firstNonBlank(
                properties.getProperty(prefix + "serviceurl"),
                properties.getProperty(prefix + "url"),
                properties.getProperty(prefix + "uri"));
        token = properties.getProperty(prefix + "token");
        authenticationClass = properties.getProperty(prefix + "authenticationclass");
        authenticationParams = properties.getProperty(prefix + "authenticationparams");
        tlsTrustCertsFilePath = properties.getProperty(prefix + "tlstrustcertsfilepath");
        allowTlsInsecureConnection = properties.getProperty(prefix + "allowtlsinsecureconnection");
        enableTlsHostnameVerification = properties.getProperty(prefix + "enabletlshostnameverification");
    }

    private boolean checkConnection() {
        if (context.getRegistry().lookupByName(connectionId) != null) {
            return false;
        }

        if (isBlank(serviceUrl)) {
            throw new IllegalArgumentException(
                    "pulsar connection parameters are invalid. Broker serviceUrl is required");
        }

        return true;
    }

    private void setConnection() throws Exception {
        ClientBuilder builder = PulsarClient.builder().serviceUrl(serviceUrl);

        if (!isBlank(authenticationClass)) {
            builder.authentication(authenticationClass.trim(), authenticationParams == null ? "" : authenticationParams);
        } else if (!isBlank(token)) {
            builder.authentication(AuthenticationFactory.token(token.trim()));
        }

        if (!isBlank(tlsTrustCertsFilePath)) {
            builder.tlsTrustCertsFilePath(tlsTrustCertsFilePath.trim());
        }
        if (!isBlank(allowTlsInsecureConnection)) {
            builder.allowTlsInsecureConnection(Boolean.parseBoolean(allowTlsInsecureConnection.trim()));
        }
        if (!isBlank(enableTlsHostnameVerification)) {
            builder.enableTlsHostnameVerification(Boolean.parseBoolean(enableTlsHostnameVerification.trim()));
        }

        PulsarClient pulsarClient = builder.build();
        context.getRegistry().bind(connectionId, pulsarClient);

        PulsarComponent pulsarComponent = context.getComponent("pulsar", PulsarComponent.class);
        pulsarComponent.setPulsarClient(pulsarClient);
        log.info("Pulsar client bound to Camel registry and configured on pulsar component with id={}", connectionId);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
