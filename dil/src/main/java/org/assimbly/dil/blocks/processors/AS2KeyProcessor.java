package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.AS2CompressionAlgorithm;

import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class AS2KeyProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {

        // Message structure
        String messageStructure = exchange.getProperty("messageStructure", String.class);

        // Password and alias
        String password = exchange.getProperty("keyPassword", String.class);
        String alias = exchange.getProperty("keyAlias", String.class);

        // Certificate for encryption
        URI encryptCertificateUri = getOptionalUri(
                exchange.getProperty("certificateForEncrypt", String.class)
        );
        boolean isToEncrypt = encryptCertificateUri != null;

        // Certificate for decryption
        URI decryptCertificateUri = getOptionalUri(
                exchange.getProperty("certificateForDecrypt", String.class)
        );
        boolean isToDecrypt = decryptCertificateUri != null
                && hasText(password)
                && hasText(alias);

        // Certificate for signing
        URI signingCertificateUri = getOptionalUri(
                exchange.getProperty("certificateForSigning", String.class)
        );
        boolean isSigned = signingCertificateUri != null
                && hasText(password)
                && hasText(alias);

        if (isSigned) {
            // Signing certificate chain
            Certificate[] signingCertificateChain =
                    getCertificateFromP12(signingCertificateUri, password, alias);
            exchange.getMessage().setHeader(
                    "CamelAs2.signingCertificateChain",
                    signingCertificateChain
            );

            // Signing private key
            PrivateKey privateKey =
                    getPrivateKey(signingCertificateUri, password, alias);
            exchange.getMessage().setHeader(
                    "CamelAs2.signingPrivateKey",
                    privateKey
            );
        }

        if (isToEncrypt) {
            // Encrypting certificate chain
            Certificate[] encryptingCertificateChain =
                    getCertificateChainFromX509(encryptCertificateUri);
            exchange.getMessage().setHeader(
                    "CamelAs2.encryptingCertificateChain",
                    encryptingCertificateChain
            );
        }

        if (isToDecrypt) {
            // Decrypting private key
            PrivateKey decryptingPrivateKey =
                    getPrivateKey(decryptCertificateUri, password, alias);
            exchange.getMessage().setHeader(
                    "CamelAs2.decryptingPrivateKey",
                    decryptingPrivateKey
            );
        }

        if (messageStructure != null && messageStructure.contains("COMPRESSED")) {
            // Compression algorithm
            exchange.getMessage().setHeader(
                    "CamelAs2.compressionAlgorithm",
                    AS2CompressionAlgorithm.ZLIB
            );
        }
    }

    // Public static methods used by AS2 inbound step

    public static String getSigningAlgorithm(Certificate[] certArr) {
        if (certArr == null || certArr.length == 0 || certArr[0] == null) {
            throw new IllegalArgumentException("Certificate chain must contain at least one certificate.");
        }

        if (!(certArr[0] instanceof X509Certificate x509Cert)) {
            throw new IllegalArgumentException("Certificate must be an X.509 certificate.");
        }

        return x509Cert.getSigAlgName().toUpperCase();
    }

    public static Certificate[] getSigningCertificateChain(
            URI certificateUri,
            String password,
            String alias) throws Exception {

        requireUri(certificateUri, "Signing certificate URI");
        requireText(password, "Keystore password");
        requireText(alias, "Keystore alias");

        return getCertificateFromP12(certificateUri, password, alias);
    }

    public static Certificate[] getValidateSigningCertificateChain(
            URI certificateUri) throws Exception {

        requireUri(certificateUri, "Signing certificate URI");

        return getCertificateChainFromX509(certificateUri);
    }

    public static PrivateKey getSigningPrivateKey(
            URI certificateUri,
            String password,
            String alias) throws Exception {

        requireUri(certificateUri, "Signing certificate URI");
        requireText(password, "Keystore password");
        requireText(alias, "Keystore alias");

        return getPrivateKey(certificateUri, password, alias);
    }

    public static PrivateKey getDecryptingPrivateKey(
            URI certificateUri,
            String password,
            String alias) throws Exception {

        requireUri(certificateUri, "Decrypting certificate URI");
        requireText(password, "Keystore password");
        requireText(alias, "Keystore alias");

        return getPrivateKey(certificateUri, password, alias);
    }

    // Get PrivateKey from a PKCS12 certificate by URI

    private static PrivateKey getPrivateKey(
            URI certificateUri,
            String password,
            String alias) throws Exception {

        requireUri(certificateUri, "Certificate URI");
        requireText(password, "Keystore password");
        requireText(alias, "Keystore alias");

        KeyStore keystore = KeyStore.getInstance("PKCS12");

        try (InputStream inputStream = certificateUri.toURL().openStream()) {
            keystore.load(inputStream, password.toCharArray());
        }

        PrivateKey key = (PrivateKey) keystore.getKey(
                alias,
                password.toCharArray()
        );

        if (key == null) {
            throw new IllegalStateException(
                    "Private key not found in keystore for alias: " + alias
            );
        }

        return key;
    }

    // Get Certificate from a PKCS12 certificate by URI

    private static Certificate[] getCertificateFromP12(
            URI certificateUri,
            String password,
            String alias) throws Exception {

        requireUri(certificateUri, "Certificate URI");
        requireText(password, "Keystore password");
        requireText(alias, "Keystore alias");

        KeyStore keystore = KeyStore.getInstance("PKCS12");

        try (InputStream inputStream = certificateUri.toURL().openStream()) {
            keystore.load(inputStream, password.toCharArray());
        }

        Certificate cert = keystore.getCertificate(alias);

        if (cert == null) {
            throw new IllegalStateException(
                    "Certificate not found in keystore for alias: " + alias
            );
        }

        return new Certificate[]{cert};
    }

    // Get Certificate from an X.509 certificate by URI

    private static Certificate[] getCertificateChainFromX509(
            URI certificateUri) throws Exception {

        requireUri(certificateUri, "Certificate URI");

        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        try (InputStream inputStream = certificateUri.toURL().openStream()) {
            Certificate cert = factory.generateCertificate(inputStream);

            if (!(cert instanceof X509Certificate)) {
                throw new IllegalStateException(
                        "Could not load a valid X.509 certificate from URI: "
                                + certificateUri
                );
            }

            return new Certificate[]{cert};
        }
    }

    // Build a URI from a String value.
    // Returns null when no certificate value was supplied.

    private static URI getOptionalUri(String value) {
        if (!hasText(value)) {
            return null;
        }

        String trimmedValue = value.trim();

        if (trimmedValue.startsWith("RAW(") && trimmedValue.endsWith(")")) {
            trimmedValue = trimmedValue.substring(
                    4,
                    trimmedValue.length() - 1
            ).trim();
        }

        if (trimmedValue.isEmpty()) {
            return null;
        }

        try {
            return URI.create(trimmedValue);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid certificate URI: " + trimmedValue,
                    e
            );
        }
    }

    private static void requireUri(URI uri, String name) {
        if (uri == null) {
            throw new IllegalArgumentException(name + " must not be null.");
        }
    }

    private static void requireText(String value, String name) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(name + " must not be null or empty.");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

}