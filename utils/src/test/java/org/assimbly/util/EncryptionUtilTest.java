package org.assimbly.util;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
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
    void encryptWritesGcmMarkedFormat() {
        String encrypted = new EncryptionUtil(PASSWORD).encrypt("secret value");

        assertThat(encrypted).startsWith("ENC(GCM|");
        assertThat(encrypted.split("\\|")).hasSize(4);
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
        assertThatThrownBy(() -> util.decrypt("ENC(a|b)"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> util.decrypt("ENC(CBC|a|b|c)"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decryptRejectsTamperedCiphertext() {
        EncryptionUtil util = new EncryptionUtil(PASSWORD);
        String[] parts = util.encrypt("secret value").split("\\|");
        byte[] cipherText = Base64.getDecoder().decode(parts[3].substring(0, parts[3].length() - 1));
        cipherText[0] ^= 1;
        String tampered = "%s|%s|%s|%s)".formatted(parts[0], parts[1], parts[2], Base64.getEncoder().encodeToString(cipherText));

        assertThatThrownBy(() -> util.decrypt(tampered))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Decryption failed");
    }

    @Test
    void decryptAcceptsLegacyCbcPayload() throws Exception {
        String legacy = encryptUnmarked("AES/CBC/PKCS5Padding", 16, "legacy secret");

        assertThat(new EncryptionUtil(PASSWORD).decrypt(legacy)).isEqualTo("legacy secret");
    }

    @Test
    void decryptAcceptsUnmarkedGcmPayload() throws Exception {
        String unmarked = encryptUnmarked("AES/GCM/NoPadding", 12, "unmarked secret");

        assertThat(new EncryptionUtil(PASSWORD).decrypt(unmarked)).isEqualTo("unmarked secret");
    }

    /** Builds an ENC(salt|iv|ciphertext) value without the GCM marker, as written before the marker existed. */
    private static String encryptUnmarked(String transformation, int ivLength, String plainText) throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        byte[] iv = new byte[ivLength];
        random.nextBytes(salt);
        random.nextBytes(iv);

        PBEKeySpec spec = new PBEKeySpec(PASSWORD.toCharArray(), salt, EncryptionUtil.PBKDF2_ITERATIONS, EncryptionUtil.KEY_LENGTH_BITS);
        byte[] key = SecretKeyFactory.getInstance(EncryptionUtil.KEY_DERIVATION_ALGORITHM).generateSecret(spec).getEncoded();
        SecretKeySpec secretKey = new SecretKeySpec(key, EncryptionUtil.CIPHER_ALGORITHM);

        AlgorithmParameterSpec params = ivLength == 12 ? new GCMParameterSpec(128, iv) : new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, params);
        byte[] encryptedBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        Base64.Encoder encoder = Base64.getEncoder();
        return EncryptionUtil.ENCODE_PREFIX
                + encoder.encodeToString(salt) + "|"
                + encoder.encodeToString(iv) + "|"
                + encoder.encodeToString(encryptedBytes)
                + EncryptionUtil.ENCODE_SUFFIX;
    }
}
