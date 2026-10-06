package org.assimbly.dil.transpiler.ssl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.net.ssl.KeyManager;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.nio.file.StandardCopyOption;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ReloadingKeyManagerTest {

    private static final String PASSWORD = "changeit";

    @TempDir
    Path dir;

    @Test
    void reloadsNewCertificateWhenKeystoreChanges() throws Exception {

        Path keystore = createKeystore("first.p12", "first");
        Path second = createKeystore("second.p12", "second");

        ReloadingKeyManager keyManager = new ReloadingKeyManager(keystore.toString(), PASSWORD);

        assertArrayEquals(new String[]{"first"}, keyManager.getServerAliases("RSA", null));

        // replace the file with another keystore (with a newer modified time)
        Files.copy(second, keystore, StandardCopyOption.REPLACE_EXISTING);
        Files.setLastModifiedTime(keystore, FileTime.from(Instant.now().plus(1, ChronoUnit.MINUTES)));

        assertArrayEquals(new String[]{"second"}, keyManager.getServerAliases("RSA", null));
        assertNotNull(keyManager.getPrivateKey("second"));
        assertNotNull(keyManager.getCertificateChain("second"));
    }

    @Test
    void keepsPreviousCertificateWhenReloadFails() throws Exception {

        Path keystore = createKeystore("first.p12", "first");

        ReloadingKeyManager keyManager = new ReloadingKeyManager(keystore.toString(), PASSWORD);

        Files.writeString(keystore, "not a keystore", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(keystore, FileTime.from(Instant.now().plus(1, ChronoUnit.MINUTES)));

        assertArrayEquals(new String[]{"first"}, keyManager.getServerAliases("RSA", null));
        assertNotNull(keyManager.getPrivateKey("first"));
    }

    @Test
    void sslContextParametersUsesReloadingKeyManager() throws Exception {

        Path keystore = createKeystore("first.p12", "first");

        KeyManager[] keyManagers = new ReloadingKeyManagersParameters(keystore.toString(), PASSWORD).createKeyManagers();

        assertEquals(1, keyManagers.length);
        assertInstanceOf(ReloadingKeyManager.class, keyManagers[0]);
    }

    private Path createKeystore(String fileName, String alias) throws IOException, InterruptedException {

        Path keystore = dir.resolve(fileName);

        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        Process process = new ProcessBuilder(keytool, "-genkeypair", "-alias", alias, "-keyalg", "RSA", "-keysize", "2048",
                "-dname", "CN=" + alias, "-validity", "2", "-storetype", "PKCS12",
                "-keystore", keystore.toString(), "-storepass", PASSWORD, "-keypass", PASSWORD)
                .redirectErrorStream(true)
                .redirectOutput(new File(dir.toFile(), fileName + ".log"))
                .start();

        assertEquals(0, process.waitFor(), "keytool failed");

        return keystore;
    }

}
