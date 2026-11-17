package org.assimbly.integrationrest.utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.time.Instant;

/** Time-based one-time passwords as Google Authenticator makes them (RFC 6238: HMAC-SHA1, 30 seconds, 6 digits). */
public class GoogleTOTPUtil {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    public static int generateToken(String secret) {
        return generateToken(secret, Instant.now().getEpochSecond());
    }

    static int generateToken(String secret, long epochSecond) {

        if(secret == null) {
            return -1;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(epochSecond / 30).array());

            // dynamic truncation: the last 4 bits say where the 31-bit code starts
            int offset = hash[hash.length - 1] & 0xF;
            return (ByteBuffer.wrap(hash, offset, Integer.BYTES).getInt() & Integer.MAX_VALUE) % 1_000_000;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Could not generate a TOTP token", e);
        }
    }

    private static byte[] decodeBase32(String base32) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : base32.toUpperCase().toCharArray()) {
            int value = BASE32_ALPHABET.indexOf(c);
            if (value < 0) {
                continue; // padding or whitespace
            }
            buffer = (buffer << 5) | value;
            bits += 5;
            if (bits >= 8) {
                bits -= 8;
                bytes.write(buffer >> bits);
            }
        }
        return bytes.toByteArray();
    }
}
