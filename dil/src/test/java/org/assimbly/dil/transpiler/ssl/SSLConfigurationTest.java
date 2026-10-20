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
