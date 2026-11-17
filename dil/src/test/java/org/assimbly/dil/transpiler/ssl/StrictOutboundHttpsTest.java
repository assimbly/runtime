package org.assimbly.dil.transpiler.ssl;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class StrictOutboundHttpsTest {

    private static final String PASSWORD = "supersecret";

    @TempDir
    Path dir;

    private HttpsServer server;
    private DefaultCamelContext context;

    @AfterEach
    void stop() {
        if (context != null) {
            context.stop();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void callSucceedsOnlyAfterServerCertificateIsAddedToTruststore() throws Exception {

        Path identity = dir.resolve("server-identity.p12");
        Path truststore = dir.resolve("outbound-truststore.p12");

        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.createSSLContextParameters(identity.toString(), PASSWORD, truststore.toString(), PASSWORD);

        int port = startServer(identity);

        context = new DefaultCamelContext();
        context.getRegistry().bind("strictOutbound", sslConfiguration.createStrictOutboundSSLContextParameters(identity.toString(), PASSWORD, truststore.toString(), PASSWORD));
        context.getRegistry().bind("noopHostnameVerifier", NoopHostnameVerifier.INSTANCE);
        context.start();

        ProducerTemplate template = context.createProducerTemplate();
        // Not localhost: it also resolves to ::1, and the http client reports the refused connection of that last address instead of the TLS failure
        String uri = "https://127.0.0.1:" + port + "/?sslContextParameters=#strictOutbound&x509HostnameVerifier=#noopHostnameVerifier&throwExceptionOnFailure=false";

        Exception exception = template.request(uri, exchange -> { }).getException();
        assertNotNull(exception);
        assertInstanceOf(SSLException.class, exception, exception.toString());

        addServerCertificateToTruststore(identity, truststore);

        Exchange exchange = template.request(uri, e -> { });
        assertNull(exchange.getException());
        assertEquals(200, exchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class));
    }

    private int startServer(Path identity) throws Exception {

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(load(identity), PASSWORD.toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

        server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext));
        server.createContext("/", httpExchange -> {
            httpExchange.sendResponseHeaders(200, -1);
            httpExchange.close();
        });
        server.start();

        return server.getAddress().getPort();
    }

    private void addServerCertificateToTruststore(Path identity, Path truststore) throws Exception {

        Certificate certificate = load(identity).getCertificate("localhost");

        KeyStore keystore = load(truststore);
        keystore.setCertificateEntry("localhost", certificate);
        try (OutputStream os = Files.newOutputStream(truststore)) {
            keystore.store(os, PASSWORD.toCharArray());
        }
        Files.setLastModifiedTime(truststore, FileTime.from(Instant.now().plus(1, ChronoUnit.MINUTES)));
    }

    private KeyStore load(Path path) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(path)) {
            keystore.load(is, PASSWORD.toCharArray());
        }
        return keystore;
    }

}
