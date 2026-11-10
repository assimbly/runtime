package org.assimbly.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptionUtilTest {

    private static final String PASSWORD = "test-password";

    @Test
    void encryptDecryptRoundTrip() {
        EncryptionUtil util = new EncryptionUtil(PASSWORD);

        String encrypted = util.encrypt("secret value");

        assertThat(encrypted).startsWith(EncryptionUtil.ENCODE_PREFIX).endsWith(EncryptionUtil.ENCODE_SUFFIX);
        assertThat(util.decrypt(encrypted)).isEqualTo("secret value");
    }

    @Test
    void encryptDoesNotDoubleEncrypt() {
        EncryptionUtil util = new EncryptionUtil(PASSWORD);

        String encrypted = util.encrypt("once");
        assertThat(util.encrypt(encrypted)).isEqualTo(encrypted);
    }

    @Test
    void decryptRejectsInvalidFormat() {
        EncryptionUtil util = new EncryptionUtil(PASSWORD);

        assertThatThrownBy(() -> util.decrypt("not-encrypted"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid encrypted text format");
    }

    @Test
    void decryptAcceptsLegacyCbcPayload() throws Exception {
        String legacy = encryptLegacyCbc(PASSWORD, "legacy secret");

        assertThat(new EncryptionUtil(PASSWORD).decrypt(legacy)).isEqualTo("legacy secret");
    }

    /** Builds an ENC(salt|iv|ciphertext) value with the former AES/CBC/PKCS5Padding scheme. */
    private static String encryptLegacyCbc(String password, String plainText) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        byte[] iv = new byte[16];
        random.nextBytes(salt);
        random.nextBytes(iv);

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, EncryptionUtil.PBKDF2_ITERATIONS, EncryptionUtil.KEY_LENGTH_BITS);
        byte[] key = SecretKeyFactory.getInstance(EncryptionUtil.KEY_DERIVATION_ALGORITHM).generateSecret(spec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(key, EncryptionUtil.CIPHER_ALGORITHM);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        Base64.Encoder encoder = Base64.getEncoder();
        return EncryptionUtil.ENCODE_PREFIX
                + encoder.encodeToString(salt) + "|"
                + encoder.encodeToString(iv) + "|"
                + encoder.encodeToString(encryptedBytes)
                + EncryptionUtil.ENCODE_SUFFIX;
    }
}
