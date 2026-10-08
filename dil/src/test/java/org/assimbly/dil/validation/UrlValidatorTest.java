package org.assimbly.dil.validation;

import org.assimbly.util.error.ValidationErrorMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UrlValidatorTest {

    private static final String ING_FX_RATES_URL =
            "https://www.ingwb.com/en/service/financial-markets/foreign-exchange-rates-for-hungary";

    @Test
    void validateURL_acceptsHttpAndHttps() {
        assertTrue(UrlValidator.validateURL("https://example.com/path"));
        assertTrue(UrlValidator.validateURL("http://example.com"));
        assertTrue(UrlValidator.validateURL(ING_FX_RATES_URL));
    }

    @Test
    void validate_returnsInvalidForMalformedUrl() {
        ValidationErrorMessage result = new UrlValidator().validate("not-a-url");
        assertNotNull(result);
        assertEquals("Url is not valid!", result.getError());
    }

    @Test
    void validate_reachesIngFxRatesUrlWithUserAgentFallback() {
        // Java/ is blocked by this site; fallback UAs should still succeed
        UrlValidator validator = new UrlValidator(
                5000,
                List.of(
                        "Java/21.0.12",
                        "Apache-HttpClient/5.4 (Java/21)",
                        "curl/8.7.1"
                )
        );
        assertNull(validator.validate(ING_FX_RATES_URL));
    }
}
