package org.assimbly.dil.blocks.processors;

import jakarta.mail.BodyPart;
import jakarta.mail.Header;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.as2.api.entity.DispositionNotificationMultipartReportEntity;
import org.assimbly.dil.blocks.exceptions.AS2BusinessException;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

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

                // Print all headers
                Enumeration<Header> headers = bodyPart.getAllHeaders();
                System.out.println("   - MDN Headers:");
                while (headers.hasMoreElements()) {
                    Header header = headers.nextElement();
                    System.out.println(header.getName() + ": " + header.getValue());
                }

                // Get the MDN body content
                String mdnText = new Scanner(bodyPart.getInputStream(), "UTF-8").useDelimiter("\\A").next();

                System.out.println("   - MDN Body Content:");
                System.out.println(mdnText);

                if (!Pattern.compile("automatic-action/MDN-sent-automatically;\\s*processed").matcher(mdnText).find()) {
                    throw new AS2BusinessException("MDN indicates message not processed: " + mdnText);
                }
            }
        } else {
            throw new AS2BusinessException("Body is not an instance of DispositionNotificationMultipartReportEntity -> " + exchange.getIn().getBody());
        }
    }

}
