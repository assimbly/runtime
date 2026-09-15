package org.assimbly.util.mail;

import jakarta.activation.DataHandler;
import org.apache.axiom.attachments.ByteArrayDataSource;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.commons.io.IOUtils;
import org.assimbly.util.helper.MimeTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class AttachmentAttacher implements Processor {

    protected Logger log = LoggerFactory.getLogger(getClass());

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");

    @Override
    public void process(Exchange exchange) throws Exception {

        AttachmentMessage in = exchange.getIn(AttachmentMessage.class);

        if(in != null){

            String fileName = in.getHeader(Exchange.FILE_NAME, String.class);
            String mimeType = in.getHeader(Exchange.CONTENT_TYPE, String.class);

            InputStream is = in.getBody(InputStream.class);

            // read ONCE
            byte[] fileBytes = IOUtils.toByteArray(is);

            // detect using fresh stream
            if (mimeType == null) {
                mimeType = MimeTypeHelper
                        .detectMimeType(new java.io.ByteArrayInputStream(fileBytes))
                        .toString();
            }

            if (fileName == null) {
                in.setHeader(Exchange.FILE_NAME,
                        LocalDateTime.now(ZoneId.systemDefault()).format(TS_FORMAT) + MimeTypeHelper.findFileExtension(mimeType));
            }

            String emailBody = in.getHeader("EmailBody", String.class);

            if (emailBody == null) {
                emailBody = "";
            }

            AttachmentMessage attMsg = exchange.getIn(AttachmentMessage.class);

            if(attMsg!=null) {
                log.info("Adding attachment '{}' with mime type: '{}'", fileName, mimeType);
                attMsg.addAttachment(fileName, new DataHandler(new ByteArrayDataSource(fileBytes, mimeType)));
            }else {
                log.warn("Adding attachment '{}' with mime type: '{}' failed as attachement is null", fileName, mimeType);
            }

            in.setHeader(Exchange.CONTENT_TYPE, "text/plain");
            in.setBody(emailBody);

        }
    }
}
