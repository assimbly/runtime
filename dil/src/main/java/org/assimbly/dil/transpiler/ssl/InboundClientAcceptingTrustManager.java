package org.assimbly.dil.transpiler.ssl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.InputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Trust manager for the global SSL context.
 *
 * Server certificates (outbound calls) are validated against the outbound truststore, which is reloaded when the file changes.
 * Client certificates (inbound calls) are accepted during the TLS handshake. The TLS handshake still proves that the client
 * owns the private key of the certificate, the certificate itself is validated per flow by the MutualTlsHandler.
 */
public class InboundClientAcceptingTrustManager extends X509ExtendedTrustManager {

    private static final Logger log = LoggerFactory.getLogger(InboundClientAcceptingTrustManager.class);

    private static final X509Certificate[] NO_ISSUERS = new X509Certificate[0];

    private final Path truststorePath;
    private final String truststorePassword;

    private X509ExtendedTrustManager delegate;
    private FileTime delegateLastModified;

    public InboundClientAcceptingTrustManager(String truststorePath, String truststorePassword) {
        this.truststorePath = Paths.get(truststorePath);
        this.truststorePassword = truststorePassword;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        logClientCertificate(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        logClientCertificate(chain);
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        logClientCertificate(chain);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType, socket);
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
        getDelegate().checkServerTrusted(chain, authType, engine);
    }

    // An empty list of accepted issuers lets clients send a certificate from any CA when a client certificate is requested
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return NO_ISSUERS;
    }

    private void logClientCertificate(X509Certificate[] chain) {
        if (log.isDebugEnabled() && chain != null && chain.length > 0) {
            log.debug("Client certificate received during TLS handshake: subject={}, issuer={}",
                    chain[0].getSubjectX500Principal(), chain[0].getIssuerX500Principal());
        }
    }

    private synchronized X509ExtendedTrustManager getDelegate() throws CertificateException {
        try {
            FileTime lastModified = Files.getLastModifiedTime(truststorePath);
            if (delegate == null || !lastModified.equals(delegateLastModified)) {
                delegate = loadDelegate();
                delegateLastModified = lastModified;
            }
            return delegate;
        } catch (CertificateException e) {
            throw e;
        } catch (Exception e) {
            throw new CertificateException("Can't load truststore " + truststorePath, e);
        }
    }

    private X509ExtendedTrustManager loadDelegate() throws Exception {

        KeyStore truststore = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(truststorePath)) {
            truststore.load(is, truststorePassword.toCharArray());
        }

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(truststore);
        for (TrustManager trustManager : tmf.getTrustManagers()) {
            if (trustManager instanceof X509ExtendedTrustManager x509TrustManager) {
                return x509TrustManager;
            }
        }

        throw new CertificateException("No X509TrustManager available for truststore " + truststorePath);

    }

}
