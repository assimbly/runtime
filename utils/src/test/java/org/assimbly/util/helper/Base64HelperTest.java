package org.assimbly.util.helper;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class Base64HelperTest {

    @Test
    void shouldRoundTripText() {
        String encoded = Base64Helper.marshal("user:pässword");

        assertThat(encoded).isEqualTo("dXNlcjpww6Rzc3dvcmQ=");
        assertThat(Base64Helper.unmarshal(encoded, StandardCharsets.UTF_8)).isEqualTo("user:pässword");
    }

    @Test
    void shouldEncodeBytesAndCharsWithoutLineBreaks() {
        byte[] bytes = new byte[100];

        assertThat(Base64Helper.marshal(bytes)).doesNotContain("\n").hasSize(136);
        assertThat(Base64Helper.marshal(bytes, StandardCharsets.UTF_8)).isEqualTo(Base64Helper.marshal(bytes));
        assertThat(Base64Helper.marshal("abc".toCharArray())).isEqualTo("YWJj");
    }

    @Test
    void shouldDecodeLeniently() {
        byte[] expected = {(byte) 0xfb, (byte) 0xff, (byte) 0xfe};

        assertThat(Base64Helper.unmarshal("+//+")).isEqualTo(expected);
        // URL-safe alphabet, missing padding and line breaks are accepted too
        assertThat(Base64Helper.unmarshal("-__-")).isEqualTo(expected);
        assertThat(Base64Helper.unmarshal("YQ")).isEqualTo("a".getBytes(StandardCharsets.UTF_8));
        assertThat(Base64Helper.unmarshal("YW\r\nJj\n")).isEqualTo("abc".getBytes(StandardCharsets.UTF_8));
    }
}
