package org.assimbly.dil.transpiler.ssl;

import org.assimbly.util.CertificatesUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SSLConfigurationTest {

    private static final String PASSWORD = "supersecret";

    @TempDir
    Path dir;

    @Test
    void newIdentityStoreGetsSelfSignedLocalhostIdentity() throws Exception {

        Path identity = dir.resolve("server-identity.p12");
        Path truststore = dir.resolve("outbound-truststore.p12");

        new SSLConfiguration().createSSLContextParameters(identity.toString(), PASSWORD, truststore.toString(), PASSWORD);

        KeyStore keystore = load(identity);
        assertTrue(keystore.isKeyEntry("localhost"));
        assertEquals("CN=localhost", ((X509Certificate) keystore.getCertificate("localhost")).getSubjectX500Principal().getName());

        assertEquals(0, load(truststore).size());
    }

    @Test
    void existingIdentityStoreIsLeftAlone() throws Exception {

        Path identity = dir.resolve("server-identity.p12");
        Path truststore = dir.resolve("outbound-truststore.p12");

        SSLConfiguration sslConfiguration = new SSLConfiguration();
        sslConfiguration.createKeystore(identity.toString());

        sslConfiguration.createSSLContextParameters(identity.toString(), PASSWORD, truststore.toString(), PASSWORD);

        assertEquals(0, load(identity).size());
    }

    @Test
    void emptyTruststoreRejectsUnknownServerWithCertificateException() throws Exception {

        Path truststore = dir.resolve("outbound-truststore.p12");
        new SSLConfiguration().createKeystore(truststore.toString());

        InboundClientAcceptingTrustManager trustManager = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD);

        X509Certificate unknown = selfSigned("unknown.example");
        assertThrows(CertificateException.class, () -> trustManager.checkServerTrusted(new X509Certificate[]{unknown}, "RSA"));
    }

    @Test
    void trustsServerCertificateAddedToTruststore() throws Exception {

        Path truststore = dir.resolve("outbound-truststore.p12");
        new SSLConfiguration().createKeystore(truststore.toString());

        InboundClientAcceptingTrustManager trustManager = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD);

        X509Certificate server = selfSigned("server.example");
        X509Certificate[] chain = {server};
        assertThrows(CertificateException.class, () -> trustManager.checkServerTrusted(chain, "RSA"));

        KeyStore keystore = load(truststore);
        keystore.setCertificateEntry("server.example", server);
        try (OutputStream os = Files.newOutputStream(truststore)) {
            keystore.store(os, PASSWORD.toCharArray());
        }
        Files.setLastModifiedTime(truststore, FileTime.from(Instant.now().plus(1, ChronoUnit.MINUTES)));

        assertDoesNotThrow(() -> trustManager.checkServerTrusted(chain, "RSA"));
    }

    @Test
    void strictTrustManagerRejectsServerWhenTruststoreIsEmpty() throws Exception {

        Path truststore = dir.resolve("outbound-truststore.p12");
        new SSLConfiguration().createKeystore(truststore.toString());

        InboundClientAcceptingTrustManager trustManager = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD, false);

        X509Certificate server = selfSigned("server.example");
        CertificateException exception = assertThrows(CertificateException.class, () -> trustManager.checkServerTrusted(new X509Certificate[]{server}, "RSA"));
        assertTrue(exception.getMessage().contains("has no certificates"));
    }

    @Test
    void strictTrustManagerTrustsServerCertificateAddedToTruststore() throws Exception {

        Path truststore = dir.resolve("outbound-truststore.p12");
        new SSLConfiguration().createKeystore(truststore.toString());

        InboundClientAcceptingTrustManager trustManager = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD, false);

        X509Certificate server = selfSigned("server.example");
        X509Certificate[] chain = {server};
        addToKeystore(truststore, server);

        assertDoesNotThrow(() -> trustManager.checkServerTrusted(chain, "RSA"));
    }

    @Test
    void onlyDefaultTrustManagerTrustsTheJdkDefaultCas() throws Exception {

        Path truststore = dir.resolve("outbound-truststore.p12");
        new SSLConfiguration().createKeystore(truststore.toString());
        addToKeystore(truststore, selfSigned("other.example"));

        X509Certificate server = selfSigned("server.example");
        X509Certificate[] chain = {server};

        // Use a JDK default truststore that trusts the server, instead of cacerts
        Path jdkDefault = dir.resolve("jdk-default.p12");
        new SSLConfiguration().createKeystore(jdkDefault.toString());
        addToKeystore(jdkDefault, server);

        String trustStore = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
        try {
            System.setProperty("javax.net.ssl.trustStore", jdkDefault.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);
            System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");

            InboundClientAcceptingTrustManager defaultCas = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD);
            InboundClientAcceptingTrustManager strict = new InboundClientAcceptingTrustManager(truststore.toString(), PASSWORD, false);

            assertDoesNotThrow(() -> defaultCas.checkServerTrusted(chain, "RSA"));
            assertThrows(CertificateException.class, () -> strict.checkServerTrusted(chain, "RSA"));
        } finally {
            restoreProperty("javax.net.ssl.trustStore", trustStore);
            restoreProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
            restoreProperty("javax.net.ssl.trustStoreType", trustStoreType);
        }
    }

    private void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name);
        } else {
            System.setProperty(name, value);
        }
    }

    private void addToKeystore(Path path, X509Certificate certificate) throws Exception {
        KeyStore keystore = load(path);
        keystore.setCertificateEntry(certificate.getSubjectX500Principal().getName(), certificate);
        try (OutputStream os = Files.newOutputStream(path)) {
            keystore.store(os, PASSWORD.toCharArray());
        }
    }

    private KeyStore load(Path path) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(path)) {
            keystore.load(is, PASSWORD.toCharArray());
        }
        return keystore;
    }

    private X509Certificate selfSigned(String cn) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return (X509Certificate) CertificatesUtil.selfsignCertificate2(keyPair, cn);
    }

}
