package org.assimbly.dil.transpiler.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Key manager for the global SSL context.
 *
 * The identity of the runtime (server-identity.p12) is reloaded when the file changes, so new or replaced certificates are
 * used for new TLS handshakes without restarting the flow or the backend. If reloading fails the previous identity is kept.
 */
public class ReloadingKeyManager extends X509ExtendedKeyManager {

    private static final Logger log = LoggerFactory.getLogger(ReloadingKeyManager.class);

    private final Path keystorePath;
    private final String keystorePassword;

    private X509ExtendedKeyManager delegate;
    private FileTime delegateLastModified;

    public ReloadingKeyManager(String keystorePath, String keystorePassword) throws GeneralSecurityException, IOException {
        this.keystorePath = Paths.get(keystorePath);
        this.keystorePassword = keystorePassword;

        // fail fast, like a normal key manager would do
        this.delegateLastModified = Files.getLastModifiedTime(this.keystorePath);
        this.delegate = loadDelegate();
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return getDelegate().getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
        return getDelegate().chooseClientAlias(keyType, issuers, socket);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
        return getDelegate().chooseEngineClientAlias(keyType, issuers, engine);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return getDelegate().getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return getDelegate().chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return getDelegate().chooseEngineServerAlias(keyType, issuers, engine);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return getDelegate().getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return getDelegate().getPrivateKey(alias);
    }

    private synchronized X509ExtendedKeyManager getDelegate() {
        try {
            FileTime lastModified = Files.getLastModifiedTime(keystorePath);
            if (!lastModified.equals(delegateLastModified)) {
                delegate = loadDelegate();
                delegateLastModified = lastModified;
                log.info("Reloaded keystore {}", keystorePath);
            }
        } catch (Exception e) {
            // Keep the previous identity (for example when the file is being replaced). Retried on the next handshake.
            log.warn("Can't reload keystore {}, using the previous certificates", keystorePath, e);
        }
        return delegate;
    }

    private X509ExtendedKeyManager loadDelegate() throws GeneralSecurityException, IOException {

        KeyStore keystore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(keystorePath)) {
            keystore.load(is, keystorePassword.toCharArray());
        }

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keystore, keystorePassword.toCharArray());
        for (KeyManager keyManager : kmf.getKeyManagers()) {
            if (keyManager instanceof X509ExtendedKeyManager x509KeyManager) {
                return x509KeyManager;
            }
        }

        throw new GeneralSecurityException("No X509KeyManager available for keystore " + keystorePath);

    }

}
