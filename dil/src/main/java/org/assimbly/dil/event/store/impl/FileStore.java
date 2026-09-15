package org.assimbly.dil.event.store.impl;

import org.apache.commons.io.FileUtils;
import org.assimbly.util.BaseDirectory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FileStore {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final String baseDir = BaseDirectory.getInstance().getBaseDirectory();
    private File file;

    public FileStore(String collectorId, org.assimbly.dil.event.domain.Store store) {
        createFile(collectorId, store);
    }

    public void store(String json) {
        List<String> line = Collections.singletonList(json);
        try {
            FileUtils.writeLines(file, line, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void createFile(String collectorid, org.assimbly.dil.event.domain.Store store) {
        String uri = store.getUri();

        // Use LocalDate with explicit ZoneId to resolve both java.time and S8688 warnings
        String today = LocalDate.now(ZoneId.systemDefault()).format(DATE_FORMATTER);

        file = new File(Objects.requireNonNullElseGet(uri, () -> baseDir + "/events/" + collectorid + "/" + today + "_events.log"));
    }

}