package org.assimbly.util;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public final class EncryptionUtil {

    private final StandardPBEStringEncryptor textEncryptor = new StandardPBEStringEncryptor();
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;

    // Private fields instead of public static
    private final String password;
    private final String algorithm;

    // Constructor that requires all necessary parameters
    public EncryptionUtil(String password, String algorithm) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (algorithm == null || algorithm.trim().isEmpty()) {
            throw new IllegalArgumentException("Algorithm cannot be null or empty");
        }

        this.password = password;
        this.algorithm = algorithm;

        this.textEncryptor.setPassword(password);
        this.textEncryptor.setAlgorithm(algorithm);
        this.textEncryptor.setIvGenerator(new RandomIvGenerator());
    }

    // Factory method for creating instances from environment properties
    public static EncryptionUtil fromEnvironment(String passwordProperty, String algorithmProperty) {
        if (passwordProperty == null || algorithmProperty == null) {
            throw new IllegalArgumentException("Environment properties cannot be null");
        }
        return new EncryptionUtil(passwordProperty, algorithmProperty);
    }

    // Getters (no setters to maintain immutability)
    public String getAlgorithm() {
        return algorithm;
    }

    // Don't expose the password for security reasons
    public boolean isPasswordSet() {
        return password != null && !password.trim().isEmpty();
    }

    public StandardPBEStringEncryptor getTextEncryptor() {
        return textEncryptor;
    }

    public String encrypt(String plainText) {
        if (plainText == null) {
            throw new IllegalArgumentException("Plain text cannot be null");
        }

        // If the value is already encrypted, do not encrypt again and return
        if (plainText.startsWith("ENC(") && plainText.endsWith(")")) {
            return plainText;
        }

        // Generate random salt
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        // Generate random IV
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // Generate key from password and salt
        SecretKeySpec secretKey = new SecretKeySpec(generateKey(password, salt), "AES");

        // Encrypt the plain text
        byte[] encryptedBytes = encryptWithIv(secretKey, iv, plainText);

        // Encode the salt, IV, and encrypted text
        Base64.Encoder encoder = Base64.getEncoder();
        String encodedSalt = encoder.encodeToString(salt);
        String encodedIv = encoder.encodeToString(iv);
        String encodedEncryptedText = encoder.encodeToString(encryptedBytes);

        // Concatenate and return
        return String.format("ENC(%s|%s|%s)", encodedSalt, encodedIv, encodedEncryptedText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) {
            throw new IllegalArgumentException("Encrypted text cannot be null");
        }

        // Validate and extract components
        if (!encryptedText.startsWith("ENC(") || !encryptedText.endsWith(")")) {
            throw new IllegalArgumentException("Invalid encrypted text format.");
        }

        String contents = encryptedText.substring(4, encryptedText.length() - 1);
        String[] parts = contents.split("\\|");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid encrypted text format.");
        }

        Base64.Decoder decoder = Base64.getDecoder();
        byte[] salt = decoder.decode(parts[0]);
        byte[] iv = decoder.decode(parts[1]);
        byte[] encryptedBytes = decoder.decode(parts[2]);

        // Generate key from password and salt
        SecretKeySpec secretKey = new SecretKeySpec(generateKey(password, salt), "AES");

        // Decrypt the encrypted text
        return decryptWithIv(secretKey, iv, encryptedBytes);
    }

    private byte[] generateKey(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed.", e);
        }
    }

    private byte[] encryptWithIv(SecretKeySpec secretKey, byte[] iv, String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParams);
            return cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8)); // Use UTF-8 encoding
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed.", e);
        }
    }

    private String decryptWithIv(SecretKeySpec secretKey, byte[] iv, byte[] encryptedBytes) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            IvParameterSpec ivParams = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParams);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8); // Use UTF-8 encoding
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed.", e);
        }
    }
    
}