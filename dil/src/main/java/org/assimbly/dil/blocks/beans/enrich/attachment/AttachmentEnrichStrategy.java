package org.assimbly.dil.blocks.beans.enrich.attachment;

import jakarta.activation.DataHandler;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.tika.io.IOUtils;
import org.assimbly.util.helper.MimeTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class AttachmentEnrichStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {
        if (original == null) {
            throw new RuntimeException("Original exchange is null, cannot add resource as attachment.");
        }

        Message resourceMessage;

        if(resource != null){
            resourceMessage = resource.getIn();
        }else{
            log.error("Could not get a response from your requested resource.");
            return original;
        }

        // Default attachment name == message id of incoming resource
        String attachmentName = resourceMessage.getMessageId();

        if (resourceMessage.getHeader(Exchange.FILE_NAME) != null) {
            attachmentName = resourceMessage.getHeader(Exchange.FILE_NAME, String.class);
        }

        InputStream body = resourceMessage.getBody(InputStream.class);

        String mimeType = MimeTypeHelper.detectMimeType(body).toString();

        if (resourceMessage.getHeader(Exchange.CONTENT_TYPE) != null) {
            mimeType = resourceMessage.getHeader(Exchange.CONTENT_TYPE, String.class);
        }

        DataHandler dataHandler;

        byte[] data = new byte[0];

        try {
            data = IOUtils.toByteArray(body);
        } catch (IOException e) { log.error(e.getMessage()); }

        log.info("Adding attachment '{}' with mime type: '{}'", attachmentName, mimeType);
        log.info("Attachment details");
        log.info("\tsize: {}", data.length);

        dataHandler = new DataHandler(data,mimeType);

        AttachmentMessage am = original.getMessage(AttachmentMessage.class);

        am.addAttachment(attachmentName, dataHandler);

        return original;
    }
}
