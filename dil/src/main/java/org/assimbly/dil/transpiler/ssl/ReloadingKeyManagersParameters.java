package org.assimbly.dil.transpiler.ssl;

import org.apache.camel.support.jsse.KeyManagersParameters;

import javax.net.ssl.KeyManager;
import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Camel's KeyManagersParameters has no setter for a custom key manager (unlike TrustManagersParameters),
 * so createKeyManagers is overridden to return the ReloadingKeyManager.
 */
public class ReloadingKeyManagersParameters extends KeyManagersParameters {

    private final String keystorePath;

    public ReloadingKeyManagersParameters(String keystorePath, String keystorePassword) {
        this.keystorePath = keystorePath;
        setKeyPassword(keystorePassword);
    }

    @Override
    public KeyManager[] createKeyManagers() throws GeneralSecurityException, IOException {
        return new KeyManager[]{new ReloadingKeyManager(keystorePath, getKeyPassword())};
    }

}
