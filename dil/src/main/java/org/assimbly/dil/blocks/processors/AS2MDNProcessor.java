package org.assimbly.dil.blocks.processors;

import jakarta.mail.BodyPart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.assimbly.dil.blocks.exceptions.AS2BusinessException;

import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

public class AS2MDNProcessor implements Processor {

    @Override
    public void process(Exchange exchange) throws Exception {
        System.out.println(" > AS2MDNProcessor");

        // Get the body of the exchange, which is the MDN entity
        Object body = exchange.getIn().getBody();

        if (exchange.getIn().getBody() instanceof DispositionNotificationMultipartReportEntity) {
            DispositionNotificationMultipartReportEntity reportEntity = (DispositionNotificationMultipartReportEntity) body;
            // Get the InputStream from the report entity
            InputStream inputStream = reportEntity.getContent();

            // Create a Mail session (this is required to create MimeMessage)
            Properties props = new Properties();
            Session session = Session.getDefaultInstance(props, null);

            // Create MimeMessage from InputStream
            MimeMessage mimeMessage = new MimeMessage(session, inputStream);

            // Extract MimeMultipart from MimeMessage
            MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();

            if (mimeMultipart != null && mimeMultipart.getCount() > 1) {
                // Get the second part (MDN message)
                BodyPart bodyPart = mimeMultipart.getBodyPart(1);

                // Read MDN body content as String
                String mdnText;
                try (Scanner scanner = new Scanner(bodyPart.getInputStream(), "UTF-8").useDelimiter("\\A")) {
                    mdnText = scanner.hasNext() ? scanner.next() : "";
                }

                // Parse MDN body lines and set headers dynamically
                for (String line : mdnText.split("\\r?\\n")) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    String[] parts = line.split(":", 2);
                    if (parts.length != 2) continue;

                    String key = "AS2" + parts[0].trim().replaceAll("\\s+", "-"); // e.g., "Reporting-UA" -> "AS2Reporting-UA"
                    String value = parts[1].trim();

                    exchange.getIn().setHeader(key, value);
                }
            }
        } else {
            exchange.getIn().setHeader("AS2Disposition", "NOT-PROCESSED");
            throw new AS2BusinessException("Body is not an instance of DispositionNotificationMultipartReportEntity");
        }
    }

}
