package org.assimbly.util;


import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;

public class EncryptionUtil {

    private final StandardPBEStringEncryptor textEncryptor = new StandardPBEStringEncryptor();

    public EncryptionUtil(String textEncryptorPassword, String algorithm) {
        this.textEncryptor.setPassword(textEncryptorPassword);
        this.textEncryptor.setAlgorithm(algorithm);
        this.textEncryptor.setIvGenerator(new RandomIvGenerator());
    }

    public String encrypt(String plainText) {
        return this.textEncryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        return this.textEncryptor.decrypt(encryptedText);
    }

}

