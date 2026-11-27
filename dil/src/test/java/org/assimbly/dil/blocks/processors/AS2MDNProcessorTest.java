package org.assimbly.dil.blocks.processors;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.component.as2.api.AS2SignatureAlgorithm;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.apache.camel.component.as2.api.entity.EntityParser;
import org.apache.camel.component.as2.api.entity.MimeEntity;
import org.apache.camel.component.as2.api.entity.MultipartSignedEntity;
import org.apache.camel.component.as2.api.util.SigningUtils;
import org.apache.camel.component.as2.internal.AS2Constants;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.protocol.HttpCoreContext;
import org.assimbly.dil.blocks.exceptions.AS2BusinessException;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AS2MDNProcessorTest {

    private static final String REPORT_BOUNDARY = "report-boundary";

    private static final String REPORT_CONTENT_TYPE =
            "multipart/report; report-type=disposition-notification; boundary=\"" + REPORT_BOUNDARY + "\"";

    private static final String REPORT_BODY = String.join("\r\n",
            "--" + REPORT_BOUNDARY,
            "Content-Type: text/plain; charset=US-ASCII",
            "Content-Transfer-Encoding: 7bit",
            "",
            "MDN for Message-ID: <test-message@sender>",
            "",
            "--" + REPORT_BOUNDARY,
            "Content-Type: message/disposition-notification",
            "Content-Transfer-Encoding: 7bit",
            "",
            "Reporting-UA: Partner AS2",
            "Original-Recipient: rfc822; partner",
            "Final-Recipient: rfc822; partner",
            "Original-Message-ID: <test-message@sender>",
            "Disposition: automatic-action/MDN-sent-automatically; processed",
            "Received-Content-MIC: 7v7F++fQaNoxnD3U8YhRzxL8hhk=, sha-256",
            "",
            "--" + REPORT_BOUNDARY + "--",
            "");

    private static final String PARTNER_CERTIFICATE_HEADER = "CamelAs2.encryptingCertificateChain";

    private static KeyPair partnerKeys;
    private static Certificate[] partnerCertificates;
    private static Certificate[] otherCertificates;

    private CamelContext context;
    private final AS2MDNProcessor processor = new AS2MDNProcessor();

    @BeforeAll
    static void createCertificates() throws Exception {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        partnerKeys = newKeyPair();
        partnerCertificates = new Certificate[] {selfSigned(partnerKeys, "CN=Partner")};
        otherCertificates = new Certificate[] {selfSigned(newKeyPair(), "CN=Someone else")};
    }

    @BeforeEach
    void startContext() {
        context = new DefaultCamelContext();
        context.start();
    }

    @AfterEach
    void stopContext() {
        context.stop();
    }

    @Test
    void readsUnsignedMdn() throws Exception {
        Exchange exchange = exchangeWithResponse(response(REPORT_CONTENT_TYPE, REPORT_BODY.getBytes(StandardCharsets.US_ASCII)));
        assertInstanceOf(DispositionNotificationMultipartReportEntity.class, exchange.getIn().getBody());

        processor.process(exchange);

        assertMdnHeaders(exchange);
        assertEquals(false, exchange.getIn().getHeader("AS2MDN-Signed"));
        assertNull(exchange.getIn().getHeader("AS2MDN-Signature-Valid"));
    }

    @Test
    void readsSignedMdnAndChecksItsSignature() throws Exception {
        Exchange exchange = exchangeWithResponse(signedMdnResponse());
        exchange.getIn().setHeader(PARTNER_CERTIFICATE_HEADER, partnerCertificates);
        assertInstanceOf(MultipartSignedEntity.class, exchange.getIn().getBody());

        processor.process(exchange);

        assertMdnHeaders(exchange);
        assertEquals(true, exchange.getIn().getHeader("AS2MDN-Signed"));
        assertEquals(true, exchange.getIn().getHeader("AS2MDN-Signature-Valid"));
    }

    @Test
    void readsSignedMdnSignedWithAnotherCertificate() throws Exception {
        Exchange exchange = exchangeWithResponse(signedMdnResponse());
        exchange.getIn().setHeader(PARTNER_CERTIFICATE_HEADER, otherCertificates);

        processor.process(exchange);

        // The check only warns for now: Camel 4.23 can reject valid signatures
        assertMdnHeaders(exchange);
        assertEquals(false, exchange.getIn().getHeader("AS2MDN-Signature-Valid"));
    }

    @Test
    void readsSignedMdnWithoutPartnerCertificate() throws Exception {
        Exchange exchange = exchangeWithResponse(signedMdnResponse());

        processor.process(exchange);

        assertMdnHeaders(exchange);
        assertEquals(true, exchange.getIn().getHeader("AS2MDN-Signed"));
        assertNull(exchange.getIn().getHeader("AS2MDN-Signature-Valid"));
    }

    @Test
    void reportsWhatThePartnerReturnedWhenItIsNotAnMdn() throws Exception {
        BasicClassicHttpResponse response = response("text/html; charset=UTF-8",
                "<html><body>Unknown AS2 partner</body></html>".getBytes(StandardCharsets.UTF_8));
        response.setCode(403);
        response.setReasonPhrase("Forbidden");
        Exchange exchange = exchangeWithResponse(response);
        assertNull(exchange.getIn().getBody());

        AS2BusinessException error = assertThrows(AS2BusinessException.class, () -> processor.process(exchange));

        assertTrue(error.getMessage().contains("HTTP status: 403 Forbidden"), error.getMessage());
        assertTrue(error.getMessage().contains("content type: text/html"), error.getMessage());
        assertTrue(error.getMessage().contains("Unknown AS2 partner"), error.getMessage());
        assertEquals("NOT-PROCESSED", exchange.getIn().getHeader("AS2Disposition"));
    }

    @Test
    void failsWithoutResponse() {
        Exchange exchange = new DefaultExchange(context);

        AS2BusinessException error = assertThrows(AS2BusinessException.class, () -> processor.process(exchange));

        assertTrue(error.getMessage().contains("No HTTP response is available"), error.getMessage());
        assertFalse(exchange.getIn().getHeaders().containsKey("AS2Original-Message-ID"));
    }

    private static void assertMdnHeaders(Exchange exchange) {
        assertEquals("automatic-action/MDN-sent-automatically; processed", exchange.getIn().getHeader("AS2Disposition"));
        assertEquals("<test-message@sender>", exchange.getIn().getHeader("AS2Original-Message-ID"));
        assertEquals("Partner AS2", exchange.getIn().getHeader("AS2Reporting-UA"));
        assertEquals("7v7F++fQaNoxnD3U8YhRzxL8hhk=, sha-256", exchange.getIn().getHeader("AS2Received-Content-MIC"));
    }

    /** Sets the body the way AS2Producer does: the parsed response entity when it is an MDN, otherwise null. */
    private Exchange exchangeWithResponse(BasicClassicHttpResponse response) throws Exception {
        EntityParser.parseAS2MessageEntity(response);
        HttpCoreContext httpContext = HttpCoreContext.create();
        httpContext.setResponse(response);

        Exchange exchange = new DefaultExchange(context);
        exchange.setProperty(AS2Constants.AS2_INTERCHANGE, httpContext);
        HttpEntity entity = response.getEntity();
        if (entity instanceof DispositionNotificationMultipartReportEntity || entity instanceof MultipartSignedEntity) {
            exchange.getIn().setBody(entity);
        }
        return exchange;
    }

    private static BasicClassicHttpResponse signedMdnResponse() throws Exception {
        // The unsigned MDN, as a part of the signed message
        BasicClassicHttpResponse unsigned = response(REPORT_CONTENT_TYPE, REPORT_BODY.getBytes(StandardCharsets.US_ASCII));
        EntityParser.parseAS2MessageEntity(unsigned);
        MimeEntity reportEntity = (MimeEntity) unsigned.getEntity();
        reportEntity.setMainBody(false);
        reportEntity.setHeader("Content-Type", REPORT_CONTENT_TYPE);

        MultipartSignedEntity signed = new MultipartSignedEntity(reportEntity,
                SigningUtils.createSigningGenerator(AS2SignatureAlgorithm.SHA256WITHRSA, partnerCertificates,
                        partnerKeys.getPrivate()),
                StandardCharsets.US_ASCII.name(), "base64", true, "signed-boundary");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        signed.writeTo(bytes);
        return response(signed.getContentType(), bytes.toByteArray());
    }

    private static BasicClassicHttpResponse response(String contentType, byte[] body) {
        BasicClassicHttpResponse response = new BasicClassicHttpResponse(200, "OK");
        response.setHeader("Content-Type", contentType);
        response.setEntity(new BasicHttpEntity(new ByteArrayInputStream(body), body.length, ContentType.parse(contentType)));
        return response;
    }

    private static KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static X509Certificate selfSigned(KeyPair keys, String name) throws Exception {
        X500Name subject = new X500Name(name);
        Date now = new Date();
        JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(subject,
                BigInteger.valueOf(System.nanoTime()), now, new Date(now.getTime() + 86_400_000L), subject,
                keys.getPublic());
        return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(keys.getPrivate())));
    }

}
