package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        InputStream inputStream = in.getBody(InputStream.class);
        ArrayList<byte[]> unzipped = new ArrayList<>();

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                unzipped.add(getZipContents(zipInputStream));
            }
            zipInputStream.closeEntry();
        }

        in.setBody(unzipped);

    }

    private byte[] getZipContents(ZipInputStream zipInputStream) throws IOException {
        var buff = new byte[1024];
        var outputStream = new ByteArrayOutputStream();
        int l;
        while ((l = zipInputStream.read(buff)) > 0) {
            outputStream.write(buff, 0, l);
        }
        return outputStream.toByteArray();
    }

}