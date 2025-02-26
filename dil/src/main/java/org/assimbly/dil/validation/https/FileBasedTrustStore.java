package org.assimbly.dil.validation.https;

import org.apache.http.conn.ssl.NoopHostnameVerifier;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class FileBasedTrustStore {

    private String path;
    private String type;
    private String password;

    public void setPath(String path) {
        this.path = path;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public KeyStore loadTrustStore() throws KeyStoreException, IOException {
        try {
            KeyStore trustStore = KeyStore.getInstance(type);

            try (InputStream inputStream = Files.newInputStream(Paths.get(path))) {
                trustStore.load(inputStream, password.toCharArray());
            }

            return trustStore;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("couldn't load truststore from: " + path, e);
        }
    }

    public void addCertificateEntry(String alias, Certificate certificate) throws KeyStoreException, IOException {
        KeyStore trustStore = loadTrustStore();
        trustStore.setCertificateEntry(alias, certificate);

        try {
            synchronized (this) {
                trustStore.store(Files.newOutputStream(Paths.get(path)), password.toCharArray());
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("couldn't write to truststore " + path, e);
        }
    }

    public void addCertificateForHttpsUrl(String url) throws Exception {
        HttpsURLConnection conn = getUnsecuredConnection(url);
        conn.connect();
        Certificate[] certs = conn.getServerCertificates();

        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate x509) {
                this.addCertificateEntry(String.valueOf(x509.getSerialNumber()), x509);
            } else {
                throw new Exception("unknown certificate type " + cert);
            }
        }
    }

    private HttpsURLConnection getUnsecuredConnection(String url) throws Exception {
        URL destinationURL = new URL(url);
        HttpsURLConnection conn = (HttpsURLConnection) destinationURL.openConnection();
        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(null, new TrustManager[]{insecureTrustManager()}, new SecureRandom());
        conn.setSSLSocketFactory(ctx.getSocketFactory());
        conn.setHostnameVerifier(new NoopHostnameVerifier());
        return conn;
    }

    private static TrustManager insecureTrustManager() {
        return new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
        };
    }
}
