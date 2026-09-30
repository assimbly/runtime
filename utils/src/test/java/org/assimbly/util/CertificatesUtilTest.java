package org.assimbly.util;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class CertificatesUtilTest {

    private static final String PASSWORD = "test-password";

    private static final String GITHUB = "https://www.github.com/";

    private static X509Certificate certificate;
    private static String base64Body;

    private static X509Certificate root;
    private static X509Certificate intermediate;
    private static X509Certificate secondIntermediate;
    private static X509Certificate leaf;
    private static KeyPair leafKeys;

    @BeforeAll
    static void createCertificate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        certificate = CertificatesUtil.selfsignCertificate(keyPair, "SHA256withRSA", "test.assimbly.org", 30);
        base64Body = new String(Base64.getMimeEncoder(64, new byte[]{'\r', '\n'}).encode(certificate.getEncoded()), StandardCharsets.UTF_8);

        KeyPair rootKeys = generator.generateKeyPair();
        KeyPair intermediateKeys = generator.generateKeyPair();
        leafKeys = generator.generateKeyPair();
        root = issue("Test Root CA", rootKeys, "Test Root CA", rootKeys, true);
        intermediate = issue("Test Intermediate CA", intermediateKeys, "Test Root CA", rootKeys, true);
        secondIntermediate = issue("Test Second Intermediate CA", generator.generateKeyPair(), "Test Root CA", rootKeys, true);
        leaf = issue("github.com", leafKeys, "Test Intermediate CA", intermediateKeys, false);
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

    @Test
    void aliasPrefixIsHostWithoutWww() {
        assertThat(CertificatesUtil.certificateAliasPrefix("https://www.github.com/")).isEqualTo("github-com");
        assertThat(CertificatesUtil.certificateAliasPrefix(" https://api.Example.co.uk:8443/x ")).isEqualTo("api-example-co-uk");
        assertThatThrownBy(() -> CertificatesUtil.certificateAliasPrefix("not a url")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectsRootByDefault() throws Exception {
        Map<String, Certificate> selected = CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf, intermediate, root}, null, new X509Certificate[0]);

        assertThat(selected).containsExactly(entry("github-com-root-ca", root));
    }

    @Test
    void selectsIntermediateOrLeaf() throws Exception {
        Certificate[] chain = {leaf, intermediate, root};

        assertThat(CertificatesUtil.selectCertificates(GITHUB, chain, "Intermediate", new X509Certificate[0]))
                .containsExactly(entry("github-com-intermediate-ca", intermediate));
        assertThat(CertificatesUtil.selectCertificates(GITHUB, chain, "leaf", new X509Certificate[0]))
                .containsExactly(entry("github-com-leaf", leaf));
    }

    @Test
    void selectsAllOrderedRootIntermediateLeaf() throws Exception {
        Map<String, Certificate> selected = CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf, intermediate, root}, "all", new X509Certificate[0]);

        assertThat(selected).containsExactly(
                entry("github-com-root-ca", root),
                entry("github-com-intermediate-ca", intermediate),
                entry("github-com-leaf", leaf));
    }

    @Test
    void numbersFurtherIntermediates() throws Exception {
        Map<String, Certificate> selected = CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf, intermediate, secondIntermediate}, "intermediate", new X509Certificate[0]);

        assertThat(selected.keySet()).containsExactly("github-com-intermediate-ca", "github-com-intermediate-ca-2");
    }

    @Test
    void looksUpRootThatServerDidNotSend() throws Exception {
        Map<String, Certificate> selected = CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf, intermediate}, "root", new X509Certificate[]{certificate, root});

        assertThat(selected).containsExactly(entry("github-com-root-ca", root));
    }

    @Test
    void failsWhenRootIsNotFound() {
        assertThatThrownBy(() -> CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf, intermediate}, "root", new X509Certificate[]{certificate}))
                .isInstanceOf(CertificateException.class)
                .hasMessageContaining("github-com");
    }

    @Test
    void failsOnUnknownType() {
        assertThatThrownBy(() -> CertificatesUtil.selectCertificates(GITHUB, new Certificate[]{leaf}, "chain", new X509Certificate[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selfSignedServerCertificateIsStoredOnce() throws Exception {
        Certificate[] chain = {certificate};

        assertThat(CertificatesUtil.selectCertificates(GITHUB, chain, "root", new X509Certificate[0]))
                .containsExactly(entry("github-com-root-ca", certificate));
        assertThat(CertificatesUtil.selectCertificates(GITHUB, chain, "all", new X509Certificate[0]))
                .containsExactly(entry("github-com-root-ca", certificate));
        assertThat(CertificatesUtil.selectCertificates(GITHUB, chain, "leaf", new X509Certificate[0]))
                .containsExactly(entry("github-com-leaf", certificate));
    }

    @Test
    void findTrustAnchorReturnsIssuerOnly() {
        assertThat(CertificatesUtil.findTrustAnchor(intermediate, new X509Certificate[]{certificate, root})).isEqualTo(root);
        assertThat(CertificatesUtil.findTrustAnchor(intermediate, new X509Certificate[]{certificate})).isNull();
    }

    @Test
    void domainUrlAcceptsHostWithOptionalPort() {
        assertThat(CertificatesUtil.domainUrl("www.github.com")).isEqualTo("https://www.github.com/");
        assertThat(CertificatesUtil.domainUrl("api.example.com:8443")).isEqualTo("https://api.example.com:8443/");
        for (String invalid : new String[]{"https://www.github.com/", "github.com/path", "github.com:port", ""}) {
            assertThatThrownBy(() -> CertificatesUtil.domainUrl(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void storeDomainCertificatesRefusesExistingDomainUnlessReplacing(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "other-com-root-ca", "github-com-root-other");
        Map<String, Certificate> all = new LinkedHashMap<>();
        all.put("github-com-root-ca", root);
        all.put("github-com-intermediate-ca", intermediate);
        all.put("github-com-intermediate-ca-2", secondIntermediate);
        all.put("github-com-leaf", leaf);
        CertificatesUtil.storeDomainCertificates(keystorePath, PASSWORD, "github-com", all, false);

        assertThatThrownBy(() -> CertificatesUtil.storeDomainCertificates(keystorePath, PASSWORD, "github-com", Map.of("github-com-root-ca", root), false))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class);

        // renewing with only the root removes the stale intermediates and leaf, other entries stay
        CertificatesUtil.storeDomainCertificates(keystorePath, PASSWORD, "github-com", Map.of("github-com-root-ca", root), true);

        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD))
                .containsOnlyKeys("github-com-root-ca", "other-com-root-ca", "github-com-root-other");
    }

    @Test
    void storeDomainCertificatesKeepsKeyEntries(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("truststore.p12").toString();
        new CertificatesUtil().importIdentity(keystorePath, PASSWORD, p12(leafKeys, leaf), "p12-password", "github-com-leaf", false);

        CertificatesUtil.storeDomainCertificates(keystorePath, PASSWORD, "github-com", Map.of("github-com-root-ca", root), true);

        KeyStore keystore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        assertThat(keystore.isKeyEntry("github-com-leaf")).isTrue();
        assertThat(keystore.containsAlias("github-com-root-ca")).isTrue();
    }

    @Test
    void getAndDeleteDomainCertificatesOnlyTouchThatDomain(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "github-com-root-ca", "github-com-intermediate-ca-3", "github-com-leaf",
                "github-com-root-root-ca", "api-github-com-root-ca", "github-com");

        assertThat(CertificatesUtil.getDomainCertificates(keystorePath, PASSWORD, "github-com"))
                .containsOnlyKeys("github-com-root-ca", "github-com-intermediate-ca-3", "github-com-leaf");

        assertThat(CertificatesUtil.deleteDomainCertificates(keystorePath, PASSWORD, "github-com"))
                .containsOnlyKeys("github-com-root-ca", "github-com-intermediate-ca-3", "github-com-leaf");
        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD))
                .containsOnlyKeys("github-com-root-root-ca", "api-github-com-root-ca", "github-com");
        assertThat(CertificatesUtil.deleteDomainCertificates(keystorePath, PASSWORD, "github-com")).isEmpty();
    }

    @Test
    void expiryOfValidCertificate() throws Exception {
        Instant now = Instant.now();
        X509Certificate expiresIn30Days = expiringAt(now.plus(Duration.ofDays(30)).plusSeconds(60));

        CertificateExpiry expiry = CertificateExpiry.of("valid", expiresIn30Days, now);

        assertThat(expiry.name()).isEqualTo("valid");
        assertThat(expiry.daysUntilExpiry()).isEqualTo(30);
        assertThat(expiry.valid()).isTrue();
        assertThat(expiry.expiresAt()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z");
    }

    @Test
    void expiryOfExpiredCertificate() throws Exception {
        Instant now = Instant.now();
        X509Certificate expired3DaysAgo = expiringAt(now.minus(Duration.ofDays(3)).minusSeconds(60));

        assertThat(CertificateExpiry.of("expired", expired3DaysAgo, now))
                .extracting(CertificateExpiry::daysUntilExpiry, CertificateExpiry::valid)
                .containsExactly(0L, false);

        CertificateExpiry.Expired expired = CertificateExpiry.Expired.of("expired", expired3DaysAgo, now);
        assertThat(expired.daysExpired()).isEqualTo(3);
        assertThat(expired.valid()).isFalse();
        assertThat(expired.expiredAt()).isEqualTo(expired3DaysAgo.getNotAfter().toInstant().toString());
    }

    @Test
    void getCertificatesReturnsAllEntries(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "b", "a");

        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsOnlyKeys("a", "b");
    }

    @Test
    void deleteExpiredCertificatesKeepsValidCertificatesAndKeyEntries(@TempDir Path tempDir) throws Exception {
        Instant now = Instant.now();
        X509Certificate expired = expiringAt(now.minus(Duration.ofDays(1)));

        String keystorePath = createTruststore(tempDir, "valid");
        KeyStore keystore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        keystore.setCertificateEntry("expired", expired);
        KeyPair identityKeys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        keystore.setKeyEntry("expired-identity", identityKeys.getPrivate(), PASSWORD.toCharArray(), new Certificate[]{expiringAt(now.minus(Duration.ofDays(1)), identityKeys)});
        CertificatesUtil.storeKeystore(keystorePath, PASSWORD, keystore);

        Map<String, X509Certificate> deleted = CertificatesUtil.deleteExpiredCertificates(keystorePath, PASSWORD, now);

        assertThat(deleted).containsOnlyKeys("expired");
        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsOnlyKeys("valid", "expired-identity");
    }

    @Test
    void certificateAliasIsCommonName() throws Exception {
        assertThat(CertificatesUtil.certificateAlias(root)).isEqualTo("test-root-ca");
        assertThat(CertificatesUtil.certificateAlias(leaf)).isEqualTo("github-com");
    }

    @Test
    void parsesPemBundleBase64DerAndDataUrl() throws Exception {
        String bundle = CertificatesUtil.convertX509CertificateToPem(root) + "\n" + CertificatesUtil.convertX509CertificateToPem(intermediate);
        String base64Der = Base64.getEncoder().encodeToString(root.getEncoded());
        String dataUrl = "data:application/x-x509-ca-cert;base64," + Base64.getEncoder().encodeToString(bundle.getBytes(StandardCharsets.UTF_8));

        assertThat(CertificatesUtil.parseCertificates(bundle)).containsExactly(root, intermediate);
        assertThat(CertificatesUtil.parseCertificates(base64Der)).containsExactly(root);
        assertThat(CertificatesUtil.parseCertificates(dataUrl)).containsExactly(root, intermediate);
        assertThatThrownBy(() -> CertificatesUtil.parseCertificates("not a certificate")).isInstanceOf(CertificateException.class);
    }

    @Test
    void importTrustedCertificatesStoresBundleUnderNameAndSuffixes(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("truststore.p12").toString();

        assertThat(CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(root, intermediate), "my-ca"))
                .containsExactly(entry("my-ca", root), entry("my-ca-2", intermediate));
        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsOnlyKeys("my-ca", "my-ca-2");
    }

    @Test
    void importTrustedCertificatesIsIdempotentButNeverReplaces(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("truststore.p12").toString();
        CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(root), "my-ca");

        // the same certificate again is fine, a different one under the same name is refused and nothing is stored
        assertThat(CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(root), "my-ca")).containsExactly(entry("my-ca", root));
        assertThatThrownBy(() -> CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(intermediate), "my-ca"))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class);

        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsExactly(entry("my-ca", root));
    }

    @Test
    void importTrustedCertificatesChecksEveryNameBeforeStoring(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("truststore.p12").toString();
        CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(root), "other-ca-2");

        assertThatThrownBy(() -> CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(intermediate, leaf), "other-ca"))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class)
                .hasMessageContaining("other-ca-2");

        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsOnlyKeys("other-ca-2");
    }

    @Test
    void putTrustedCertificateReplacesButNotAnIdentity(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("truststore.p12").toString();
        CertificatesUtil.importTrustedCertificates(keystorePath, PASSWORD, List.of(root), "my-ca");
        new CertificatesUtil().importIdentity(keystorePath, PASSWORD, p12(leafKeys, leaf), "p12-password", "server", false);

        CertificatesUtil.putTrustedCertificate(keystorePath, PASSWORD, "my-ca", intermediate);

        assertThat(CertificatesUtil.getCertificates(keystorePath, PASSWORD)).containsEntry("my-ca", intermediate);
        assertThatThrownBy(() -> CertificatesUtil.putTrustedCertificate(keystorePath, PASSWORD, "server", root))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class);
    }

    @Test
    void validateAliasRejectsInvalidAndReservedNames() {
        CertificatesUtil.validateAlias("github-com-root-ca", Set.of("expired"));
        CertificatesUtil.validateAlias("0e5f4b1a-7f43-4c1e-9d1e-2a7a9c1f3b55", Set.of());
        CertificatesUtil.validateAlias("example.com", Set.of());

        for (String invalid : new String[]{"My CA", "UPPER", "-leading", "trailing-", "a/b", ""}) {
            assertThatThrownBy(() -> CertificatesUtil.validateAlias(invalid, Set.of())).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> CertificatesUtil.validateAlias("expired", Set.of("expired"))).hasMessageContaining("reserved");
    }

    @Test
    void importIdentityStoresPrivateKeyUnderName(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("server-identity.p12").toString();
        String p12 = "data:application/x-pkcs12;base64," + p12(leafKeys, leaf, intermediate);

        Map<String, Certificate> imported = new CertificatesUtil().importIdentity(keystorePath, PASSWORD, p12, "p12-password", "server", false);

        assertThat(imported).containsExactly(entry("server", leaf));
        KeyStore keystore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        assertThat(keystore.isKeyEntry("server")).isTrue();
        assertThat(keystore.getCertificateChain("server")).containsExactly(leaf, intermediate);
    }

    @Test
    void importIdentityRefusesExistingNameButReplaceRenews(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("server-identity.p12").toString();
        CertificatesUtil util = new CertificatesUtil();
        util.importIdentity(keystorePath, PASSWORD, p12(leafKeys, leaf), "p12-password", "server", false);

        assertThatThrownBy(() -> util.importIdentity(keystorePath, PASSWORD, p12(leafKeys, leaf, intermediate), "p12-password", "server", false))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class);
        util.importIdentity(keystorePath, PASSWORD, p12(leafKeys, leaf, intermediate), "p12-password", "server", true);

        KeyStore keystore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        assertThat(keystore.getCertificateChain("server")).hasSize(2);
    }

    @Test
    void replaceIdentityNeedsExactlyOnePrivateKey(@TempDir Path tempDir) throws Exception {
        KeyStore twoKeys = KeyStore.getInstance("PKCS12");
        twoKeys.load(null, null);
        twoKeys.setKeyEntry("1", leafKeys.getPrivate(), "p12-password".toCharArray(), new Certificate[]{leaf});
        twoKeys.setKeyEntry("2", leafKeys.getPrivate(), "p12-password".toCharArray(), new Certificate[]{leaf});
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        twoKeys.store(out, "p12-password".toCharArray());
        String p12 = Base64.getEncoder().encodeToString(out.toByteArray());
        String keystorePath = tempDir.resolve("server-identity.p12").toString();

        assertThatThrownBy(() -> new CertificatesUtil().importIdentity(keystorePath, PASSWORD, p12, "p12-password", "server", true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new CertificatesUtil().importIdentity(keystorePath, PASSWORD, p12, "p12-password", "server", false))
                .containsOnlyKeys("server", "server-2");
    }

    @Test
    void importIdentityWithoutPrivateKeyFails(@TempDir Path tempDir) throws Exception {
        KeyStore certificatesOnly = KeyStore.getInstance("PKCS12");
        certificatesOnly.load(null, null);
        certificatesOnly.setCertificateEntry("root", root);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        certificatesOnly.store(out, "p12-password".toCharArray());
        String p12 = Base64.getEncoder().encodeToString(out.toByteArray());

        assertThatThrownBy(() -> new CertificatesUtil().importIdentity(tempDir.resolve("server-identity.p12").toString(), PASSWORD, p12, "p12-password", "server", false))
                .isInstanceOf(KeyStoreException.class);
    }

    @Test
    void importP12CertificateReturnsTheIdentitysOwnCertificate(@TempDir Path tempDir) throws Exception {
        Map<String, Certificate> imported = new CertificatesUtil().importP12Certificate(tempDir.resolve("server-identity.p12").toString(), PASSWORD, p12(leafKeys, leaf, intermediate), "p12-password");

        assertThat(imported).containsExactly(entry("1", leaf));
    }

    @Test
    void generateIdentityStoresPrivateKey(@TempDir Path tempDir) throws Exception {
        String keystorePath = tempDir.resolve("server-identity.p12").toString();

        Map<String, Certificate> generated = CertificatesUtil.generateIdentity(keystorePath, PASSWORD, "myserver.example.com", null);

        assertThat(generated).containsOnlyKeys("myserver-example-com");
        KeyStore keystore = CertificatesUtil.loadKeyStore(keystorePath, PASSWORD, "PKCS12");
        assertThat(keystore.isKeyEntry("myserver-example-com")).isTrue();
        assertThat(keystore.getKey("myserver-example-com", PASSWORD.toCharArray())).isNotNull();
    }

    @Test
    void generateIdentityRefusesExistingName(@TempDir Path tempDir) throws Exception {
        String keystorePath = createTruststore(tempDir, "server");

        assertThatThrownBy(() -> CertificatesUtil.generateIdentity(keystorePath, PASSWORD, "myserver.example.com", "server"))
                .isInstanceOf(CertificatesUtil.AliasExistsException.class);
    }

    /** A base64 encoded PKCS12 file with one private key entry (alias "1"). */
    private static String p12(KeyPair keys, X509Certificate... chain) throws Exception {
        KeyStore p12 = KeyStore.getInstance("PKCS12");
        p12.load(null, null);
        p12.setKeyEntry("1", keys.getPrivate(), "p12-password".toCharArray(), chain);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p12.store(out, "p12-password".toCharArray());
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static X509Certificate expiringAt(Instant notAfter) throws Exception {
        return expiringAt(notAfter, KeyPairGenerator.getInstance("RSA").generateKeyPair());
    }

    private static X509Certificate expiringAt(Instant notAfter, KeyPair keys) throws Exception {
        return issue("expiry.test", keys, "expiry.test", keys, false, notAfter);
    }

    private static X509Certificate issue(String cn, KeyPair subjectKeys, String issuerCn, KeyPair issuerKeys, boolean ca) throws Exception {
        return issue(cn, subjectKeys, issuerCn, issuerKeys, ca, Instant.now().plus(Duration.ofDays(30)));
    }

    private static X509Certificate issue(String cn, KeyPair subjectKeys, String issuerCn, KeyPair issuerKeys, boolean ca, Instant notAfter) throws Exception {
        Instant now = Instant.now();
        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=" + issuerCn),
                BigInteger.valueOf(now.toEpochMilli()).add(BigInteger.valueOf(cn.hashCode() & 0xffff)),
                Date.from(now.minus(Duration.ofDays(60))),
                Date.from(notAfter),
                new X500Name("CN=" + cn),
                subjectKeys.getPublic())
                .addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(issuerKeys.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
