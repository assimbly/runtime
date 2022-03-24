package org.assimbly.util.helper;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

public final class Base64Helper {

    private Base64Helper() {}

    public static String unmarshal(String base64, Charset charset) {
        byte[] decoded = Base64.decodeBase64(base64);

        return new String(decoded, charset);
    }

    public static byte[] unmarshal(String base64) {
        return Base64.decodeBase64(base64);
    }

    public static String marshal(String string){
        byte[] encoded = Base64.encodeBase64(string.getBytes());
       return new String(encoded);
    }

    public static String marshal(byte[] bytes){
        byte[] encoded = Base64.encodeBase64(bytes);
        return new String(encoded);
    }

    public static String marshal(byte[] bytes, Charset charset){
        byte[] encoded = Base64.encodeBase64(bytes);
        return new String(encoded, charset);
    }

    public static String marshal(char[] bytes){
        byte[] encoded = Base64.encodeBase64(new String(bytes).getBytes());
        return new String(encoded);
    }
}
