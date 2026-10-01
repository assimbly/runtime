package org.assimbly.util;

import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Expiry of a certificate in a keystore. An expired certificate has daysUntilExpiry 0 and is not valid.
 */
public record CertificateExpiry(String name, String expiresAt, long daysUntilExpiry, boolean valid) {

    public static CertificateExpiry of(String name, X509Certificate certificate, Instant now) {
        Instant notAfter = certificate.getNotAfter().toInstant();
        boolean valid = notAfter.isAfter(now);
        long daysUntilExpiry = valid ? Duration.between(now, notAfter).toDays() : 0;
        return new CertificateExpiry(name, format(notAfter), daysUntilExpiry, valid);
    }

    /**
     * A certificate in a keystore that is past its expiry date.
     */
    public record Expired(String name, String expiredAt, long daysExpired, boolean valid) {

        public static Expired of(String name, X509Certificate certificate, Instant now) {
            Instant notAfter = certificate.getNotAfter().toInstant();
            long daysExpired = notAfter.isBefore(now) ? Duration.between(notAfter, now).toDays() : 0;
            return new Expired(name, format(notAfter), daysExpired, false);
        }

    }

    private static String format(Instant instant) {
        return instant.truncatedTo(ChronoUnit.SECONDS).toString();
    }

}
