package org.assimbly.util.helper;

import org.apache.tika.detect.Detector;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.apache.tika.parser.AutoDetectParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public final class MimeTypeHelper {

    private static final Logger log = LoggerFactory.getLogger(MimeTypeHelper.class);

    // loading the parsers is expensive, the detector can be shared between threads
    private static final Detector DETECTOR = new AutoDetectParser().getDetector();

    private MimeTypeHelper() {}

    public static MediaType detectMimeType(InputStream content) {
        try {
            return DETECTOR.detect(content, new Metadata());
        } catch (IOException e) {
            log.error("MimeType not detected",e);
        }

        return MediaType.TEXT_PLAIN;
    }

    public static String findFileExtension(String mimeType) {
        try {
            return MimeTypes.getDefaultMimeTypes().forName(mimeType).getExtension();
        } catch (MimeTypeException _) {
            return null;
        }
    }
}