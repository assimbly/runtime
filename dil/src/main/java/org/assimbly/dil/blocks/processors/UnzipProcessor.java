package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.FilenameUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipProcessor implements Processor {

    List<String> textExtensions = Arrays.asList(
            "txt", "csv", "conf", "cfg", "data", "docx", "edi", "edifact", "edf","log", "ini",
            "md", "msg", "bat", "sh", "json", "rtf", "tsv", "xml", "html", "yaml"
    );

    public void process(Exchange exchange) throws Exception {
        InputStream inputStream = exchange.getIn().getBody(InputStream.class);
        if (inputStream == null) return;

        List<Map<String, Object>> filesList = new ArrayList<>();

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String fileName = entry.getName();
                    byte[] content = getZipContents(zipInputStream);

                    Object finalBody;
                    String extension = FilenameUtils.getExtension(fileName).toLowerCase();

                    // Convert to String if it's a known text format
                    if (textExtensions.contains(extension)) {
                        finalBody = new String(content, StandardCharsets.UTF_8);
                    } else {
                        finalBody = content;
                    }

                    Map<String, Object> fileData = new HashMap<>();
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

    private byte[] getZipContents(ZipInputStream zipInputStream) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int l;
        while ((l = zipInputStream.read(buff)) > 0) {
            outputStream.write(buff, 0, l);
        }
        return outputStream.toByteArray();
    }

}