package org.assimbly.util.helper;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class Base64Helper {

    private Base64Helper() {}

    public static String unmarshal(String base64, Charset charset) {
        return new String(unmarshal(base64), charset);
    }

    // Lenient: accepts the standard and the URL-safe alphabet, with or without padding, and skips line breaks
    public static byte[] unmarshal(String base64) {
        return Base64.getMimeDecoder().decode(base64.replace('-', '+').replace('_', '/'));
    }

    public static String marshal(String string){
        return marshal(string.getBytes(StandardCharsets.UTF_8));
    }

    public static String marshal(byte[] bytes){
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String marshal(byte[] bytes, Charset charset){
        return new String(Base64.getEncoder().encode(bytes), charset);
    }

    public static String marshal(char[] bytes){
        return marshal(new String(bytes));
    }
}
