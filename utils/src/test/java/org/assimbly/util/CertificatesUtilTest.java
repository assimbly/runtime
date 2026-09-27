package org.assimbly.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class CertificatesUtilTest {

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
    void emptyPemReturnsNull() throws Exception {
        assertThat(CertificatesUtil.convertPemToX509Certificate("  ")).isNull();
        assertThat(CertificatesUtil.convertPemToX509Certificate(null)).isNull();
    }
}
