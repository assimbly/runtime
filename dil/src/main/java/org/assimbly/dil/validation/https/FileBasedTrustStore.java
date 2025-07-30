package org.assimbly.dil.validation.https;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

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
                try (OutputStream out = Files.newOutputStream(Paths.get(path))) {
                    trustStore.store(out, password.toCharArray());
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new KeyStoreException("couldn't write to truststore " + path, e);
        }
    }

}
