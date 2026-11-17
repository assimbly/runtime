package org.assimbly.dil.blocks.beans.enrich.zipfile;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RuntimeCamelException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileEnrichStrategy implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {

        if (newExchange == null) {
            // there’s no remote file to consume
            return oldExchange;
        }

        Message in = oldExchange.getIn();
        Message resource = newExchange.getIn();

        byte[] sourceZip = in.getBody(byte[].class);
        byte[] resourceData = newExchange.getContext().getTypeConverter().convertTo(byte[].class, resource.getBody());

        String fileName = resource.getHeader(Exchange.FILE_NAME_CONSUMED, String.class);
        if(fileName == null) {
            fileName = resource.getHeader(Exchange.FILE_NAME, String.class);
        }
        if(fileName == null) {
            throw new IllegalArgumentException("Zip enrich: the enriched message has no file name (header " + Exchange.FILE_NAME + ")");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            List<String> elementNames = writeZipEntry(zos, resourceData, fileName);
            copyZipEntries(zos, sourceZip, elementNames);
        } catch (IOException e) {
            // Continuing would replace the body with a zip that misses entries
            throw new RuntimeCamelException("Zip enrich: failed to add " + fileName + " to the zip file", e);
        }

        in.setBody(baos.toByteArray());

        return oldExchange;
    }

    // Returns the names of the written entries (the file and its parent directories)
    private List<String> writeZipEntry(ZipOutputStream zos, byte[] data, String filepath) throws IOException {
        List<String> elementNames = new ArrayList<>();
        Iterator<Path> elements = Path.of(filepath).iterator();
        StringBuilder sb = new StringBuilder();

        while (elements.hasNext()) {
            String element = elements.next().toString();
            boolean isLast = !elements.hasNext();

            sb.append(isLast ? element : element + "/");
            elementNames.add(sb.toString());
            zos.putNextEntry(new ZipEntry(sb.toString()));

            if (isLast) {
                zos.write(data);
                zos.closeEntry();
            }
        }

        return elementNames;
    }

    private void copyZipEntries(ZipOutputStream zos, byte[] source, List<String> elementNames) throws IOException {
        ZipEntry existingEntry;

        try(ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(source))) {

            while ((existingEntry = zis.getNextEntry()) != null) {
                String entryName = existingEntry.getName();

                if (elementNames.contains(entryName)) {
                    continue;
                }

                zos.putNextEntry(new ZipEntry(entryName));

                byte[] byteBuff = new byte[4096];
                int bytesRead;
                while ((bytesRead = zis.read(byteBuff)) != -1) {
                    zos.write(byteBuff, 0, bytesRead);
                }

                zos.closeEntry();
                zis.closeEntry();
            }
        }
    }
}
