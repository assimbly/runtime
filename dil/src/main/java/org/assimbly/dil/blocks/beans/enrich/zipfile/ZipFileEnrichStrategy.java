package org.assimbly.dil.blocks.beans.enrich.zipfile;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipFileEnrichStrategy implements AggregationStrategy {

    protected Logger log = LoggerFactory.getLogger(getClass());
    private List<String> elementNames;

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        elementNames = new ArrayList<>();

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

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            writeZipEntry(zos, resourceData, fileName);
            copyZipEntries(zos, sourceZip);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

        in.setBody(baos.toByteArray());

        return oldExchange;
    }

    private void writeZipEntry(ZipOutputStream zos, byte[] data, String filepath) throws IOException {
        Iterator<Path> elements = Paths.get(filepath).iterator();

        StringBuilder sb = new StringBuilder();

        while (elements.hasNext()) {
            Path path = elements.next();
            String element = path.toString();

            // If there are more elements to come this element is a directory
            // The "/" at the end tells the ZipEntry it is a folder
            if(elements.hasNext()) {
                element += "/";
            }

            elementNames.add(sb + element);

            // Each entry needs the complete path, including previous created folders.
            zos.putNextEntry(new ZipEntry(sb + element));

            sb.append(element);

            if(elements.hasNext()) {
                continue;
            }

            zos.write(data);
            zos.closeEntry();
        }
    }

    private void copyZipEntries(ZipOutputStream zos, byte[] source) throws IOException {
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
