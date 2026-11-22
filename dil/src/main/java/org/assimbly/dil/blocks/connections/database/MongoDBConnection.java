package org.assimbly.dil.blocks.connections.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.camel.CamelContext;
import org.jasypt.properties.EncryptableProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MongoDBConnection {

    private static final int DEFAULT_PORT = 27017;
    private static final String DEFAULT_AUTH_SOURCE = "admin";

    protected Logger log = LoggerFactory.getLogger(getClass());

    private final CamelContext context;
    private final EncryptableProperties properties;
    private final String connectionId;

    private String uri;
    private String hosts;
    private String host;
    private String port;
    private String username;
    private String password;
    private String authSource;
    private boolean tls;

    public MongoDBConnection(CamelContext context, EncryptableProperties properties, String connectionId) {
        this.context = context;
        this.properties = properties;
        this.connectionId = connectionId;
    }

    public void start() {
        setFields();

        if (checkConnection()) {
            log.info("Creating new MongoDB client connection with id={}", connectionId);
            setConnection();
        } else {
            log.info("Reusing MongoDB client connection with id={}", connectionId);
        }
    }

    private void setFields() {
        String prefix = "connection." + connectionId + ".";
        uri = firstNonBlank(
                properties.getProperty(prefix + "uri"),
                properties.getProperty(prefix + "url"),
                properties.getProperty(prefix + "connectionstring"));
        hosts = properties.getProperty(prefix + "hosts");
        host = properties.getProperty(prefix + "host");
        port = properties.getProperty(prefix + "port");
        username = properties.getProperty(prefix + "username");
        password = properties.getProperty(prefix + "password");
        authSource = firstNonBlank(
                properties.getProperty(prefix + "authsource"),
                properties.getProperty(prefix + "authenticationdatabase"),
                DEFAULT_AUTH_SOURCE);
        String tlsValue = firstNonBlank(
                properties.getProperty(prefix + "tls"),
                properties.getProperty(prefix + "ssl"));
        tls = Boolean.parseBoolean(tlsValue);
    }

    private boolean checkConnection() {
        if (context.getRegistry().lookupByName(connectionId) != null) {
            return false;
        }

        if (isBlank(uri) && isBlank(hosts) && isBlank(host)) {
            throw new IllegalArgumentException(
                    "mongodb connection parameters are invalid. Connection uri, hosts, or host is required");
        }

        return true;
    }

    private void setConnection() {
        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder();

        if (!isBlank(uri)) {
            settingsBuilder.applyConnectionString(new ConnectionString(uri));
        } else {
            List<ServerAddress> addresses = parseServerAddresses();
            settingsBuilder.applyToClusterSettings(builder -> builder.hosts(addresses));

            if (!isBlank(username) && password != null) {
                MongoCredential credential = MongoCredential.createCredential(
                        username, authSource, password.toCharArray());
                settingsBuilder.credential(credential);
            }

            if (tls) {
                settingsBuilder.applyToSslSettings(builder -> builder.enabled(true));
            }
        }

        MongoClient mongoClient = MongoClients.create(settingsBuilder.build());
        context.getRegistry().bind(connectionId, mongoClient);
        log.info("MongoDB client bound to Camel registry with connection id={}", connectionId);
    }

    private List<ServerAddress> parseServerAddresses() {
        List<ServerAddress> addresses = new ArrayList<>();

        if (!isBlank(hosts)) {
            for (String entry : hosts.split(",")) {
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) {
                    addresses.add(parseSingleAddress(trimmed));
                }
            }
        } else {
            int resolvedPort = isBlank(port) ? DEFAULT_PORT : Integer.parseInt(port.trim());
            addresses.add(new ServerAddress(host.trim(), resolvedPort));
        }

        if (addresses.isEmpty()) {
            throw new IllegalArgumentException(
                    "mongodb connection parameters are invalid. No valid host addresses found");
        }

        return addresses;
    }

    private static ServerAddress parseSingleAddress(String hostPort) {
        int colonIdx = hostPort.lastIndexOf(':');
        if (colonIdx > 0 && colonIdx < hostPort.length() - 1) {
            String hostPart = hostPort.substring(0, colonIdx).trim();
            int portPart = Integer.parseInt(hostPort.substring(colonIdx + 1).trim());
            return new ServerAddress(hostPart, portPart);
        }
        return new ServerAddress(hostPort, DEFAULT_PORT);
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
