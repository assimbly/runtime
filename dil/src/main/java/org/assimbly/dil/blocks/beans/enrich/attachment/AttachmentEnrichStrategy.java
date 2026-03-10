package org.assimbly.dil.blocks.beans.enrich.attachment;

import jakarta.activation.DataHandler;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.attachment.AttachmentMessage;
import org.assimbly.util.helper.MimeTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class AttachmentEnrichStrategy implements AggregationStrategy {

    private static final Logger log = LoggerFactory.getLogger(AttachmentEnrichStrategy.class);
    private static final String UNDEFINED_FILE_NAME = "UndefinedFileName";

    @Override
    public Exchange aggregate(Exchange original, Exchange resource) {

        if (original == null) {
            throw new IllegalArgumentException("Original exchange is null, cannot add resource as attachment.");
        }

        if (resource == null) {
            log.error("Resource (enriched message) exchange is null, cannot add resource as attachment.");
            return original;
        }

        Message resourceMessage = resource.getIn();

        String attachmentName = resolveAttachmentName(resource, resourceMessage);
        String mimeType = resolveMimeType(resourceMessage);
        byte[] data = readBodyAsBytes(resourceMessage, mimeType);

        log.info("[Enrich] Adding attachment. key={} mime-type={} size={}", attachmentName, mimeType, data.length);

        AttachmentMessage am = original.getMessage(AttachmentMessage.class);
        am.addAttachment(attachmentName, new DataHandler(data, mimeType));
        original.getMessage().setHeader(Exchange.FILE_NAME, attachmentName);

        return original;

    }

    private String resolveMimeType(Message resourceMessage) {
        String contentTypeHeader = resourceMessage.getHeader(Exchange.CONTENT_TYPE, String.class);
        if (contentTypeHeader != null) {
            return contentTypeHeader;
        }
        InputStream body = resourceMessage.getBody(InputStream.class);
        return MimeTypeHelper.detectMimeType(body).toString();
    }

    private byte[] readBodyAsBytes(Message resourceMessage, String mimeType) {
        // Re-fetch the body since stream may have been consumed during MIME detection
        try (InputStream body = resourceMessage.getBody(InputStream.class)){

            if (body == null) {
                log.warn("[Enrich] Resource body is null, attaching empty byte array.");
                return new byte[0];
            }

            return body.readAllBytes();
        } catch (IOException e) {
            log.error("[Enrich] Failed to read resource body for mime-type={}: {}", mimeType, e.getMessage(), e);
            return new byte[0];
        }

    }

    private String resolveAttachmentName(Exchange resource, Message resourceMessage) {
        String enrichName = resource.getProperty("Enrich-AttachmentName", String.class);
        if (enrichName != null) {
            return enrichName;
        }
        String fileName = resourceMessage.getHeader(Exchange.FILE_NAME, String.class);
        if (fileName != null) {
            return fileName;
        }
        return UNDEFINED_FILE_NAME;
    }

}