package org.assimbly.dil.blocks.processors;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.assimbly.dil.blocks.exceptions.AS2BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

public class AS2MDNProcessor implements Processor {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    @Override
    public void process(Exchange exchange) throws Exception {
        Object body = exchange.getIn().getBody();

        if (body instanceof DispositionNotificationMultipartReportEntity reportEntity) {
            processMdnReportBody(exchange, reportEntity);
        } else {
            exchange.getIn().setHeader("AS2Disposition", "NOT-PROCESSED");
            throw new AS2BusinessException(
                    "Body is not an instance of DispositionNotificationMultipartReportEntity");
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

}