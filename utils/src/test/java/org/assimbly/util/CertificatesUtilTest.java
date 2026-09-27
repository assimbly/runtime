package org.assimbly.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CertificatesUtilTest {

    private static final String PASSWORD = "test-password";

    private static X509Certificate certificate;
    private static String base64Body;

    @BeforeAll
    static void createCertificate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        certificate = CertificatesUtil.selfsignCertificate(keyPair, "SHA256withRSA", "test.assimbly.org", 30);
        base64Body = new String(Base64.getMimeEncoder(64, new byte[]{'\r', '\n'}).encode(certificate.getEncoded()), StandardCharsets.UTF_8);
    }

    @Test
    void roundTrip() throws Exception {
        String pem = CertificatesUtil.convertX509CertificateToPem(certificate);

        assertThat(CertificatesUtil.convertPemToX509Certificate(pem)).isEqualTo(certificate);
    }

    @Test
    void pemWithoutLineBreakBeforeFooter() throws Exception {
        // format stored by earlier versions of convertX509CertificateToPem
        String pem = "-----BEGIN CERTIFICATE-----\n" + base64Body + "-----END CERTIFICATE-----";

        assertThat(CertificatesUtil.convertPemToX509Certificate(pem)).isEqualTo(certificate);
    }

    @Test
    void standardPemWithUnixLineEndings() throws Exception {
        String pem = "-----BEGIN CERTIFICATE-----\n" + base64Body.replace("\r\n", "\n") + "\n-----END CERTIFICATE-----\n";

        assertThat(CertificatesUtil.convertPemToX509Certificate(pem)).isEqualTo(certificate);
    }

    @Test
    void deleteCertificateRemovesOnlyThatAlias(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "keep", "remove");

        assertThat(new CertificatesUtil().deleteCertificate(keystorePath, PASSWORD, "remove")).isTrue();

        KeyStore truststore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        assertThat(truststore.containsAlias("remove")).isFalse();
        assertThat(truststore.containsAlias("keep")).isTrue();
    }

    @Test
    void deleteUnknownCertificateReturnsFalse(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "keep");

        assertThat(new CertificatesUtil().deleteCertificate(keystorePath, PASSWORD, "unknown")).isFalse();

        assertThat(CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12").containsAlias("keep")).isTrue();
    }

    private static String createTruststore(Path dir, String... aliases) throws Exception {
        KeyStore truststore = KeyStore.getInstance("PKCS12");
        truststore.load(null, PASSWORD.toCharArray());
        for (String alias : aliases) {
            truststore.setCertificateEntry(alias, certificate);
        }
        String keystorePath = dir.resolve("truststore.p12").toString();
        CertificatesUtil.storeKeystore(keystorePath, PASSWORD, truststore);
        return keystorePath;
    }

    @Test
    void emptyPemReturnsNull() throws Exception {
        assertThat(CertificatesUtil.convertPemToX509Certificate("  ")).isNull();
        assertThat(CertificatesUtil.convertPemToX509Certificate(null)).isNull();
    }
}
