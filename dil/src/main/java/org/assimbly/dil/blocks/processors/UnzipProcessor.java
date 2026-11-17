package org.assimbly.dil.blocks.processors;

import java.util.*;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FilenameUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipProcessor implements Processor {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt", "csv", "conf", "cfg", "data", "docx", "edi", "edifact", "edf","log", "ini",
            "md", "msg", "bat", "sh", "json", "rtf", "tsv", "xml", "html", "yaml"
    );

    // All entries are held in memory, so without a limit a small zip (zip bomb) can exhaust the heap
    private static final long DEFAULT_MAX_UNZIPPED_BYTES = 1024L * 1024 * 1024;

    private final long maxUnzippedBytes;

    public UnzipProcessor() {
        this(DEFAULT_MAX_UNZIPPED_BYTES);
    }

    public UnzipProcessor(long maxUnzippedBytes) {
        this.maxUnzippedBytes = maxUnzippedBytes;
    }

    public void process(Exchange exchange) throws Exception {
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        if (inputStream == null) return;

        List<Map<String, Object>> filesList = new ArrayList<>();
        long remainingBytes = maxUnzippedBytes;

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    byte[] content = getZipContents(zipInputStream, remainingBytes);
                    remainingBytes -= content.length;

                    Object finalBody;
                    String extension = FilenameUtils.getExtension(fileName).toLowerCase();

                    // Convert to String if it's a known text format
                    if (TEXT_EXTENSIONS.contains(extension)) {
                        finalBody = new String(content, StandardCharsets.UTF_8);
                    } else {
                        finalBody = content;
                    }

                    Map<String, Object> fileData = new ConcurrentHashMap<>();
                    fileData.put("name", fileName);
                    fileData.put("content", finalBody);
                    filesList.add(fileData);
                }
                zipInputStream.closeEntry();
            }
        }

        // Set the list as the body for the Splitter to pick up
        exchange.getIn().setBody(filesList);
    }

    private byte[] getZipContents(ZipInputStream zipInputStream, long remainingBytes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int l;
        while ((l = zipInputStream.read(buff)) > 0) {
            remainingBytes -= l;
            if (remainingBytes < 0) {
                throw new IOException("Zip file exceeds the maximum unzipped size of " + maxUnzippedBytes + " bytes");
            }
            outputStream.write(buff, 0, l);
        }
        return outputStream.toByteArray();
    }

}
