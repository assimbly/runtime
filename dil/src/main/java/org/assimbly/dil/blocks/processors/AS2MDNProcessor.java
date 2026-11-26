package org.assimbly.dil.blocks.processors;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.entity.ApplicationEntity;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.apache.camel.component.as2.api.entity.MimeEntity;
import org.apache.camel.component.as2.api.entity.MultipartSignedEntity;
import org.apache.camel.component.as2.api.util.SigningUtils;
import org.apache.camel.component.as2.internal.AS2Constants;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.assimbly.dil.blocks.exceptions.AS2BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.Locale;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Reads the MDN the partner returned for an outbound AS2 message and sets its fields as AS2* headers.
 * Handles unsigned MDNs (multipart/report) and signed MDNs (multipart/signed around a multipart/report).
 */
public class AS2MDNProcessor implements Processor {

    private static final Logger log = LoggerFactory.getLogger(AS2MDNProcessor.class);

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    /** Set by AS2KeyProcessor from the partner's public certificate. */
    static final String PARTNER_CERTIFICATE_HEADER = "CamelAs2.encryptingCertificateChain";

    static final String SIGNED_HEADER = "AS2MDN-Signed";
    static final String SIGNATURE_VALID_HEADER = "AS2MDN-Signature-Valid";

    private static final int MAX_BODY_IN_ERROR = 500;

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();

        if (body instanceof MultipartSignedEntity signedEntity) {
            MimeEntity signedPart = signedEntity.getSignedDataEntity();
            if (!(signedPart instanceof DispositionNotificationMultipartReportEntity reportEntity)) {
                fail(exchange, "The partner returned a signed message that is not an MDN: the signed part is "
                        + describe(signedPart));
                return;
            }
            log.debug("Received a signed MDN");
            exchange.getIn().setHeader(SIGNED_HEADER, true);
            checkSignature(exchange, signedEntity);
            processMdnReportBody(exchange, reportEntity);
        } else if (body instanceof DispositionNotificationMultipartReportEntity reportEntity) {
            log.debug("Received an unsigned MDN");
            exchange.getIn().setHeader(SIGNED_HEADER, false);
            processMdnReportBody(exchange, reportEntity);
        } else {
            fail(exchange, "The partner did not return an MDN. " + describeResponse(exchange));
        }
    }

    /**
     * Only logs the result. Camel 4.23 verifies the signature over a re-serialised copy of the signed part
     * instead of the bytes received, so a valid MDN can fail the check.
     */
    private void checkSignature(Exchange exchange, MultipartSignedEntity signedEntity) {
        Certificate[] partnerCertificates = exchange.getIn().getHeader(PARTNER_CERTIFICATE_HEADER, Certificate[].class);
        if (partnerCertificates == null || partnerCertificates.length == 0) {
            log.debug("No partner certificate on the step, so the MDN signature is not checked");
            return;
        }
        boolean valid = SigningUtils.isValid(signedEntity, partnerCertificates);
        exchange.getIn().setHeader(SIGNATURE_VALID_HEADER, valid);
        if (valid) {
            log.debug("MDN signature is valid");
        } else {
            log.warn("MDN signature could not be verified with the partner's certificate");
        }
    }

    private void processMdnReportBody(
            Exchange exchange,
            DispositionNotificationMultipartReportEntity reportEntity)
            throws IOException, MessagingException {

        InputStream inputStream = reportEntity.getContent();

        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage mimeMessage = new MimeMessage(session, inputStream);
        MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();

        if (mimeMultipart != null && mimeMultipart.getCount() > 1) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(1);

            String mdnText;
            try (Scanner scanner = new Scanner(
                    bodyPart.getInputStream(),
                    StandardCharsets.UTF_8).useDelimiter("\\A")) {

                mdnText = scanner.hasNext() ? scanner.next() : "";
            }

            setMdnHeader(exchange, mdnText);
        } else {
            log.warn("MDN has no disposition notification part");
        }

        String disposition = exchange.getIn().getHeader("AS2Disposition", String.class);
        log.debug("MDN disposition: {}, original message id: {}, MIC: {}",
                disposition,
                exchange.getIn().getHeader("AS2Original-Message-ID"),
                exchange.getIn().getHeader("AS2Received-Content-MIC"));
        if (disposition == null || !disposition.trim().toLowerCase(Locale.ROOT).endsWith("processed")) {
            log.warn("MDN disposition is not processed: {}", disposition);
        }
    }

    private void setMdnHeader(Exchange exchange, String mdnText) {
        for (String line : mdnText.split("\\r?\\n")) {
            line = line.trim();

            String[] parts = line.split(":", 2);
            if (line.isEmpty() || parts.length != 2) {
                continue;
            }

            String key = "AS2"
                    + WHITESPACE_PATTERN.matcher(parts[0].trim()).replaceAll("-");

            String value = parts[1].trim();

            exchange.getIn().setHeader(key, value);
        }
    }

    private void fail(Exchange exchange, String message) throws AS2BusinessException {
        log.warn(message);
        exchange.getIn().setHeader("AS2Disposition", "NOT-PROCESSED");
        throw new AS2BusinessException(message);
    }

    /** The HTTP status, content type and start of the body the partner answered with. */
    private String describeResponse(Exchange exchange) {
        HttpCoreContext context = exchange.getProperty(AS2Constants.AS2_INTERCHANGE, HttpCoreContext.class);
        HttpResponse response = context == null ? null : context.getResponse();
        if (response == null) {
            return "No HTTP response is available.";
        }

        StringBuilder description = new StringBuilder("HTTP status: ")
                .append(response.getCode());
        if (response.getReasonPhrase() != null) {
            description.append(' ').append(response.getReasonPhrase());
        }
        Header contentType = response.getFirstHeader("Content-Type");
        description.append(", content type: ").append(contentType == null ? "none" : contentType.getValue());

        if (response instanceof ClassicHttpResponse classicResponse) {
            String text = readText(classicResponse.getEntity());
            if (text != null && !text.isBlank()) {
                if (text.length() > MAX_BODY_IN_ERROR) {
                    text = text.substring(0, MAX_BODY_IN_ERROR) + "...";
                }
                description.append(", body: ").append(text.strip());
            }
        }
        return description.toString();
    }

    private String readText(HttpEntity entity) {
        if (entity == null) {
            return null;
        }
        try {
            if (entity instanceof ApplicationEntity applicationEntity) {
                Object message = applicationEntity.getEdiMessage();
                if (message instanceof InputStream stream) {
                    return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                }
                return message == null ? null : message.toString();
            }
            if (entity.isRepeatable()) {
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.debug("Could not read the response body", e);
        }
        return null;
    }

    private String describe(Object entity) {
        if (entity == null) {
            return "empty";
        }
        if (entity instanceof HttpEntity httpEntity && httpEntity.getContentType() != null) {
            return httpEntity.getContentType();
        }
        return entity.getClass().getSimpleName();
    }

}
