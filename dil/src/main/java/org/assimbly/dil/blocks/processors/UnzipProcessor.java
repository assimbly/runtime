package org.assimbly.dil.blocks.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.commons.io.FilenameUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipProcessor implements Processor {

    public void process(Exchange exchange) throws Exception {

        Message in = exchange.getIn();
        InputStream inputStream = in.getBody(InputStream.class);
        ArrayList<byte[]> unzipped = new ArrayList<>();
        ArrayList<String> unzippedCamelFileName = new ArrayList<>();
        List<String> fileExtensions = Arrays.asList(
                "txt", "csv", "conf", "cfg", "data", "docx", "edi", "edifact", "edf","log", "ini",
                "md", "msg", "bat", "sh", "json", "rtf", "tsv", "xml", "html", "yaml"
        );
        boolean textFiles = true;

        ZipInputStream zipInputStream = new ZipInputStream(inputStream);

        //Get every file from the zip file
        ZipEntry entry;
        while ((entry = zipInputStream.getNextEntry()) != null) {
            String fileExtension = FilenameUtils.getExtension(entry.getName());
            if (!fileExtensions.contains(fileExtension)) {
                textFiles = false;
            }
            if (!entry.isDirectory()) {
                unzippedCamelFileName.add(entry.getName());
                unzipped.add(getZipContents(zipInputStream));
            }
            zipInputStream.closeEntry();
        }

        //If the zip file only contains text files than convert the files to utf-8 strings
        if(textFiles){
            ArrayList<String> unzippedText = new ArrayList<>();
            for (byte[] byteArray : unzipped) {
                unzippedText.add(new String(byteArray, StandardCharsets.UTF_8));
            }
            in.setBody(unzippedText);
            in.setHeader("CamelFileName", unzippedCamelFileName);
        } else{
            in.setBody(unzipped);
        }

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