package org.assimbly.dil.kamelets;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteConfigurationBuilder;
import org.apache.camel.builder.TemplatedRouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.PluginHelper;
import org.apache.camel.support.ResourceHelper;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests the PGP encrypt-action and decrypt-action Kamelets using Camel registry references and file/base64 URIs. */
class PgpKameletsTest {

    private static final String RECIPIENT_ID = "recipient@example.com";
    private static final String RECIPIENT_PASSWORD = "secret-passphrase";
    private static final String SENDER_ID = "sender@example.com";
    private static final String SENDER_PASSWORD = "sender-passphrase";
    private static final String PAYLOAD = "<order><id>42</id><item>Widget</item></order>";

    private static KeyMaterial recipientKeys;
    private static KeyMaterial unprotectedKeys;
    private static KeyMaterial senderKeys;

    private CamelContext context;

    @BeforeAll
    static void generateKeys() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        recipientKeys = generateKeyMaterial(RECIPIENT_ID, RECIPIENT_PASSWORD.toCharArray());
        unprotectedKeys = generateKeyMaterial("nopass@example.com", new char[0]);
        senderKeys = generateKeyMaterial(SENDER_ID, SENDER_PASSWORD.toCharArray());
    }

    @BeforeEach
    void startContext() throws Exception {
        context = new DefaultCamelContext();
        context.addRoutes(new RouteConfigurationBuilder() {
            @Override
            public void configuration() {
                routeConfiguration("0");
            }
        });
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/encrypt-action.kamelet.yaml"));
        PluginHelper.getRoutesLoader(context).loadRoutes(
                ResourceHelper.resolveMandatoryResource(context, "classpath:kamelets/decrypt-action.kamelet.yaml"));
        context.start();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void encryptAndDecryptWithMinimalConfigurationUsingRegistryRefs() throws Exception {
        context.getRegistry().bind("recipientPublicKey", recipientKeys.armoredPublicKey());
        context.getRegistry().bind("recipientPrivateKey", recipientKeys.armoredSecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-encrypt-route")
                .parameter("routeId", "pgp-encrypt-route")
                .parameter("stepId", "1")
                .parameter("in", "direct:encrypt")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPublicKey")
                .parameter("keyUserid", RECIPIENT_ID)
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-decrypt-route")
                .parameter("routeId", "pgp-decrypt-route")
                .parameter("stepId", "2")
                .parameter("in", "direct:decrypt")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPrivateKey")
                .parameter("password", RECIPIENT_PASSWORD)
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            byte[] encrypted = template.requestBody("direct:encrypt", PAYLOAD, byte[].class);
            assertFalse(new String(encrypted, StandardCharsets.UTF_8).contains("<order>"));

            String decrypted = template.requestBody("direct:decrypt", encrypted, String.class);
            assertEquals(PAYLOAD, decrypted);
        }
    }

    @Test
    void encryptArmoredProducesAsciiArmoredBlockAndDecrypts() throws Exception {
        context.getRegistry().bind("recipientPublicKey", recipientKeys.armoredPublicKey());
        context.getRegistry().bind("recipientPrivateKey", recipientKeys.armoredSecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-encrypt-armored-route")
                .parameter("routeId", "pgp-encrypt-armored-route")
                .parameter("stepId", "1")
                .parameter("in", "direct:encrypt-armored")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPublicKey")
                .parameter("keyUserid", RECIPIENT_ID)
                .parameter("armored", "true")
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-decrypt-armored-route")
                .parameter("routeId", "pgp-decrypt-armored-route")
                .parameter("stepId", "2")
                .parameter("in", "direct:decrypt-armored")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPrivateKey")
                .parameter("password", RECIPIENT_PASSWORD)
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            String armored = template.requestBody("direct:encrypt-armored", PAYLOAD, String.class);
            assertTrue(armored.startsWith("-----BEGIN PGP MESSAGE-----"), armored);
            assertFalse(armored.contains("<order>"));

            String decrypted = template.requestBody("direct:decrypt-armored", armored, String.class);
            assertEquals(PAYLOAD, decrypted);
        }
    }

    @Test
    void decryptWorksWithoutPasswordForUnprotectedPrivateKey() throws Exception {
        context.getRegistry().bind("unprotectedPublicKey", unprotectedKeys.armoredPublicKey());
        context.getRegistry().bind("unprotectedPrivateKey", unprotectedKeys.armoredSecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-encrypt-nopass")
                .parameter("routeId", "pgp-encrypt-nopass")
                .parameter("stepId", "1")
                .parameter("in", "direct:encrypt-nopass")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:unprotectedPublicKey")
                .parameter("keyUserid", "nopass@example.com")
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-decrypt-nopass")
                .parameter("routeId", "pgp-decrypt-nopass")
                .parameter("stepId", "2")
                .parameter("in", "direct:decrypt-nopass")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:unprotectedPrivateKey")
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            byte[] encrypted = template.requestBody("direct:encrypt-nopass", PAYLOAD, byte[].class);
            String decrypted = template.requestBody("direct:decrypt-nopass", encrypted, String.class);
            assertEquals(PAYLOAD, decrypted);
        }
    }

    @Test
    void encryptAndDecryptSupportsFileAndBase64KeyringUris(@TempDir Path tempDir) throws Exception {
        Path pubFile = tempDir.resolve("pubring.gpg");
        Files.write(pubFile, recipientKeys.binaryPublicKey());
        String base64SecKey = Base64.getEncoder().encodeToString(recipientKeys.binarySecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-encrypt-file")
                .parameter("routeId", "pgp-encrypt-file")
                .parameter("stepId", "1")
                .parameter("in", "direct:encrypt-file")
                .parameter("sink", "true")
                .parameter("keyFileName", "file:" + pubFile.toAbsolutePath())
                .parameter("keyUserid", RECIPIENT_ID)
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-decrypt-base64")
                .parameter("routeId", "pgp-decrypt-base64")
                .parameter("stepId", "2")
                .parameter("in", "direct:decrypt-base64")
                .parameter("sink", "true")
                .parameter("keyFileName", "base64:" + base64SecKey)
                .parameter("password", RECIPIENT_PASSWORD)
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            byte[] encrypted = template.requestBody("direct:encrypt-file", PAYLOAD, byte[].class);
            String decrypted = template.requestBody("direct:decrypt-base64", encrypted, String.class);
            assertEquals(PAYLOAD, decrypted);
        }
    }

    @Test
    void encryptWithSignatureAndDecryptWithRequiredVerification() throws Exception {
        context.getRegistry().bind("recipientPublicKey", recipientKeys.armoredPublicKey());
        context.getRegistry().bind("recipientPrivateKey", recipientKeys.armoredSecretKey());
        context.getRegistry().bind("senderPublicKey", senderKeys.armoredPublicKey());
        context.getRegistry().bind("senderPrivateKey", senderKeys.armoredSecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-sign-encrypt")
                .parameter("routeId", "pgp-sign-encrypt")
                .parameter("stepId", "1")
                .parameter("in", "direct:sign-encrypt")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPublicKey")
                .parameter("keyUserid", RECIPIENT_ID)
                .parameter("signatureKeyFileName", "ref:senderPrivateKey")
                .parameter("signatureKeyUserid", SENDER_ID)
                .parameter("signaturePassword", SENDER_PASSWORD)
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-verify-decrypt")
                .parameter("routeId", "pgp-verify-decrypt")
                .parameter("stepId", "2")
                .parameter("in", "direct:verify-decrypt")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPrivateKey")
                .parameter("password", RECIPIENT_PASSWORD)
                .parameter("signatureKeyFileName", "ref:senderPublicKey")
                .parameter("signatureKeyUserid", SENDER_ID)
                .parameter("signatureVerificationOption", "required")
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            byte[] encrypted = template.requestBody("direct:sign-encrypt", PAYLOAD, byte[].class);
            String decrypted = template.requestBody("direct:verify-decrypt", encrypted, String.class);
            assertEquals(PAYLOAD, decrypted);
        }
    }

    @Test
    void decryptFailsWithWrongPassword() throws Exception {
        context.getRegistry().bind("recipientPublicKey", recipientKeys.armoredPublicKey());
        context.getRegistry().bind("recipientPrivateKey", recipientKeys.armoredSecretKey());

        TemplatedRouteBuilder.builder(context, "encrypt-action")
                .routeId("pgp-encrypt-wrongpw")
                .parameter("routeId", "pgp-encrypt-wrongpw")
                .parameter("stepId", "1")
                .parameter("in", "direct:encrypt-wrongpw")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPublicKey")
                .parameter("keyUserid", RECIPIENT_ID)
                .add();

        TemplatedRouteBuilder.builder(context, "decrypt-action")
                .routeId("pgp-decrypt-wrongpw")
                .parameter("routeId", "pgp-decrypt-wrongpw")
                .parameter("stepId", "2")
                .parameter("in", "direct:decrypt-wrongpw")
                .parameter("sink", "true")
                .parameter("keyFileName", "ref:recipientPrivateKey")
                .parameter("password", "wrong-password")
                .add();

        try (ProducerTemplate template = context.createProducerTemplate()) {
            byte[] encrypted = template.requestBody("direct:encrypt-wrongpw", PAYLOAD, byte[].class);
            assertThrows(CamelExecutionException.class,
                    () -> template.requestBody("direct:decrypt-wrongpw", encrypted, String.class));
        }
    }

    private record KeyMaterial(
            byte[] binaryPublicKey,
            byte[] binarySecretKey,
            String armoredPublicKey,
            String armoredSecretKey) {
    }

    private static KeyMaterial generateKeyMaterial(String identity, char[] passphrase) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        PGPKeyPair pgpKeyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, new Date());
        PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA1);

        PBESecretKeyEncryptor secretKeyEncryptor = passphrase.length == 0
                ? null
                : new JcePBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha1Calc)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build(passphrase);

        PGPKeyRingGenerator generator = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                pgpKeyPair,
                identity,
                sha1Calc,
                null,
                null,
                new JcaPGPContentSignerBuilder(pgpKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME),
                secretKeyEncryptor);

        PGPPublicKeyRing publicKeyRing = generator.generatePublicKeyRing();
        PGPSecretKeyRing secretKeyRing = generator.generateSecretKeyRing();

        byte[] binaryPublic = publicKeyRing.getEncoded();
        byte[] binarySecret = secretKeyRing.getEncoded();

        return new KeyMaterial(binaryPublic, binarySecret, armor(binaryPublic), armor(binarySecret));
    }

    private static String armor(byte[] encoded) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ArmoredOutputStream armored = new ArmoredOutputStream(out)) {
            armored.write(encoded);
        }
        return out.toString(StandardCharsets.UTF_8);
    }
}
