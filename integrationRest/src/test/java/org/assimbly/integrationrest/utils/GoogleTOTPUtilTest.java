package org.assimbly.integrationrest.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleTOTPUtilTest {

    // "12345678901234567890", the SHA-1 secret of the RFC 6238 test vectors, in Base32
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    void shouldGenerateTheTokensOfTheRfcTestVectors() {
        // the RFC lists 8 digits (94287082, 07081804, 89005924); a token is the last 6 of them
        assertThat(GoogleTOTPUtil.generateToken(RFC_SECRET, 59L)).isEqualTo(287082);
        assertThat(GoogleTOTPUtil.generateToken(RFC_SECRET, 1111111109L)).isEqualTo(81804);
        assertThat(GoogleTOTPUtil.generateToken(RFC_SECRET, 1234567890L)).isEqualTo(5924);
    }

    @Test
    void shouldAcceptALowercaseSecretWithPadding() {
        assertThat(GoogleTOTPUtil.generateToken("gezdgnbvgy3tqojqgezdgnbvgy3tqojq====", 59L)).isEqualTo(287082);
    }

    @Test
    void shouldReturnMinusOneWithoutSecret() {
        assertThat(GoogleTOTPUtil.generateToken(null)).isEqualTo(-1);
    }
}
