package org.assimbly.dil.blocks.beans.enrich.attachment;

import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.attachment.DefaultAttachment;
import org.apache.camel.dataformat.mime.multipart.MimeMultipartDataFormat;
import org.assimbly.util.helper.MimeTypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

        Message resourceMessage = resource.getMessage();

        String attachmentName = resolveAttachmentName(original, resourceMessage);
        byte[] data = readBodyAsBytes(resourceMessage);
        String mimeType = resolveMimeType(resourceMessage, data);

        log.info("[Enrich] Adding attachment. key={} mime-type={} size={}", attachmentName, mimeType, data.length);

        AttachmentMessage am = original.getMessage(AttachmentMessage.class);
        if (am == null) {
            throw new IllegalStateException("Original message does not support attachments.");
        }
        ByteArrayDataSource dataSource = new ByteArrayDataSource(data, mimeType);
        DefaultAttachment attachment = new DefaultAttachment(dataSource);
        // Choose the attachment encoding from its bytes: ASCII XML stays 7bit,
        // while non-ASCII or binary content gets an appropriate MIME encoding.
        // This overrides Camel's application/* default only for this attachment.
        attachment.setHeader("Content-Transfer-Encoding", MimeUtility.getEncoding(dataSource));
        am.addAttachmentObject(attachmentName, attachment);
        marshalMultipart(original, am);
        return original;

    }

    private void marshalMultipart(Exchange original, AttachmentMessage message) {
        byte[] body = readBodyAsBytes(message);
        message.setHeader(Exchange.CONTENT_TYPE, resolveMimeType(message, body));

        MimeMultipartDataFormat multipart = new MimeMultipartDataFormat();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            multipart.start();
            try {
                multipart.marshal(original, body, output);
                message.setBody(output.toByteArray());
                message.removeHeader(Exchange.CONTENT_LENGTH);
                // The attachments are now part of the body, not separate payloads.
                message.clearAttachments();
            } finally {
                multipart.stop();
            }
        } catch (Exception e) {
            throw new RuntimeCamelException("Failed to marshal attachment enrichment as MIME multipart.", e);
        }
    }

    private String resolveMimeType(Message resourceMessage, byte[] data) {
        String contentTypeHeader = resourceMessage.getHeader(Exchange.CONTENT_TYPE, String.class);
        if (contentTypeHeader != null) {
            return contentTypeHeader;
        }
        return MimeTypeHelper.detectMimeType(new ByteArrayInputStream(data)).toString();
    }

    private byte[] readBodyAsBytes(Message resourceMessage) {
        try (InputStream body = resourceMessage.getBody(InputStream.class)){

            if (body == null) {
                if (resourceMessage.getBody() != null) {
                    throw new IllegalArgumentException("Enrichment body cannot be converted to an InputStream.");
                }
                return new byte[0];
            }

            return body.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeCamelException("Failed to read attachment enrichment body.", e);
        }

    }

    private String resolveAttachmentName(Exchange original, Message resourceMessage) {
        String enrichName = original.getProperty("Enrich-AttachmentName", String.class);
        if (enrichName != null && !enrichName.isBlank()) {
            return enrichName;
        }
        String fileName = resourceMessage.getHeader(Exchange.FILE_NAME, String.class);
        if (fileName != null && !fileName.isBlank()) {
            return fileName;
        }
        return UNDEFINED_FILE_NAME;
    }

}
