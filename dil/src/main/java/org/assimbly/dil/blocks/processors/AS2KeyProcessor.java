package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.AS2CompressionAlgorithm;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class AS2KeyProcessor implements Processor {

    boolean isToEncrypt = false;
    boolean isToDecrypt = false;
    boolean isSigned = false;

    @Override
    public void process(Exchange exchange) throws Exception {

        // message structure
        String messageStructure = exchange.getProperty("messageStructure", String.class);

        // password and alias
        String password = exchange.getProperty("keyPassword", String.class);
        String alias = exchange.getProperty("keyAlias", String.class);

        URI encryptCertificateUri = null, decryptCertificateUri = null, signingCertificateUri = null;

        // certificateForEncrypt
        String encryptCertificate = exchange.getProperty("certificateForEncrypt", String.class);
        if (encryptCertificate != null) {
            encryptCertificateUri = buildUriFromString(encryptCertificate);
        }
        isToEncrypt = encryptCertificateUri != null;

        // certificateForDecrypt
        String decryptCertificate = exchange.getProperty("certificateForDecrypt", String.class);
        if (decryptCertificate != null) {
            decryptCertificateUri = buildUriFromString(decryptCertificate);
        }
        isToDecrypt = decryptCertificateUri != null && password != null && !password.isEmpty() && alias != null && !alias.isEmpty();

        // certificateForSigning
        String signingCertificate = exchange.getProperty("certificateForSigning", String.class);
        if (signingCertificate != null) {
            signingCertificateUri = buildUriFromString(signingCertificate);
        }
        isSigned = signingCertificateUri != null && password != null && !password.isEmpty() && alias != null && !alias.isEmpty();


        if(isSigned) {
            // signingCertificateChain
            Certificate[] signingCertificateChain = getCertificateFromP12(signingCertificateUri, password, alias);
            exchange.getMessage().setHeader("CamelAs2.signingCertificateChain", signingCertificateChain);

            // signingPrivateKey
            PrivateKey privateKey = getPrivateKey(signingCertificateUri, password, alias);
            exchange.getMessage().setHeader("CamelAs2.signingPrivateKey", privateKey);
        }

        if(isToEncrypt) {
            // encryptingCertificateChain
            Certificate[] encryptingCertificateChain = getCertificateChainFromX509(encryptCertificateUri);
            exchange.getMessage().setHeader("CamelAs2.encryptingCertificateChain", encryptingCertificateChain);
        }

        if(isToDecrypt) {
            // decryptingPrivateKey
            PrivateKey decryptingPrivateKey = getPrivateKey(decryptCertificateUri, password, alias);
            exchange.getMessage().setHeader("CamelAs2.decryptingPrivateKey", decryptingPrivateKey);
        }

        if(messageStructure != null && messageStructure.contains("COMPRESSED")) {
            // compressionAlgorithm
            exchange.getMessage().setHeader("CamelAs2.compressionAlgorithm", AS2CompressionAlgorithm.ZLIB);
        }

    }

    // public static methods used by as2 inbound step
    public static Certificate[] getSigningCertificateChain(URI certificateUri, String password, String alias) throws Exception {
        return getCertificateFromP12(certificateUri, password, alias);
    }
    public static PrivateKey getSigningPrivateKey(URI certificateUri, String password, String alias) throws Exception {
        return getPrivateKey(certificateUri, password, alias);
    }
    public static PrivateKey getDecryptingPrivateKey(URI certificateUri, String password, String alias) throws Exception {
        return getPrivateKey(certificateUri, password, alias);
    }


    // get PrivateKey from a P12 certificate - by a URI
    private static PrivateKey getPrivateKey(URI certificateUri, String password, String alias) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        PrivateKey key;

        try (InputStream inputStream = certificateUri.toURL().openStream()) {
            keystore.load(inputStream, password.toCharArray());
        }
        key = (PrivateKey) keystore.getKey(alias, password.toCharArray());

        if (key == null) {
            throw new IllegalStateException("Private key not found in keystore.");
        }

        return key;
    }

    // get Certificate from a P12 certificate - by a URI
    private static Certificate[] getCertificateFromP12(URI certificateUri, String password, String alias) throws Exception {
        KeyStore keystore = KeyStore.getInstance("PKCS12");
        Certificate cert;

        try (InputStream inputStream = certificateUri.toURL().openStream()) {
            keystore.load(inputStream, password.toCharArray());
        }
        cert = keystore.getCertificate(alias);

        if (cert == null) {
            throw new IllegalStateException("Certificate not found in keystore.");
        }

        return new Certificate[]{cert};
    }

    // get Certificate from a X509 certificate - by a URI
    private static Certificate[] getCertificateChainFromX509(URI certUri) throws Exception {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        URL url = certUri.toURL();

        try (InputStream is = url.openStream()) {
            Certificate cert = factory.generateCertificate(is);

            if (cert == null || !(cert instanceof X509Certificate)) {
                throw new IllegalStateException("Could not load a valid X.509 certificate from URI: " + certUri);
            }

            return new Certificate[]{cert};
        }
    }

    // builds a URI from a String value
    private URI buildUriFromString(String value){
        String trimmedValue = value.trim();

        if (trimmedValue.startsWith("RAW(") && trimmedValue.endsWith(")")) {
            value = trimmedValue.substring(4, trimmedValue.length() - 1);
        } else {
            value = trimmedValue;
        }

        try {
            return new URI(value);
        } catch (Exception e) {
            return null;
        }
    }

}
