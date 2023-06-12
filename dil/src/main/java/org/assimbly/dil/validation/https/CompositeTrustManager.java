package org.assimbly.dil.validation.https;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeTrustManager implements X509TrustManager {

    private List<X509TrustManager> trustManagers = new ArrayList<>();

    public void addTrustManagers(KeyStore trustStore) throws KeyStoreException {
        try {
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            for(TrustManager tm: tmf.getTrustManagers()) {
                if(tm instanceof X509TrustManager) {
                    addTrustManager((X509TrustManager) tm);
                }
            }
        } catch (Exception e) {
            throw new KeyStoreException(e);
        }
    }

    public void addTrustManager(X509TrustManager trustManager) {
        trustManagers.add(trustManager);
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        CertificateException latestException = null;
        boolean trusted = false;

        for(X509TrustManager trustManager: trustManagers) {
            try {
                trustManager.checkClientTrusted(x509Certificates, s);
                trusted = true;
            } catch(CertificateException cex) {
                latestException = cex;
            }
        }

        if(!trusted) {
            throw latestException;
        }
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
        CertificateException latestException = null;
        boolean trusted = false;

        for(X509TrustManager trustManager: trustManagers) {
            try {
                trustManager.checkServerTrusted(x509Certificates, s);
                trusted = true;
            } catch(CertificateException cex) {
                latestException = cex;
            }
        }

        if(!trusted) {
            throw latestException;
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        List<X509Certificate> acceptedIssuers = new ArrayList<>();

        for(X509TrustManager trustManager: trustManagers) {
            acceptedIssuers.addAll(Arrays.asList(trustManager.getAcceptedIssuers()));
        }

        return acceptedIssuers.toArray(new X509Certificate[0]);
    }
}
